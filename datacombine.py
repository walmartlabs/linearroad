import sys, os

# datacombine.py: combine data files outside of a db
#  read the files in folder x and write out a new single file
# Usage: datacombine.py <input folder of cleaned files> <combined output file>

folder = sys.argv[1]
outfile = open(sys.argv[2], 'w')

if not os.path.isdir(folder):
        print "First argument must be a directory of cleaned data files"
        print "Usage: datacombine.py <folder of cleaned files> <output file>"
        sys.exit(1)

dirpath = os.path.dirname(folder)
files = list(os.listdir(folder))

# iterate through the files and get the max car id of each file to accumulate and add to carid and queryid

maxcarid = 0
maxqid = 0
filecount = 0
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
                        if t[0] != '0':
                                qidint += maxqid
                                t[9] = str(qidint)
                        if t[0] == '0':
                                t[4] = str(filecount)

                outfile.write(",".join(t)+"\n")

        print "curmaxcarid: " + str(curmaxcarid)
        print "curmaxqid: " + str(curmaxqid)
        print "maxcarid: " + str(maxcarid)
        print "maxqid: " + str(maxqid)
        print "---"
        maxcarid += curmaxcarid+1
        maxqid += curmaxqid+1
        print "maxcarid: " + str(maxcarid)
        print "maxqid: " + str(maxqid)

        f.close()
        filecount += 1
