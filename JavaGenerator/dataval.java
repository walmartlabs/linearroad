import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * Created by Sung Kim on 3/2/2016.
 * Based on dataval.py: with a single raw mitsim file performs the following:
 * 1) check for position reports that are not 30 secs apart, and simply report.
 * 2) ensure car does not reappear after exiting.
 * 3) remove negative positions and segments.
 * 4) remove type 3 queries with a  day of '0' if any.
 *
 * This script will usually be used in conjunction with other scripts in a chain.
 * See: https://github.com/walmart/linearroad/GENERATEDATA.md
 *
 * Usage: java dataval <raw_file> <cleaner_file>
*/
public class dataval {
    /**
     * A max to comfortably hold up to the max possible car id when using files generated
     * with 1000 cars / min / seg.
     */
    static final int MAX_CARID = 300000;

    /**
     * We put some utility functions here.
     */
    /**
     * Print an array of tokens as Strings to stdout.
     * @param out_token
     * @param tokens
     */
    public static void printTokens(StringBuilder out_token, String[] tokens) {
        out_token.setLength(0);
        for (String token : tokens) {
            out_token.append(token+",");
        }
        System.out.println(out_token.deleteCharAt(out_token.length()-1));
    }

    /**
     * Print an array of tokens as Strings to a PrintWriter.
     * @param writer
     * @param out_token
     * @param tokens
     */
    public static void printTokensToFile(PrintWriter writer, StringBuilder out_token, String[] tokens) {
        out_token.setLength(0);
        for (String token : tokens) {
            out_token.append(token+",");
        }
        writer.println(out_token.deleteCharAt(out_token.length()-1));
    }

    /**
     * Print an array of tokens as Integers to a PrintWriter.
     * @param writer
     * @param out_token
     * @param tokens
     */
    static void printITokensToFile(PrintWriter writer, StringBuilder out_token, int[] tokens) {
        out_token.setLength(0);
        for (int token : tokens) {
            out_token.append(token+",");
        }
        writer.println(out_token.deleteCharAt(out_token.length()-1));
    }

    /**
     * Clean the file.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // For timing.
        long st = System.nanoTime();

        String raw_file_name = args[0];
        String out_file_name = args[1];

        // For feedback.
        System.out.println("Validating data file: " + raw_file_name);

        // We're using a simple array, which will end up being half full since, for performance.
        // Each carid easily maps into an array index and we only need to track its last time.
        int[] cars = new int[MAX_CARID];
        // We need to track aleady exited cars to prevent magic re-entries.
        int[] exited = new int[MAX_CARID];

        File raw_file = new File(raw_file_name);
        File out_file = new File(out_file_name);

        BufferedReader reader;
        PrintWriter writer;

        reader = new BufferedReader(new FileReader(raw_file));
        writer = new PrintWriter(out_file);

        String line;
        StringBuilder out_token = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split(",");

            // Type 0 notifications.
            if (tokens[0].equals("0")) {
                int time = Integer.parseInt(tokens[1]);
                int carid = Integer.parseInt(tokens[2]);
                int lane = Integer.parseInt(tokens[5]);
                int seg = Integer.parseInt(tokens[7]);

                // If this car has already exited ignore this notification and move to the next one.
                if (exited[carid] != 0) {
                    continue;
                }

                // Java initializes array values to 0, so if this is the first time this car has been seen
                // set its time to the time parsed from the current line.
                if (cars[carid] == 0) {
                    cars[carid] = time;
                } else { // Otherwise, since this car has been seen before check if this new line has a time that's not
                    // 30 seconds greater than the last registered time.
                    // One of the reasons this appears to happen is when the mitsim generator creates stopped cars
                    // or accidents.
                    if (cars[carid] != time - 30) {
                        // We don't do anything, we just make of note of it.
                        System.out.println(cars[carid] + "-" + time);
                        System.out.println("Time error for car " + carid + " at time " + time);
                    }
                    // Update the car with this 'last notifiication' time.
                    cars[carid] = time;
                }

                // If the car is exiting, note it in the exited array.
                if (tokens[5].equals("4")) {
                    exited[carid] = time;
                }

                // Fix any negative segments in the data; both the segment and the x-position.
                if (seg < 0) {
                    printTokens(out_token, tokens);
                    tokens[7] = "0";
                    tokens[8] = "0";
                }
            } else if (tokens[0].equals("2")) { // Ignore Type 2 notifications.
            } else if (tokens[0].equals("3")) { // Ignore 0 day Type 3's.
               if (tokens[14].equals("0")) { // A Type 3 query should be asking for days > 0. Day 0 is really a Type 2.
                   // So, skip if there's a day 0 Type 3.
                   continue;
               }
            }

            // Write out the line with any modifications made.
            printTokensToFile(writer, out_token, tokens);
        }
        reader.close();
        writer.close();

        System.out.println("Time to run dataval.java: " + ((System.nanoTime() - st)/1000000));
    }
}

