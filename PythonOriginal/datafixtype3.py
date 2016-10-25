import time, sys, MySQLdb

# datafixtype3.py: 
# Run AFTER combine.py.
# This fixes the toll file to have matching xways with the main data file.
# Otherwise, the Type 3 requests from the main data file would not match the randomly generated toll file.
# Prints cleaned results to stdout.
# Usage: python datafixtype3.py <post-processed .dat file> <historical-toll.out file> <new historical file>
# Note: the <post-processed .dat file> is no longer needed and can just be any file for now.
#
# NOTE: Again, this file may be better if re-written in the Java version, 'fixtolls.java.'


db = MySQLdb.connect(db="test",user="root",host="127.0.0.1")
c = db.cursor()
db2 = MySQLdb.connect(db="test",user="root",host="127.0.0.1")
c2 = db2.cursor()

datfile = sys.argv[1]  # arg 1 is no longer needed and can be removed.  it was necessary when the output file, rather than the database was used
histfile = sys.argv[2]
outfile = sys.argv[3]

print "Dropping historical table"
c.execute("DROP TABLE IF EXISTS histtoll")

print "Creating historical table"
c.execute("CREATE TABLE histtoll (carid int, day int, xway int, amt int, shard key(carid))")

print "Loading historical table"
st = time.time()
c.execute("LOAD DATA INFILE '" + histfile + "' INTO TABLE histtoll FIELDS TERMINATED BY ','")
db.commit()

print "Time to load '" + histfile + "': " + str(time.time() - st)

print "Use the already existing input table..."
c.execute("SELECT carid, day, xway FROM input WHERE type = 3")
rc = c.rowcount
count = 0
for i in xrange(0, rc):
    r = c.fetchone()
    c2.execute("UPDATE histtoll SET xway = " + str(r[2]) + " WHERE carid = " + str(r[0]) + " AND day = " + str(r[1]))
    db2.commit()
    count += 1

print "Number of type 3 corrections: " + str(count)

print "Print NEW historical tolls file"
c.execute("SELECT * FROM histtoll INTO OUTFILE '" + outfile + "' FIELDS TERMINATED BY ','")
db.commit()

c.close()
db.close()
c2.close()
db2.close()
