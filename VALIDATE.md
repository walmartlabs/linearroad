# Validating Results

## Notes

## Generating the Validation File (or the expected output)
The original validator was written in Python and was a collaborative effort based on some of the idiosyncracies in the data as found by the various vendor-participants.  It is all in-memory, using Python dictionaries, so the number of expressways that can be validated is limited by available memory.  The original was also single-threaded.  

One technique to reduce the memory footprint for validation is to reduce the historical tolls files to only those records that actually match a query within the main data file.  The newer Java version does this automatically, while also being multi-threaded.

The creation of expected output and the comparison to output created by any potential solution are two separate steps.

To create the file of expected output:
`java ValidateMTBQ<*additional suffixes*> <LR input file> <num xways> <toll file>`

Output will be a file named `out` in the current directory.

To run a comparison of the expected output with the output of a streaming product run:
`java CompareFiles <validator output> <product output>`
The expected output is loaded into a Java Map and the product output is read line-by-line and checked against what is present in the Map. Product output that is not found in the expected output,  product output values outside the expected ranges, or product output not matching the expected output are flagged.  This stage of validation is also limited by available memory.

Various solutions using various database backends to store state while generating the expected output were used but all were slower (some by orders of magnitude) than the Java + Aerospike combination.  When time permits further work may be done to increase the xway sizes that can be validated in a timely manner, "timely" being the key word. 
