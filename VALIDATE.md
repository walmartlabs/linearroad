# Validating Results

## Notes

## Generating the Validation File (or the expected output)
The original validator was written in Python and was a collaborative effort based on some of the idiosyncracies in the data as found by the various vendor-participants.  The original Python validator was all in-memory, using Python dictionaries, so the number of expressways that can be validated was limited by available memory.  The original was also single-threaded.  

One technique to reduce the memory footprint for validation is to reduce the historical tolls files to only those records that actually match a query within the main data file.  The newer Java version does this automatically, while also being multi-threaded.

The Java version is found here: https://github.com/walmart/linearroad/tree/master/JavaValidator

The current Java version uses Aerospike to hold the toll state for all cars. Initial testing showed using Aerospike for this purpose allowed for validation file creation in less time, for large sets, in less time than even using Java's HashMap. 

The creation of expected output and the comparison to output created by any potential solution are two separate steps.

To create the file of expected output:

```time java ValidateMTBQEven3AeroTolls <datafile: path> <num XWays: int> <tollfile: path>```

MT (Multi-Threaded) BQ (Blocking Queue) Even (wait till all threads have processed each second before proceeding to next second) Aero (uses AeroSpike) Tolls (cleans the toll file)

Output will be a file named `out` in the current directory.

To run a comparison of the expected output with the output of a streaming product run:

```java CompareFiles <validator output> <product output>```

The expected output is loaded into a Java Map and the product output is read line-by-line and checked against what is present in the Map. Product output that is not found in the expected output,  product output values outside the expected ranges, or product output not matching the expected output are flagged.  This stage of validation is also limited by available memory.

Various solutions using various database backends to store state while generating the expected output were used but all were slower (some by orders of magnitude) than the Java + Aerospike combination.  When time permits further work may be done to increase the xway sizes that can be validated in a timely manner, "timely" being the key word. 
