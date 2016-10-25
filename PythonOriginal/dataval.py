import sys, time

# dataval.py: With a raw mitsim data file perform the following:
# 1) Check for position reports that are not 30 secs apart, and simply report.
# 2) Ensure car does not reappear after exiting.
# 3) Remove negative positions and segments.
# 4) Remove type 3 queries with a  day of '0' if any.
# Usage: dataval.py <raw_file> <cleaner_file>

f = open(sys.argv[1])
w = open(sys.argv[2], 'w')
print "Validating data file: " + sys.argv[1]

# This is the map that will hold all carid's and the last line show.
cars = {}  # K: carid     V: time
exited = {}  # K: carid     V: time
# Time how long it to perform parts of this cleanup.
st = time.time()

# Read through the file and fix issues.
for line in f:
    # 'discard isn't used."
    #discard = False
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
            continue  # Skip this row as it already exited.
        if carid not in cars:
            cars[carid] = ctime # Add the car and set its time.
        else:
            # 30 sec incr?
            if int(cars[carid]) != int(ctime)-30:
                print cars[carid] + " " + ctime
                print "Time error for car " + carid + " at time " + ctime
            cars[carid] = ctime
        if lane == '4': # Put this car in the exited dict.
            exited[carid] = ctime
        if int(seg) < 0: # Fix negative segments and positions.
            print t
            t[7] = '0'
            t[8] = '0'
    elif type == '2': # Ignore Type 2's.
        pass
    elif type == '3': # Ignore Type 3's with day 0.
        if day == '0':
            continue

    # 'discard' isn't used.
    #if not discard:
    w.write(",".join(t)+"\n")

print "Time to run dataval.py: " + str(time.time() - st)

