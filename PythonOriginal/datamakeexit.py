import sys

# ====
# Note: This step isn't really necessary and can be removed.
# ====

# datamakeexit.py: Ensure that all vehicles get off the xway.
# Run after dataval.py and datarm2.py.
# Usage: datamakeexit.py <file> <outfile>

f = open(sys.argv[1])

lasttimes = {}

# Read the file and find the last time for each vehicle.
for line in f:
    t = line.strip().split(",")
    lasttimes[t[2]] = t[1]

# Go back to the beginning of the file and re-read,
# when the last notification for a car is seen modify the line to make it an exiting notification.
f.seek(0)
for line in f:
    t = line.strip().split(",")
    if t[1] == lasttimes[t[2]] and t[0] == '0': # Only last appearing type 0 queries need adjustment.
        t[3] = '10'
        t[5] = '4'
        print ",".join(t)
    else:
        print line.strip()
