import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * Created by Sung Kim on 3/2/2016.
 * ====
 * Note: This step isn't really necessary anymore and can be removed.
 * ====
 * Based on datamakeexit.py: Ensure that all vehicles get off the xway.
 * Run after dataval.java and datarm2.java.
 * Usage: java datamakeexit <file>  <outfile>
 */
public class datamakeexit {
    // Arbitrary max carid for running 1000 cars / min / segment as set in <>.pl.
    static final int MAX_CARID = 300000;

    // Just throwing the Exception away for now.
    public static void main(String[] args) throws Exception {

        // Create the array to hold the max times for each car.
        int[] lasttimes = new int[MAX_CARID];

        String input_file_name = args[0];
        String output_file_name = args[1];

        File input_file = new File(input_file_name);
        File output_file = new File(output_file_name);

        // For reading the infile twice and writing out the modified data file.
        BufferedReader reader;
        PrintWriter writer;

        // For reading the lines.
        String line;
        String[] tokens;
        StringBuilder out_token = new StringBuilder();

        // Read the file and find the last time for each vehicle.
        reader = new BufferedReader(new FileReader(input_file));
        // We're timing how long it takes to read the file each time.
        long st = System.nanoTime();
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            int carid = Integer.parseInt(tokens[2]);
            int time = Integer.parseInt(tokens[1]);
            lasttimes[carid] = time;
        }
        reader.close();
        System.err.println("Time for first read: " + ((System.nanoTime() - st)/1000000));

        // Go back to the beginning of the file and re-read,
        // when the last notification for a car is seen modify the line to make it an exiting notification.
        // We time again.
        st = System.nanoTime();
        reader = new BufferedReader(new FileReader(input_file));
        writer = new PrintWriter(output_file);
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            int carid = Integer.parseInt(tokens[2]);
            int time = Integer.parseInt(tokens[1]);
            // Only last appearing type 0 queries need adjustment.
            if (time == lasttimes[carid] && tokens[0].equals("0")) {
                tokens[3] = "10";
                tokens[5] = "4";
            }
            dataval.printTokensToFile(writer, out_token, tokens);
        }
        reader.close();
        writer.close();
        System.err.println("Time for second read: " + ((System.nanoTime() - st)/1000000));
    }
}
