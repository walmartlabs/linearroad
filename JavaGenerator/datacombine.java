import java.io.*;

/**
 * Created by Sung Kim on 3/2/2016.
 * Based on datacombine.py.
 *
 * A Java version to take a directory of cleaned linear road data files output from the MITSIM simulator
 * and combine them into a single large file.
 *
 * This is necessary because each linear road file stands as an independent file with its own max car and query id.
 *
 * The one thing this combination doesn't do is propertly set the time for the combined file.
 * So, the combined file will have n 0...10784, 0...10784 segments.
 * This is reconciled after finding the replacement cars in 'replacecars.java' and 'combine_after_replace.java'.
 * The first of the above splits out the files into individual files per xway.
 * The latter combines them into properly time-ordered fashion in a single file.
 *
 * Usage: java datacombine <DIR of cleaned files> <output file>
 *
 */
public class datacombine {

    // The number of fields in a line of input.
    final static int NUM_FIELDS = 15;

    public static void main(String[] args) throws Exception {

        // Simple check for valid number of input parameters.
        if (args.length != 2) {
            System.err.println("Usage: java datacombine <DIR of cleaned files> <output file>");
            System.exit(1);
        }

        // The directory that holds the cleaned files.
        File dir = new File(args[0]);
        // The name of the new single combined outfile.
        File outfile = new File(args[1]);

        // Ensure we're working with a directory, otherwise exit.
        if (!dir.isDirectory()) {
            System.err.println(dir.getName() + " is not a directory.");
            System.exit(1);
        }

        // To write out the combined file.
        PrintWriter writer = new PrintWriter(outfile);

        BufferedReader reader;
        String line;
        String[] tokens;
        // Hold Integer versions of the String tokens we get from splitting/tokenizing the line.
        int[] itokens = new int[NUM_FIELDS];

        StringBuilder out_token = new StringBuilder();

        // Overall max's.
        int maxCarId = 0;
        int maxQId = 0;
        // Used for xway number.
        int fileCount = 0;
        // Current file max's.
        int curMaxCarId;
        int curMaxQId;

        // Open each file from the directory and read/process it.
        for (File f : dir.listFiles()) {
            // Create a reader for the current file.
            reader = new BufferedReader(new FileReader(f));
            // Reset the current maxes for the carid and queryid.
            curMaxCarId = 0;
            curMaxQId = 0;

            // Read the file and make the required adjustments.
            while ((line = reader.readLine()) != null) {
                tokens = line.split(",");

                // Convert the String tokens into an array of int's.
                int i = 0;
                for (String t : tokens) {
                    itokens[i++] = Integer.parseInt(t);
                }

                // Update current max's for carid's and queryid's if applicable.
                if (itokens[2] > curMaxCarId) curMaxCarId = itokens[2];
                if (itokens[9] > curMaxQId) curMaxQId = itokens[9];

                // Adjust the carid's, queryid's, and xways for data creations with greater than a single file/xway.
                if (fileCount > 0) {
                    itokens[2] += maxCarId;
                    if (itokens[0] != 0) itokens[9] += maxQId; // Update queryid's only for non-Type 0 notifications.
                    if (itokens[0] == 0) itokens[4] = fileCount; // Update the xway number.
                }

                // Write the newly adjust line to the outfile.
                dataval.printITokensToFile(writer, out_token, itokens);
            }

            // Update the overall max's to be used.
            maxCarId += curMaxCarId+1;
            maxQId += curMaxQId+1;
            fileCount++;
        }
        writer.close();

        // Print the max carid so we can use it to build the historical tolls file.
        System.out.println(maxCarId-1);
    }
}
