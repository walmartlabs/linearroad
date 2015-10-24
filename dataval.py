import sys, time

# dataval.py: with a raw mitsim file perform the following:
#  check for position reports that are not 30 secs apart, and simply report
#  ensure each car has lanes 0 and 4
#  discard any singles

# Usage: dataval.py <input_file> <good_file>

f = open(sys.argv[1])
w = open(sys.argv[2], 'w')
print "Validating data file: " + sys.argv[1]

# This is the map that will hold all carid's and the last line show
reports = {}  # K: carid     V: line
cars = {}  # K: carid     V: time
exited = {}  # K: carid     V: time
errors = {}
st = time.time()

for line in f:
        discard = False
        t = line.strip().split(",")
        type =  t[0]
        ctime = t[1]
        carid = t[2]
        speed = t[3]
        xway  = t[4]
        lane  = t[5]
        dir   = t[6]
        seg   = t[7]
        pos   = t[8]
        day   = t[14]

        if type == '0':
                if carid in exited:
                        continue  # skip this row
                if carid not in cars:
                        cars[carid] = ctime
                else:
                        # 30 sec incr?
                        if int(cars[carid]) != int(ctime)-30:
                                print cars[carid] + " " + ctime
                                print "Time error for car " + carid + " and time " + ctime
                        cars[carid] = ctime
                if lane == '4': # put this car in the exited dict
                        exited[carid] = ctime
                if int(seg) < 0:
                        print t
                        t[7] = '0'
                        t[8] = '0'
        elif type == '2':
                pass
        elif type == '3':
                if day == '0':
                        continue

        if not discard:
                #w.write(line)
                w.write(",".join(t)+"\n")

print "Time to run dataval.py: " + str(time.time() - st)
                                                            
