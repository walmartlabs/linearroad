import sys

# datarm2.py: Remove carid's with only one or two records.
# Writes to stdout.
# Usage: python datarm2.py <file> > <outfile>

f = open(sys.argv[1])

# Hold carid's and the number of times the carid appears in this file.
counts = {}
for line in f:
    t = line.strip().split(",")
    if t[2] not in counts:
        counts[t[2]] = 1
    else:
        counts[t[2]] += 1

# Read the file again and ignore those carid's that don't have more than two records.
f.seek(0)
for line in f:
    t = line.strip().split(",")
    if t[2] in counts:
        if counts[t[2]] > 2: # Ensure this carid has > 2 records.
            if t[0] != '4':  # Ignore type 4's.
                if t[0] == '3':  # Redundant if run through dataval.py, but check for day 0 type 3's.
                    if t[len(t)-1] == '0':
                        continue
                print line.strip()
