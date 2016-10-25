import sys, os

# datacombine.py: Combine data files outside of a db.
# Read the files in folder x and write out a new single file.
# The time fields will not be accurate in the final file. This is why this combined file was then loaded into a
# database. The database takes care of ordering the data by time. The Java version is different and requires time
# ordering in the 'combine_after_replace' because the Java version does not use a database.
# Aside: Python is wonderfully self-documenting.
# Compare the Java versions to the Python versions and you'll see succinctness, clarity, and ease of reading in
# the Python versions.
# Why? There simply isn't as much code to decipher. Amazing.
# The Java versions happen to be faster, much faster.
# Usage: datacombine.py <input folder of cleaned files> <combined output file>

folder = sys.argv[1]
outfile = open(sys.argv[2], 'w')

if not os.path.isdir(folder):
    print "First argument must be a directory of cleaned data files"
    print "Usage: datacombine.py <folder of cleaned files> <output file>"
    sys.exit(1)

# We need full paths to the files.
dirpath = os.path.dirname(folder)
files = list(os.listdir(folder))

# Iterate through the files and get the max car id of each file to accumulate and add to carid and queryid.
maxcarid = 0
maxqid = 0
filecount = 0 # Used to assign xways.
for file in files:
    f = open(dirpath+"/"+file)

    # Find current max carid and qid for this file
    curmaxcarid = 0
    curmaxqid = 0

    for line in f:
        t = line.strip().split(",")

        caridint = int(t[2])
        qidint = int(t[9])

        if caridint > curmaxcarid:
                curmaxcarid = caridint
        if qidint > curmaxqid:
                curmaxqid = qidint

        if filecount > 0:
            caridint += maxcarid
            t[2] = str(caridint)
            if t[0] != '0': # Update queryid's only for non-Type 0 notifications.
                qidint += maxqid
                t[9] = str(qidint)
            if t[0] == '0': # Update the xway number.
                t[4] = str(filecount)

        outfile.write(",".join(t)+"\n")

    maxcarid += curmaxcarid+1
    maxqid += curmaxqid+1

    f.close()
    filecount += 1
