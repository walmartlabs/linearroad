import sys, os, time, random
import MySQLdb
import subprocess

# combine.py: now takes a single, combined, clean file and creates the tolls and re-entrant cars
#  this script requires the original 'historical-tolls.pl' script and the 'p_duplicates.py' script
#  it turns out mysql can run the original self-join query fairly quickly up to a large number of replacements, so
#    p_duplicates.py may not be necessary, or even the best option anymore
# Usage: python combine.py <file> <output dir> <num xways>

file = sys.argv[1]  # not used, because
datadir = sys.argv[2]  # no trailing '/'
numXWays = sys.argv[3]

db = MySQLdb.connect(db="test",host="127.0.0.1",user="root")
db2 = MySQLdb.connect(db="test",host="127.0.0.1",user="root")
c = db.cursor()
c2 = db2.cursor()
overlap = 10
maxCarId = None

def generateRandomTable(maxCarId, overlap, db):
        c = db.cursor()
        for i in xrange(100, maxCarId):
                if random.random() * 100 < overlap:  # the perl rand() function returns 0 < max; we use python vals from [0.0, 1.0)
                        c.execute("INSERT INTO duplicatecars VALUES ("+str(i)+")")
                        db.commit()
        c.close()

# DROP ALL TABLES IF THEY EXIST
print "Dropping tables..."
c.execute("DROP TABLE IF EXISTS input")
c.execute("DROP TABLE IF EXISTS carsandtimes")
c.execute("DROP TABLE IF EXISTS carstoreplace")
c.execute("DROP TABLE IF EXISTS duplicatecars")

# CREATE input TABLE
print "Creating tables..."
c.execute("CREATE TABLE IF NOT EXISTS input ( type int, time int, carid int, speed int, xway int, lane int, dir int, seg int, pos int, qid int, m_init int, m_end int, dow int, tod int, day int, shard key(xway))")  # just finding a field that won't change
c.execute("CREATE INDEX inputcarid ON input (carid)")
c.execute("CREATE INDEX inputcaridtime ON input (carid, time)")
c.execute("CREATE INDEX inputtime ON input (time)")
c.execute("CREATE INDEX inputlane ON input (lane)")
c.execute("CREATE INDEX inputtype ON input (type)")

# CREATE duplicatecars TABLE
c.execute("CREATE TABLE duplicatecars (carid int, shard key(carid))")

# CREATE carstoreplace TABLE
c.execute("CREATE TABLE carstoreplace (carid int, cartoreplace int, shard key(carid))")

# INSERT RECORDS
print "Inserting records..."
start_time = time.time()
print "Start time: " + time.strftime("%H:%M:%S")
# this may be too slow
# the other, and better option, is to parallel load the data and skip this step (COMMENT OUT THE DELETE TABLE input ABOVE!)
# memsql loads can be very, very quick and thus a combination of memsql and mysql _may_ yield the fastest results
c.execute("LOAD DATA INFILE '"+file+"' INTO TABLE input FIELDS TERMINATED BY ','")
db.commit()

print "Total time to load file(s) ... " + str(time.time() - start_time) + " seconds."

# GET MAX CAR ID
print "Getting maxCarId..."
c.execute("SELECT max(carid) FROM input")
r = c.fetchone()
maxCarId = r[0]
print "maxCarId: " + str(maxCarId)

# GENERATE HISTORICAL TOLLS
print "Generating historical tolls..."
subprocess.call(["perl", "historical-tolls.pl", str(numXWays), str(maxCarId), "."])
subprocess.call(["mv", "historical-tolls.out", datadir+"/my.tolls.out"])

# Generate random duplicate values for potential replacement
print "Creating random table..."
generateRandomTable (maxCarId, overlap, db)

# CREATE carsandtimes TABLE
print "Creating carsandtimes..."
c.execute("CREATE TABLE carsandtimes (carid int, entertime int, leavetime int, xway int, shard key(carid))") # MemSQL does NOT support CREATE TABLE ... AS SELECT
c.execute("SELECT duplicatecars.carid, min(input.time) as entertime, max(input.time) as leavetime, xway FROM duplicatecars, input WHERE duplicatecars.carid=input.carid GROUP by duplicatecars.carid")
for i in xrange(0, c.rowcount):
        r = c.fetchone()
        c2.execute("INSERT INTO carsandtimes VALUES ("+str(r[0])+","+str(r[1])+","+str(r[2])+","+str(r[3])+")")
        db2.commit()
c.execute("CREATE INDEX carsandtimescarid ON carsandtimes (carid)")
c.execute("CREATE INDEX carsandtimescaridenter ON carsandtimes (carid, entertime)")
c.execute("CREATE INDEX carsandtimescaridleave ON carsandtimes (carid, leavetime)")

# CREATE MATCHES FOR MULTIPLE EXITS AND RE-ENTRANTS
print "Processing and creating duplicates..."
c.execute("SELECT * FROM carsandtimes INTO OUTFILE '"+datadir+"/carsandtimes.csv' FIELDS TERMINATED BY ','")

# RUN THE DUPLICATION SCRIPT
subprocess.call(["python", "p_duplicates.py", datadir, str(numXWays)])

# Take the results of the duplication script and insert into TABLE carstoreplace
print "Loading carstoreplace file into carstoreplace table."
c.execute("LOAD DATA INFILE '"+datadir+"/carstoreplace.csv' INTO TABLE carstoreplace FIELDS TERMINATED BY ','")
db.commit()

# Update input table with duplicates
c.execute("SELECT * FROM carstoreplace")
num_recs = c.rowcount
print "Number of cars to replace: " + str(num_recs)
for i in xrange(0, num_recs):
        r = c.fetchone()
        print "Replacing record " + str(i) + " of " + str(num_recs) + ", " + str(r[1]) + " with " + str(r[0])
        c2.execute("UPDATE input SET carid="+str(r[0])+" WHERE carid="+str(r[1]));
        db2.commit()

# Export final file
print "Exporting final data file to " + datadir + "/my.data.out"
c.execute("SELECT * FROM input ORDER BY time INTO OUTFILE '"+datadir+"/my.data.out' FIELDS TERMINATED BY ','")

c.close()
c2.close()
db.close()
db2.close()
