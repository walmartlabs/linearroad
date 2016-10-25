import random, sys

# p_duplicates.py: Since the self-join on carsandtimes is just TOO slow to create the random replacements.
# This script should be called by combine.py
#
# NOTE: This is really slow too. For best results create Python versions of:
# create_carsandtimes.java, create_carstoreplace.java, and combine_after_replace.java.

dir = sys.argv[1] # Ensure there is no trailing '/' when calling from combine.py.
f1 = open(dir + '/carsandtimes.csv')
f2 = open(dir + '/carstoreplace.csv','w')

# Do we need to account for more than one expressway (affects whether duplicates are assigned to different xways).
numXWays = 1
if len(sys.argv) > 2:
    numXWays = sys.argv[2]
print "numXWays: " + str(numXWays)

# Place token lists into a python list (carid, entertime, leavetime, seg).
c1 = []
for l in f1:
    c1.append(l.strip().split(','))
f1.close

# Create a copy of the list.
c2 = list(c1)

# Shuffle both lists.
random.shuffle(c1)
random.shuffle(c2)

replacements = []

# 4.10) Print the size of the original carsandtimes list of tuples.
print "Original number of carsandtimes: " + str(len(c1))

#####################################################
# find an appropriate car to use as a re-entrant car.
# car1: the current car of the first list.
# cars0: the list of the current car (to remove car1 if a match is found).
# cars: the list from which to find a match.
# replacements: the list to hold the replacements tuples.
#####################################################
def findCar(car1, cars0, cars, replacements):
    random_inc = 1000 * random.random() + 61  #
        
    # Try 1000 times to find a replacement for each car1.
    for i in xrange(0,1000):
        car2 = cars[random.randint(0,len(cars)-1)]
        if float(car2[1]) > float(car1[2])+random_inc:
            if numXWays > 1:
                if car2[3] == car1[3]: # Try again the xways are the same.
                    continue
            replacements.append([car1[0], car2[0]])
            print len(replacements), # Print find out how many replacements we've found so far.
            # NOTE: we are modifying the list as we're iterating over it.
            cars0.remove(car1)
            cars0.remove(car2)
            cars.remove(car1)
            cars.remove(car2)
            break

for i in xrange(0,1):  # Can choose arbitrary number of times to run loop.
    for c in c1:
        findCar(c, c1, c2, replacements)
    print "Length of c1: " + str(len(c1))
    print "Length of c2: " + str(len(c2))

print "Number of replacements to make: " + str(len(replacements))

for t in replacements:
    f2.write(str(t[0])+","+str(t[1])+"\n")


