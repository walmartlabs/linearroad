import time, sys, MySQLdb

# datafixtype3.v.1.0.2.py: this would need to be run, AFTER, combine.py is run.
# Usage: python datafixtype3.py <post-processed .dat file> <historical-toll.out file> <new historical file>
# prints cleaned results to stdout

# load complete historical-tolls.out file into memory, key: carid, day; values: xway, amt
# take the .dat file
# filter out all type 3
# for each type 3, re-write the xway to the .dat file

# v.1.0.2 uses MemSQL instead python dicts for faster runtime

db = MySQLdb.connect(db="test",user="root",host="127.0.0.1")
c = db.cursor()
db2 = MySQLdb.connect(db="test",user="root",host="127.0.0.1")
c2 = db2.cursor()

datfile = sys.argv[1]
histfile = sys.argv[2]
outfile = sys.argv[3]

histdict = {}

print "Dropping historical table"
c.execute("DROP TABLE IF EXISTS histtoll")
print "Creating historical table"
c.execute("CREATE TABLE histtoll (carid int, day int, xway int, amt int, shard key(carid))")
print "Loading historical table"
st = time.time()
c.execute("LOAD DATA INFILE '" + histfile + "' INTO TABLE histtoll FIELDS TERMINATED BY ','")
db.commit()
print "Time to load '" + histfile + "': " + str(time.time() - st)
print "Creating indexes on histtoll"
#c.execute("CREATE INDEX caridi ON histtoll(carid)")
#c.execute("CREATE INDEX xwayi ON histtoll(xway)")
#c.execute("CREATE INDEX dayi ON histtoll(day)")
#c.execute("CREATE INDEX keyi ON histtoll(carid,day,xway)")

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
# now, reprint the historical-tolls file, BUT, how to do so SORTED?! by carid, and day?  (Does it even matter?)
print "Print NEW historical tolls file"
c.execute("SELECT * FROM histtoll INTO OUTFILE '" + outfile + "' FIELDS TERMINATED BY ','")
db.commit()

c.close()
db.close()

