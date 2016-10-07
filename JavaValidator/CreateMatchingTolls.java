import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * Created by Sung Kim on 3/11/2016.
 * Create a file with only those tolls that actually show up in the data file.
 * This makes creating a Validation file much faster.
 * If there's no desire to test the ability of a data store then using a cleaned file to run the actual tests
 * for candidate Streaming data processing systems would work just as well.
 */

public class CreateMatchingTolls {
    public static void createMatchingTollsFile(String mainFile, String tollFile, String newFile) throws Exception {
        File datafile = new File(mainFile);
        File tollfile = new File(tollFile);
        File newtfile = new File(newFile);

        HashMap<String, Integer> t3s = new HashMap<>();     // carid, day, xway -> t[2], t[14], t[4]

        // Load type 3's
        BufferedReader reader = new BufferedReader(new FileReader(datafile));
        String line;
        String[] tokens;
        String key;
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            if (tokens[0].equals("3")) {
                key = tokens[2] + "-" + tokens[14] + "-" + tokens[4];
                t3s.put(key, null);
            }
        }
        reader.close();

        // Find actual matching lines in the tolls files and write those out to a new file
        reader = new BufferedReader(new FileReader(tollfile));
        PrintWriter writer = new PrintWriter(newtfile);
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            // carid, day, xway, toll
            key = tokens[0] + "-" + tokens[1] + "-" + tokens[2];
            if (t3s.containsKey(key)) {
                writer.println(line);
            }
        }
        reader.close();
        writer.close();
    }

    public static void main(String[] args) throws Exception {
       createMatchingTollsFile(args[0], args[1], args[3]);
    }
}
