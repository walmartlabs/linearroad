import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by Sung Kim on 3/4/2016.
 *
 * After using relacecars.java we have multiple files that need to be combined in time order.
 * *****************************************************************************************************************
 * This step, along with replacecars (even if there are no cars to replace) is ABSOLUTELY NECESSARY because the non-
 * database version of combining files does NOT time order the records as a database normally would if the records
 * were inserted into a database and then pulled ORDER(ed) BY time.
 * *****************************************************************************************************************
 * That means opening one writer but having N readers.
 * Each reader will write all its time values for a given time/second before moving onto the next time/second.
 * <p>
 * Usage: java combine_after_replace <dir of replaced files> <output_finalDataFile>
 */
public class combine_after_replace {
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        File outputFile = new File(args[1]);

        String line;
        String[] tokens;

        int time, currTime = 0; // We start at time 0, of course.

        // Make sure we are pulling from a directory of split files. We are not currently checking for file validity.
        // The user needs to validate no other files from files output from replacecars.java are in the folder.
        if (!dir.isDirectory()) {
            System.err.println(dir + " is not a directory");
            System.exit(1);
        }

        // A list of readers, one for each file.
        ArrayList<BufferedReader> readers = new ArrayList<>();
        for (File f : dir.listFiles()) {
            readers.add(new BufferedReader(new FileReader(f)));
        }
        // An array of 'lastTimes seen' for each reader.
        int[] lastTimes = new int[readers.size()];

        // When we transition from one sec to the next we need to keep the first line for writing on the next loop.
        String[] firstLines = new String[readers.size()];

        int index;
        PrintWriter writer = new PrintWriter(outputFile);
        while (true) {

            // The index of the reader we're currently reading from
            index = 0;

            // We just cycle through the readers, reading complete sections of a given second at a time,
            // for all the files, each round.
            for (BufferedReader reader : readers) {
                //System.out.println("Reading from: " + reader); // DEBUG/FEEDBACK

                // Write out a first for this second, if there's one available.
                if (firstLines[index] != null) {
                    writer.println(firstLines[index]);
                }

                // Now read for the given second from this file.
                while (true) {
                    line = reader.readLine();

                    // If we get to the end of the file, go ahead and try the next file.
                    // This is the EOF condition.
                    if (line == null) {
                        index++;
                        break; // EOF
                    }
                    tokens = line.split(",");
                    time = Integer.parseInt(tokens[1]);

                    // Transitioning to a new time/second,
                    // so save the first line,
                    // and let the next reader read.
                    if (time > lastTimes[index]) {
                        lastTimes[index] = time;
                        //System.out.println("New second for reader: " + reader.toString()); // DEBUG/FEEDBACK
                        firstLines[index++] = line;
                        // Breaks from this current while loop that is reading this current file.
                        // Goes to choosing a new BufferedReader.
                        break;
                    }

                    // This file is still on the current time/second so Write the given line for the current file.
                    writer.println(line);
                    // Update the last time for this current reader/file.
                    lastTimes[index] = time;
                }
            }

            // After reading (and writing) all the files for a given second, increment the time and read all the files
            // again for the next second.
            currTime++;

            if (currTime > 10784) break; // Done.
        }
        writer.close();
    }
}
