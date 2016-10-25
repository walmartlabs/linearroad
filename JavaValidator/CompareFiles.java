import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

/**
 * Created by Sung Kim on 5/30/2016.
 * Compare the output from a product run versus the expected output from the Validator.
 *
 * We check existence of an expected line of output and then check the values of that output.
 *
 * We don't check for extra lines in the output.
 *
 * Usage: java CompareFiles <validator output> <product output>
 * Caveat: This version will only work up to the limits of the memory of the machine on which it's run.
 */
public class CompareFiles {
    /**
     * A convenience class to help us work with the data.
      */
    private static class KVTuple {
        String key;
        String value;

        KVTuple(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + " -- " + value;
        }
    }

    /**
     * Based on the type of the input line, return the properly deconstructed key and values.
     * This de-multiplexes the multiplexing of all types into a single data line.
     * @param tokens
     * @return
     */
    private static KVTuple getKVFromType(String[] tokens) {
        String key = null;
        String value = null;
        // [], square brackets, represent the "key" for each Type of validator and solution output.
        // [0, carid, time], proc_time, lav, toll
        // [1, time], proc_time, [xway, acc_seg, dir, carid]
        // [2, time], proc_time, toll_time, [qid], balance
        // [3, time], proc_time, [qid, toll]
        switch (tokens[0]) {
            case "0":
                key = tokens[0] + ";" + tokens[1] + ";" + tokens[2];
                value = tokens[3] + ";" + tokens[4] + ";" + tokens[5];
                break;
            case "1":
                key = tokens[0] + ";" + tokens[1] + ";" + tokens[3] + ";" + tokens[4] + ";" + tokens[5] + ";" + tokens[6];
                value = tokens[2];
                break;
            case "2":
            case "5":
                key = tokens[0] + ";" + tokens[1] + ";" + tokens[4];
                value = tokens[2] + ";" + tokens[3] + ";" + tokens[5];
                break;
            case "3":
                key = tokens[0] + ";" + tokens[1] + ";" + tokens[3] + ";" + tokens[4];
                value = tokens[2];
                break;
        }
        return new CompareFiles.KVTuple(key, value);
    }

    /**
     * Create a KVTuple object from a parse line of validator output and place into the validator output Map.
     *
     * @param output
     * @param tokens
     */
    private static void insertType(HashMap<String, String> output, String[] tokens) {
        CompareFiles.KVTuple kv = getKVFromType(tokens);
        output.put(kv.key, kv.value);
    }

    /**
     * Take the file created by the Validator and upload it, as the proper types, into a passed-by-reference Map.
     *
     * @param output The HashMap to hold the validator output split into key/value
     * @param file   The validator output file
     */
    private static long loadValidatorOutput(HashMap<String, String> output, String file) {
        BufferedReader reader;
        String line;
        String[] tokens;
        long numValidatorRecords = 0;
        try {
            reader = new BufferedReader(new FileReader(new File(file)));
            while ((line = reader.readLine()) != null) {
                tokens = line.split(",");
                insertType(output, tokens);
                numValidatorRecords++;
            }
        } catch (Exception e) {
            System.err.println(e);
        }
        return numValidatorRecords;
    }

    /**
     * The bulk of the validation will occur here.
     * Based on the values of a given key, which tests existence (the presence of the key means the solution output
     * has some expected output), test the values against the expected values.
     *
     * @param vOutput
     * @param kv
     * @param type
     * @return
     */
    private static long checkType(HashMap<String, String> vOutput, KVTuple kv, String type) {
        String vValue = vOutput.get(kv.key);
        //System.out.println("kv: " + kv);
        if (vValue == null) {
            System.out.println("Key " + kv.key + " does not exist in validator output.");
            return 0;
        }
        // If the key exists, check the processing time, the lav, and the toll
        String[] vValues = vValue.split(";");
        String[] pValues = kv.value.split(";");

        // All values start with the proc_time field
        if (Double.valueOf(pValues[0]) > 5000.0)
            System.out.println(kv.key + " has time > 5"); // Remember, System.currentTimeMillis() is milliseconds since Jan 1, 1970

        switch (type) {
            case "0":
                // [0, carid, time], proc_time, lav, toll
                if (Integer.parseInt(pValues[2]) != Integer.parseInt(vValues[2]))
                    System.out.println(kv.key + " has non-matching toll of " + pValues[2] + "; " + vValues[2] + " expected.");
                break;
            case "1":
                // [1, time], proc_time, [xway, acc_seg, dir, carid]
                // For accidents, besides existence, the only thing to check is the proc time
                break;
            case "2":
            case "5":
                // [2, time], proc_time, toll_time, [qid], balance
                if (Integer.parseInt(pValues[1]) != Integer.parseInt(vValues[1]))
                    System.out.println(kv.key + " has non-matching toll time of " + pValues[1] + "; " + vValues[1] + " expected.");
                if (Integer.parseInt(pValues[2]) != Integer.parseInt(vValues[2]))
                    System.out.println(kv.key + " has non-matching balance of " + pValues[2] + "; " + vValues[2] + " expected.");
                break;
            case "3":
                // [3, time], proc_time, [qid, toll]
                // For toll history, everything should match so proc_time is the only thing to check
                break;
            default:
                //System.err.println("Odd product output line: " + line);
        }
        return 1;
    }

    /**
     * Just a wrapper to checkType(). More functionality can be added, i.e. more types of checks, here.
     *
     * @param vOutput
     * @param line
     * @return
     */
    private static long checkLine(HashMap<String, String> vOutput, String line) {
        String[] tokens;
        tokens = line.split(",");
        return checkType(vOutput, getKVFromType(tokens), tokens[0]);
    }

    /**
     * Take productOutput file and find the corresponding keys (and values) in the built up validatorOutput Map.
     * This is the main function that starts validation after a validator-produced file is uploaded.
     *
     * @param validatorOutput   The filled HashMap of expected output created by the Validator.
     * @param productOutputFile The fileName of the product output.
     */
    private static long compareProductOutput(HashMap<String, String> validatorOutput, String productOutputFile) {
        BufferedReader reader;
        String line;
        long recordCount = 0;
        try {
            reader = new BufferedReader(new FileReader(new File(productOutputFile)));
            while ((line = reader.readLine()) != null) {
                recordCount += checkLine(validatorOutput, line);
            }
        } catch (Exception e) {
            System.err.println(e);
        }
        return recordCount;
    }

    /**
     * Run the validator.
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 2) {
            System.out.println("Usage: java CompareFiles <validator output> <product output>");
            System.exit(1);
        }
        // Load the validator output into a hash map
        HashMap<String, String> validatorOutput = new HashMap<>();
        // Holder for number of Product records processed
        long numValidatorRecords = 0;
        long numProductRecords = 0;
        // How big are output files again? About 1/5 the size of the input, which is still rather large
        // Keep it simple, [] represents the keys
        // [0, carid, time], proc_time, lav, toll
        // [1, time], proc_time, [xway, acc_seg, dir, carid]
        // [2, time], proc_time, toll_time, [qid], balance
        // [3, time], proc_time, [qid, toll]
        numValidatorRecords = loadValidatorOutput(validatorOutput, args[0]);
        numProductRecords = compareProductOutput(validatorOutput, args[1]);
        System.out.println("Total number of records in Validator file: " + numValidatorRecords);
        System.out.println("Total number of records in Product file: " + numProductRecords);
    }
}
