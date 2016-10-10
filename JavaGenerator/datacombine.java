import java.io.*;

/**
 * Created by Sung Kim on 3/2/2016.
 * A Java version to take a directory of cleaned linear road data files output from the MITSIM simulator
 * and combine them into a single large file.
 *
 * This is necessary because each linear road file stands as an independent file with its own max car and query id.
 *
 * Usage: java datacombine <DIR of cleaned files> <output file>
 *
 */
public class datacombine {

    final static int NUM_FIELDS = 15;

    public static void main(String[] args) throws Exception {

        // Simple check for valid number of input parameters
        if (args.length != 2) {
            System.err.println("Usage: java datacombine <DIR of cleaned files> <output file>");
            System.exit(1);
        }

        // The directory that holds the cleaned files.
        File dir = new File(args[0]);
        // The name of the new single combined outfile.
        File outfile = new File(args[1]);

        // Ensure we're working with a directory.
        if (!dir.isDirectory()) {
            System.err.println(dir.getName() + " is not a directory.");
            System.exit(1);
        }

        PrintWriter writer = new PrintWriter(outfile);

        BufferedReader reader;
        String line;
        String[] tokens;
        int[] itokens = new int[15];
        int i;
        StringBuilder out_token = new StringBuilder();

        // Overall max's
        int maxCarId = 0;
        int maxQId = 0;
        // Used for xway number
        int fileCount = 0;
        // Current file max's
        int curMaxCarId;
        int curMaxQId;

        // Open each file from the directory and read/process it.
        for (File f : dir.listFiles()) {
            //DEBUG
            // System.out.println(f);
            //DEBUG END
            reader = new BufferedReader(new FileReader(f));
            curMaxCarId = 0;
            curMaxQId = 0;
            while ((line = reader.readLine()) != null) {
                tokens = line.split(",");

                // Convert the String tokens into an array of int's.
                i = 0;
                for (String t : tokens) {
                    itokens[i++] = Integer.parseInt(t);
                }

                if (itokens[2] > curMaxCarId) curMaxCarId = itokens[2];
                if (itokens[9] > curMaxQId) curMaxQId = itokens[9];

                // Adjust the carid's, queryid's, and xways for data creations with greater than a single file/xway.
                if (fileCount > 0) {
                    itokens[2] += maxCarId;
                    if (itokens[0] != 0) itokens[9] += maxQId;
                    if (itokens[0] == 0) itokens[4] = fileCount;
                }

                // Write the newly adjust line to the outfile
                dataval.printITokensToFile(writer, out_token, itokens);
            }

            // Update the overall max's to be used
            maxCarId += curMaxCarId+1;
            maxQId += curMaxQId+1;
            fileCount++;
        }
        writer.close();

        // Print the max carid so we can use it to build the historical tolls file
        System.out.println(maxCarId-1);
    }
}
