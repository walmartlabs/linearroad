import sys, os, time, random
import MySQLdb
import subprocess

# combine.py: this file takes individual cardatapoints.outX files and creates a single file with cars incremented by a given amount
# written as if to be run on the node with MemSQL
# Usage: python combine.py <file> <output dir> <num xways>

#datadir = '/datadrive/linear'  # this will be the dir of the memsql db
file = sys.argv[1]
datadir = sys.argv[2]  # no trailing '/'
numXWays = sys.argv[3]
#files = list(os.listdir(datadir + "/xways"))

db = MySQLdb.connect(db="test",host="127.0.0.1",user="root")
db2 = MySQLdb.connect(db="test",host="127.0.0.1",user="root")
c = db.cursor()
c2 = db2.cursor()
#numXWays = len(files)
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
c.execute("DROP TABLE IF EXISTS maxtimes")

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

# CREATE maxtimes TABLE
#c.execute("CREATE TABLE maxtimes (carid int, maxtime int, shard key(carid))")
#c.execute("CREATE INDEX maxtimesid ON maxtimes (carid)")

# INSERT RECORDS
print "Inserting records..."
start_time = time.time()
print "Start time: " + time.strftime("%H:%M:%S")
fileCount = 0
#print "Number of input files: " + str(numXWays)
#buffer = 2000
#for file in files:
#       if not os.path.isdir(file):
#               print "Processing file " + file + ", which is fileCount: " + str(fileCount)
#               # Get the current max(carid) and max(qid) to use for incrementing carid and qids
#               c.execute("SELECT MAX(carid), MAX(qid) FROM input")
#               r = c.fetchone()
#               # update the previous before adding the next, and this REALLY assumes that no new carids will overlap,
#               ## i.e. the max carid from each run will not overlap 100 + the max id from the previous run
#               ### add a buffer just in case?
#               #### There is no way this DOESN'T overlap AND this means the original script is also wrong
#               ##### How much buffer do you have to add?  1000? 5000? 10000?  279502 280354 is the biggest different of the first five.  10K it is.
#               ###### Nevermind for now if larger data sets yield larger differences.
#               if fileCount > 0:
#                       print "Updating existing records...setting to xway=" + str(fileCount)
#                       # You NEED two separate updates or you permanantly destroy any value (if there was any in the first place) from the qid field
#                       c.execute("UPDATE input SET qid=qid+"+str(r[1]+buffer)+" WHERE xway=0 AND qid <> -1")
#                       db.commit()
#                       c.execute("UPDATE input SET carid=carid+"+str(r[0]+buffer)+" WHERE xway=0")
#                       db.commit()
#                       c.execute("UPDATE input SET xway="+str(fileCount)+" WHERE xway=0")
#                       db.commit()
#               # insert file into table 'input'
#               print "Loading file " + datadir+"/xways/"+file
c.execute("LOAD DATA INFILE '"+file+"' INTO TABLE input FIELDS TERMINATED BY ','")
db.commit()
# Print MAX carid and MAX qids from this file
c.execute("SELECT COUNT(*) FROM input WHERE xway=0")
if c.rowcount > 0:
        r = c.fetchone()
print "Num of xway 0 records in the dataset is " + str(r[0])

print "Total number of records imported from " + str(fileCount) + " files: "
c.execute("SELECT COUNT(*) FROM input")
if c.rowcount > 0:
        r = c.fetchone()
        print r[0]

print "Total time to load file(s) ... " + str(time.time() - start_time) + " seconds."

# REMOVE ALL CARS THAT ONLY HAVE ONE RECORD AND TYPE 4 Records
#c.execute("DELETE FROM input WHERE type=4")
#db.commit()
#print "Removing single records..."
#c.execute("SELECT carid, count(*) FROM input GROUP BY carid HAVING count(*) = 1")
#if c.rowcount > 0:
#       for i in xrange(0, c.rowcount):
#               r = c.fetchone()
#               c2.execute("DELETE FROM input WHERE carid="+str(r[0]))
#               db2.commit()

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

# Remove singles
#print "Creating table of singles..."
#c.execute("SELECT carid, count(*) FROM input GROUP BY carid HAVING count(*) = 1")
#num_recs = c.rowcount
#print "Number of singles: " + str(num_recs)
#for i in xrange(0, num_recs):
#       r = c.fetchone()
#       print "Deleting single " + str(i) + "/"+ str(num_recs) + ", carid: " + str(r[0])
#       c2.execute("DELETE FROM input WHERE carid="+str(r[0]));
#       db2.commit()

# Skip this because the paper says all cars finish a journey, but then says not all cars finish a journey!
# ENSURE ALL CARS EXIT EXPRESSWAY (important?)
#print "Making sure all cars exit..."
#print "Creating maxtimes table..."
# Fill up maxtimes table
#c.execute("SELECT carid, max(time) as maxtime FROM input GROUP BY carid")
#for i in xrange(0, c.rowcount):
#       r = c.fetchone()
#       c2.execute("INSERT INTO maxtimes VALUES ("+str(r[0])+","+str(r[1])+")")
#       db2.commit()
# Use maxtimes table to modify input table
#print "Using maxtimes to update input table..."
#c.execute("SELECT * FROM maxtimes")
#num_recs = c.rowcount
#for i in xrange(0, num_recs):
#       r = c.fetchone()
#       #c2.execute("UPDATE input SET lane=4, speed="+str(random.randint(10,30))+" WHERE carid="+str(r[0])+" AND time="+str(r[1])+" AND lane <> 4 AND type=0");
#       print "Updating record " + str(i) + " of " + str(num_recs)
#       c2.execute("UPDATE input SET lane=4, speed=10 WHERE carid="+str(r[0])+" AND time="+str(r[1])+" AND lane <> 4 AND type=0");
#       db2.commit()

#c.execute("UPDATE input SET lane=4 AND speed="+str(random.randint(10,39))+" WHERE seg=99 AND dir=0 AND lane <> 4")
#c.execute("UPDATE input SET lane=4 AND speed="+str(random.randint(10,39))+" WHERE seg=0 AND dir=1 AND lane <> 4")

# REMOVE ALL POSITION REPORTS THAT ARE ANAMOLOUS

# CREATE MATCHES FOR MULTIPLE EXITS AND RE-ENTRANTS
print "Processing and creating duplicates..."
## Guarantee that cars WON'T re-enter the same expressway as the original trip, if possible
### Since this part took too long on in ANY database with large record sets, moving to python script p_duplicates.py
# Copy to a file carsandtimes table
c.execute("SELECT * FROM carsandtimes INTO OUTFILE '"+datadir+"/carsandtimes.csv' FIELDS TERMINATED BY ','")

# RUN THE DUPLICATION SCRIPT
#subprocess.call(["python", "p_duplicates.v.1.0.0.py", datadir, str(numXWays)])
subprocess.call(["python", "p_duplicates.v.1.0.2.py", datadir, str(numXWays)])

# Take the results of the duplication script and insert into TABLE carstoreplace
################################
## I have no idea why the below statement doesn't work.  It works if you run in the command line first...
### So, you have to load via the command line (client) and then run runcarstoreplaceupdate.py
## Need the db.commit()
################################
print "Loading carstoreplace file into carstoreplace table."
c.execute("LOAD DATA INFILE '"+datadir+"/carstoreplace.csv' INTO TABLE carstoreplace FIELDS TERMINATED BY ','")
db.commit()
# Update input table with duplicates
#c.execute("UPDATE input SET carid='carstoreplace.carid' WHERE carid='carstoreplace.cartoreplace'")
# Query above doesn't work, sooo... one by one it is!
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

                                                                                        
