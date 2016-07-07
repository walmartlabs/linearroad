# linearroad
Walmart version of the Linear Road streaming benchmark.

## Overview
LinearRoad is a streaming data management system (SDMS) benchmark originally created in 2004.
It was created at a time when SDMS systems were relatively new.
The original Linear Road benchmark paper was a joint effort between collaborators from Stanford University, Brandeis University, Massachusetts Institute of Technology, and the Oregon Health and Science University/Oregon Graduate Institute.  It has since been endorsed by Stanford, Brandeis, MIT, and Brown Universities as an SDMS benchmark.

All original files were downloaded from http://www.cs.brandeis.edu/~linearroad/tools.html.
These original files were then modified or re-written for performance reasons, including for quick creation of arbitrarily large datasets.

0.2 release.


## Notes
Type 4 queries are not implemented as per the original paper, nor are Type 4 queries implemented in subsequent implementations.  Type 4 may be implemented in the future.

The validator and many portions of data generation were originally re-written in Python but have since been re-written again in Java, mainly for performance reasons.  The choice of Python was initially arbitrary.  The original rewrite of the validator was limited by available RAM.  Versions that leverage multiple nodes and/or persisent datastores have been experimented with.  The current version of the Validator uses a single node, multi-threaded, with Aerospike as the datastore.  Using this setup up to a 50 xway dataset has been validated in only 234 minutes on a single Azure DS12 node with four cores and 28 GB of RAM.  

### Data Generation
Datasets of arbitrary sizes can be generated on a single machine or by parallelizing the expressway generation on multiple machines.  The original mitsim (microscopic traffic simulator) program creates each expressway as a separate file and each expressway file can take three hours or more to generate.  The file size for a one xway three hour simulation with 1,000 cars per segment per hour is ~1GB and contains ~20M tuples.  Since each file is independent of all other files, the creation of these raw files can be parallelized on many machines, or VM's.  

Each independent file created by mitsim is expressway 0, and each has its own independent car and query id numbering starting at ~100 and ~1, respectively.  In order to combine an arbitrary number of these files into a single simulation file the expressway number, as well as the car and query ids must be incremented according to the number of expressways being combined.

Before combining the files some cleaning on the original, "raw" files is necessary to create a "clean"er set before running the combiner.  This cleaning helps remove noise from the data.  An example of noise: carids exist that exit but magically reappear without going through an entry lane.

After cleaning, the initial combination process merges _n_ "clean" files, incrementing the expressway number from _0 thru n-1_ for each respective, cleaned file.  It also increments the car and query ids by a current _max_car_id_ and current _max_query_id_ from the previous file to avoid overlap.

Then, the subsequent combination process create random historical tolls and creates the random re-entrant cars by replacing a percentage of random cars by other random cars that meet the criteria of having an entry time _1000 * random.random() + 61_ greater than the exit time of another car.

The percentage of cars to check for possible re-entry is 10% by default.  Note that this does not mean 10% of the cars will be re-entrant but only that 10% will be checked to see if they _can_ be re-entrant.   

Carid's monotonically increase but they do not so by only increments of 1.  The actual carid's present in a given expressway may be 102, 120, ..., 123, 124, 130, etc...

From this pool of potential re-entrant carid's, a table with the _enter-time_,  _leave-time_, and  _expressway_ of each carid is created.  The carid's in this new table are checked to see if a carid with an _enter-time > 1000 * random.random() + 61 + leave-time_ of another car exists.  If the number of simulated expressways > 1, the carid's must be from different expressways.  We are simulating a car leaving one expressway and re-entering on a different expressway at a later point in time.  If only one expressway is present we are simply simulating a car re-entering at a later point in time.   The _1000 * random.random() + 61_ from the original paper appears to be arbitrary.  NOTE: Python's random.randint() is very slow and was the root of some Python performance issues.  Even taking randint() into account and switching to random.random() the current Java code is at least 2x faster than the Python code.

This process of making re-entrant carid's was a bottleneck to creating a single file from any arbitrary number of cleaned files.  The original SQL version used the following query:

_SELECT times.carid, times.entertime, times.leavetime, times_1.carid as carid1, times_1.entertime as entertime1, times_1.leavetime as leavetime1
FROM carsandtimes as times, carsandtimes AS times_1
WHERE times_1.entertime>times.leavetime+1000*random()+61
LIMIT 1;_

If a match is found the two carids that match are removed from the _carsandtimes_ table and entered into a new _carstoreplace_ table, which simply holds two carid's per row.

This query slows down tremendously as the "low-hanging fruit" is removed.  For perspective: for the 50 expressway data set there are 7,851,650 unique carid's with a max carid of 13,958,137.  From this max carid we get 1,395,100 potential duplicate, or re-entrant, cars per our description above.  And, we get 783,265 actual carid's that exist.  And, from these that actually exist we find, or create, 204,095 re-entrant cars.

The issue with the original SQL statement is the size of the self-joined table.  If attempted with this 50 expressway set the self-joined table would be up to 783,265 ^ 2, or 613,504,060,225 rows, from which to try and find an entry matching the WHERE clause.  Again, low hanging fruit can be found relatively quickly--from negligible to under a second.  But, as these easier finds are removed each additional match can run for many seconds, to minutes and tens of minutes, as up to the max rows above are potentially scanned.  Even if we limit the number of re-entrant cars to 200K, and even if the performance were steady at one per second, it would take over 55 hours to create the re-entrant cars.  But, as the query does slow down, and creation of even 200K would likely run into days, weeks, or more the above query was untenable.

To mitigate the issue above a separate script was created that creates the re-entrant cars.  The original revision was faster than the database option but still not fast, taking 10's of hours.  The newly written Java version performs this portion in seconds to minutes.

Data preparation and generation has all been completely re-written in Java and all the previous issues have been mitigated.  A 250 expressway set can now be generated from clean files in under 24 hours without a database.  File cleansing time has been halved to under three minutes per 1GB file from over six minutes per file.  And, raw file generation no longer requires a databases.

The tolls are simply a random table using the max carid after all the files have been combined.  So, if the max carid were 100 with two expressways then the tolls table would be carid's 1 thru 100, with a row for each carid-day combination, where days run from 1 thru 69.  Each historical toll row will have a random expressway from 0 or 1 and a random toll value from 0 thru 99.  So, the table size will be max carid * 69.  For our 50 expressway set the number of rows is 963,111,453.  Note that the random expressway will not match the expressway created associated with the position report tuple and must be corrected.  This is accounted for later.   

The original mitsim paper from 1996 can be found here https://its.mit.edu/sites/default/files/documents/MITSIM2.PDF.
