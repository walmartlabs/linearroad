import sys

# datarm2.py: remove carid's with only one or two records
#  writes to stdout
# Usage: python datarm2.py <file> > <outfile>

f = open(sys.argv[1])

counts = {}
for line in f:
        t = line.strip().split(",")
        if t[2] not in counts:
                counts[t[2]] = 1
        else:
                counts[t[2]] += 1

f.seek(0)
for line in f:
        t = line.strip().split(",")
        if t[2] in counts:
                if counts[t[2]] > 2:  # ensure this carid has > 2 records
                        if t[0] != '4':  # ignore type 4's
                                if t[0] == '3':  # redundant if run through dataval.py, but check for day 0 type 3's
                                        if t[len(t)-1] == '0':
                                                continue
                                print line.strip()
