import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * Created by Sung Kim on 3/4/2016.
 *
 * This is also a non-database version of cleaning the tolls file.
 * This is also possible because we only need the type3's from the final data file.
 * How many are there?
 * Per 3h1x => 1% ~> 20K.
 * Thus, 250x => 5000K => 5M (that's not bad).
 * <p>
 * Usage: java fixtolls <rawTollFile> <finalDataFile> <output_finalTollFile>
 */
public class fixtolls {

    public static void main(String[] args) throws Exception {
        File rawTollFile = new File(args[0]);
        File finalDataFile = new File(args[1]);
        File outputFile = new File(args[2]);

        // Need carid, day, xway
        // Key: carid+"-"+day -- Value: xway
        HashMap<String, String> type3sInData = new HashMap<>();
        String line;
        String key;
        String[] tokens;

        // Read through the main data file, the final one and extract all Type 3 notifications.
        BufferedReader reader = new BufferedReader(new FileReader(finalDataFile));
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("3")) {
                tokens = line.split(",");
                key = tokens[2] + "-" + tokens[14];
                type3sInData.put(key, tokens[4]);
            }
        }
        reader.close();

        // Read through the newly generated toll file and if there's a matching key from the main data input file
        // ensure that the xways will match. Note, this seems a bit trivial but since the queries have an xway and it
        // forms part of the key of the Type 3 query, we need them to match.
        PrintWriter writer = new PrintWriter(outputFile);
        reader = new BufferedReader(new FileReader(rawTollFile));
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            key = tokens[0] + "-" + tokens[1];
            if (type3sInData.containsKey(key)) {
                tokens[2] = type3sInData.get(key);
            }
            writer.println(tokens[0] + "," + tokens[1] + "," + tokens[2] + "," + tokens[3]);
        }
        writer.close();
    }
}
