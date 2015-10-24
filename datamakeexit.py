import sys

# datamakeexit.py: ensure that all vehicles get off the xway
#  run after dataval.py and datarm2.py
# Usage: datamakeexit.py <file> <outfile>

f = open(sys.argv[1])

lasttimes = {}

for line in f:
        t = line.strip().split(",")
        lasttimes[t[2]] = t[1]

f.seek(0)
for line in f:
        t = line.strip().split(",")
        if t[1] == lasttimes[t[2]] and t[0] == '0': # only last appearing type 0 queries need adjustment
                t[3] = '10'
                t[5] = '4'
                print ",".join(t)
        else:
                print line.strip()
