# linearroad
Walmart version of the Linear Road streaming benchmark.

## Overview
LinearRoad is a streaming data management system (SDMS) benchmark originally created in 2004.
It was created at a time when SDMS systems were relatively new.
The original Linear Road benchmark paper was a joint effort between collaborators from Stanford University, Brandeis University, Massachusetts Institute of Technology, and the Oregon Health and Science University/Oregon Graduate Institute.  And it has since been endorsed by Stanford, Brandeis, MIT, and Brown Universities as an SDMS benchmark.

All original files were downloaded from http://www.cs.brandeis.edu/~linearroad/tools.html.
These original files were then modified or re-written for performance reasons, including the creation of arbitrarily large datasets in a reasonable amount of time.

The spirit of the original files was followed.

This is a 0.1 release.

Changes will continually be made to bring the code closer to the intent of the original paper and new features will be added.

## Notes
Type 4 queries are not implemented as per the original paper, nor are Type 4 queries implemented in subsequent implementations.  We plan on implementing them in the near future.

The validator and many portions of data generation have been completely rewritten in Python.  The choice of Python was arbitrary.  At the moment, the validator is limited by RAM.  A version that leverages a NoSQL K/V store (currently Redis) to mitigate RAM issues is currently being developed and tested.  This memory limitation puts a boundary on the number of expressways that can currently be validated.

### Data Generation
Datasets of arbitrary sizes can be generated on a single machine or by parallelizing the expressway generation on multiple machines.  The original mitsim (microscopic traffic simulator) program creates each expressway as a separate file.  But, each file/expressway can take up to three hours or more to create.  The file size for a one expressway, three hour simulation, with 1,000 cars per segment per hour is ~1GB and will contain ~20M tuples.  Since each file is independent of all other files, you can parallelize the creation of these base files on as many machines or VM's as is desired.  

Each independent file created by mitsim is expressway 0, and each with its own independent car and query id numbering starting at ~0 till some _n_.  In order to combine an arbitrary number of these files into a single simulation file the expressway number, as well as the car and query ids must be incremented according to the number of expressways being combined.

Before combining the files we run some cleaning on the original, "raw" files to create a "clean"er set before running the combination.  This cleaning helps remove some noise from the data.  For example, some carids in the raw files will have exited but will magically reappear without going through an entry lane.

After cleaning, the initial combination process merges _n_ "clean" files, incrementing the expressway number from _0 thru n-1_ for each cleaned file.  It also increments the car and query ids by a current max car id and current max query id from the previous file to avoid overlap.

Then, the subsequent combination process creates the tolls and creates the random re-entrant cars by replacing a percentage of random cars by other random cars that meet the criteria of having an entry time _1000 * random.random() + 61_ greater than the exit time of another car.

The percentage of cars to check for possible re-entry is 10% by default.  Note that this does not mean 10% of the cars will be re-entrant but only that 10% will be checked to see if they _can_ be re-entrant.  And, this 10% is also not actually 10% of the actual number of cars, since the function used to create these possibly re-entrant cars uses _max carid_ and assumes the presence of carid's from 100 to _max carid_.  The 100 is arbitrary since carids below 100 exist.  But, more importantly, although carid's monotonically increase they do not so by only increments of 1.  The actual carid's present in a given expressway may be 5, 20, ..., 123, 124, 130, etc...  But, the 10% generated assumes the presence of  carid's 101, 102, 103, etc....  Nevertheless, a random number of carid's that represents _at most_ 10% of the actual carid's is created as _duplicatecars_.  Meaning, try to duplicate these cars--which is the same as try to make these cars re-entrant.

From this larger number of potential re-entrant carid's, a table with the _enter-time_, the _leave-time_, and the _expressway_ of each carid that actually exists in the generated data is created.  Then comes the phase where carid's in this new table is checked to see if a carid with an _enter-time > 1000 * random.random() + 61 + leave-time_ of another car exists.  And, if more than expressway is simulated, the carid's must be from different expressways.  We are simulating a car leaving one expressway and re-entering on a different expressway at a later point in time.  If only one expressway is present then we are simply simulating a car re-entering at a later point in time.  The _1000 * random.random() + 61_ appears to be arbitrary.  Python's random.random() returns a floating point number between 0.0 and 1.0, not including 1.0, or [0.0,1.0).

This process of making re-entrant carid's was a bottleneck of creating a single file from any arbitrary number of cleaned files.  The original SQL version used the following query:

_SELECT times.carid, times.entertime, times.leavetime, times_1.carid as carid1, times_1.entertime as entertime1, times_1.leavetime as leavetime1
FROM carsandtimes as times, carsandtimes AS times_1
WHERE times_1.entertime>times.leavetime+1000*random()+61
LIMIT 1;_

If a match is found the two carids that match are removed from the carsandtimes table and entered into a new carstoreplace table, which simply holds two carid's per row.

This query slows down tremendously as the "low-hanging fruit" is removed.  For perspective: for the 50 expressway data set there are 7,851,650 unique carid's with a max carid of 13,958,137.  From this max carid we get 1,395,100 potential duplicate, or re-entrant, cars per our description above.  And, we get 783,265 actual carid's that exist.  And, from these that actually exist we find, or create, 204,095 re-entrant cars.

The issue with the original SQL statement is the size of the self-joined table.  If attempted with this 50 expressway set the self-joined table would be up to 783,265 ^ 2, or 613,504,060,225 rows, from which to try and find an entry matching the WHERE clause.  Again, low hanging fruit can be found relatively quickly--from negligible to under a second.  But, as these easier finds are removed each additional match can run for many seconds, to minutes and tens of minutes, as up to the max rows above are potentially scanned.  Even if we limit the number of re-entrant cars to 200K, and even if the performance were steady at one per second, it would take over 55 hours to create the re-entrant cars.  But, as the query does slow down, and creation of even 200K would likely run into days, weeks, or more the above query was untenable.

To mitigate the issue above a separate script was created that creates the re-entrant cars.

~~This script creates the re-entrant cars in a single pass (or any number of passes).~~
~~All the cars in the actual existing cars are loaded into a list, shuffled, and that list is operated on to create a separate list of lists, or tuples, of _(carid,cartoreplace)_.~~
~~The current script tries 1,000 random times to find a suitable match.  If a match is found the current carid and the matching carid are removed.  Since a list is modified during iteration this means elements will be skipped if only one passed is used.  This script is itself an improvement over an O(n^2) version which simply iterated through a copy of the list to find a suitable replacement.  Ideally this script is O(n), but for almost 1M records it still had a run time of roughly 30 hours for a single pass.  Some modifications to improve the run time include reducing the number of tries to 500 or maybe 100.~~

~~Another option would be to stop looping the original query after an arbitrary number of replacements are found.  Or, stop when queries start taking more than some arbitrary number of seconds.~~

Data preparation and generation has been completely re-written in Java and all the previous issues have been mitigated.  A 250 expressway set can now be generated from clean files in under 24 hours without a database.  File cleansing time has been halved.  Raw file generation no longer requires a databases.

The tolls are simply a random table using the max carid after all the files have been combined.  So, if the max carid were 100 with two expressways then the tolls table would be carid's 1 thru 100, with a row for each carid-day combination, where days run from 1 thru 69.  Each historical toll row will have a random expressway from 0 or 1 and a random toll value from 0 thru 99.  So, the table size will be max carid * 69.  For our 50 expressway set the number of rows is 963,111,453.  Note that the random expressway will not match the expressway created associated with the position report tuple.  This is accounted for later.   

The original mitsim paper from 1996 can be found here https://its.mit.edu/sites/default/files/documents/MITSIM2.PDF.
