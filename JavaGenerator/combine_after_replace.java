import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by Sung Kim on 3/4/2016.
 * After using replacecars_1 we have multiple files that need to be combined in time order
 * That means opening one writer but n readers, writing in turn based on time
 * <p>
 * Usage: java combine_after_replace <dir of replaced files> <output_finalDataFile>
 */
public class combine_after_replace {
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        File outputFile = new File(args[1]);
        String line;
        String[] tokens;
        int time, currTime = 0; // We start at time 0

        if (!dir.isDirectory()) {
            System.err.println(dir + " is not a directory");
            System.exit(1);
        }

        ArrayList<BufferedReader> readers = new ArrayList<>();
        for (File f : dir.listFiles()) {
            readers.add(new BufferedReader(new FileReader(f)));
        }
        int[] lastTimes = new int[readers.size()];
        String[] firstLines = new String[readers.size()]; // When we transition from one sec to the next we need to keep the first line for writing on the next loop

        int index;
        PrintWriter writer = new PrintWriter(outputFile);
        while (true) {
            index = 0;  // The index of the reader we're currently reading from
            for (BufferedReader reader : readers) {
                //System.out.println("Reading from: " + reader);
                if (firstLines[index] != null) {
                    writer.println(firstLines[index]);
                }
                while (true) {
                    line = reader.readLine();
                    if (line == null) {
                        index++;
                        break; // EOF
                    }
                    tokens = line.split(",");
                    time = Integer.parseInt(tokens[1]);
                    if (time > lastTimes[index]) {
                        lastTimes[index] = time;
                        //System.out.println("New second for reader: " + reader.toString());
                        firstLines[index++] = line;
                        break;
                    }
                    writer.println(line);
                    lastTimes[index] = time;
                }
            }
            currTime++;
            //System.out.println(currTime); // Each loop through every reader is one simulation second
            if (currTime > 10784) break; // Done
        }
        writer.close();
    }
}
