import random, sys

# p_duplicates.py: since the self-join on carsandtimes is just TOO slow to create the random replacements we pull the information out into flat files for python manip.
# Need flag to indicate when we have more than one xway in the file
# Usage: python p_duplicates.py <True|False>
# This script should generally be called by combine.py

# 1) copy or select out the carsandtimes tables
# connect to the database and pull down a copy?  Don't knnow if you can pull a remote copy.
# create a new table so we can guarantee that cars are in different lanes when they reenter
# increase the overlap count?
        # decrease the amount of time needed between cars

# v.1.0.2
# Moves from O(N^2) to O(N)

# Assume will always run on memsql database is always the default output folder
dir = sys.argv[1] # no trailing '/'
#f1 = open(sys.argv[1])
f1 = open(dir + '/carsandtimes.csv')
f2 = open(dir + '/carstoreplace.csv','w')

# Do we need to account for more than one expressway (affects whether duplicates are assigned to different xways)
numXWays = 1
if len(sys.argv) > 2:
        numXWays = sys.argv[2]
print "numXWays: " + str(numXWays)

# 2) place token lists into a python list (carid, entertime, leavetime, seg)
# These are not official tuple types, just lists
c1 = []
for l in f1:
        c1.append(l.strip().split(','))
f1.close

# 3) create a copy of the list
c2 = list(c1)

# 3.10) create a third copy for validation
c3 = list(c1)

