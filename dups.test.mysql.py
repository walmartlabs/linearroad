import sys, os, time, random
import MySQLdb

# test the self-join performance of mysql
# this simply tests the ability to find re-entrant duplicates

file = sys.argv[1]

db = MySQLdb.connect(db="test",host="127.0.0.1",user="root")
db2 = MySQLdb.connect(db="test",host="127.0.0.1",user="root")
c = db.cursor()
c2 = db2.cursor()

# DROP ALL TABLES IF THEY EXIST
print "Dropping tables..."
c.execute("DROP TABLE IF EXISTS carsandtimes")
c.execute("DROP TABLE IF EXISTS carstoreplace")

# CREATE carstoreplace TABLE
print "Creating tables..."
c.execute("CREATE TABLE carstoreplace (carid int, cartoreplace int, primary key(carid))")
c.execute("CREATE TABLE carsandtimes (carid int, entertime int, leavetime int, xway int, primary key(carid))") # MemSQL does NOT support CREATE TABLE ... AS SELECT

print "Loading data..."
c.execute("LOAD DATA LOCAL INFILE '"+file+"' INTO TABLE carsandtimes FIELDS TERMINATED BY ','")
db.commit()
print "Creating indexes..."
c.execute("CREATE INDEX carsandtimescarid ON carsandtimes (carid)")
c.execute("CREATE INDEX carsandtimescaridenter ON carsandtimes (carid, entertime)")
c.execute("CREATE INDEX carsandtimescaridleave ON carsandtimes (carid, leavetime)")

# CREATE MATCHES FOR MULTIPLE EXITS AND RE-ENTRANTS
print "Processing and creating duplicates..."
sql = "SELECT times.carid, times.entertime, times.leavetime, times_1.carid as carid1, times_1.entertime as entertime1, times_1.leavetime as leavetime1"
sql += " FROM carsandtimes as times, carsandtimes AS times_1"
sql += " WHERE times_1.entertime>times.leavetime+1000*rand()+61"
sql += " LIMIT 1"
total_st = time.time()
st = time.time()
c.execute(sql)
et = time.time()
print et-st
replacements = 0

while c.rowcount > 0:
    r = c.fetchone()
    print r
    c2.execute("INSERT INTO carstoreplace VALUES("+str(r[0])+","+str(r[3])+")")
    db2.commit()
    c2.execute("DELETE FROM carsandtimes WHERE carid="+str(r[0])+" OR carid="+str(r[3]))
    db2.commit()
    replacements += 1
    print replacements
    st = time.time()
    c.execute(sql)
    et = time.time()
    print et-st
    print et-total_st

c.close()
c2.close()
db.close()
db2.close()

print "Finished!"
