import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Sung Kim on 3/3/2016.
 * replacecars
 * A NON-database replacecars.  It should be performant since the simple act of loading a file into a db can take a lot of time.
 * BUT, also write out each xway to a separate file to combine later in time order
 * Usage: java replacecars <carstoreplace_file> <combined_file>  <outfile_base_name>
 */
public class replacecars {
    public static void main(String[] args) throws Exception {
        File carsToReplaceFile = new File(args[0]);
        File combinedFile = new File(args[1]);
        String outfileBaseName = args[2];
        HashMap<String, String> carsToReplace = new HashMap<>();  // Test with ints afterwards
        BufferedReader reader;
        PrintWriter writer;
        String tokens[];
        String line;
        StringBuilder out_token = new StringBuilder();
        int xway, time, lastTime;

        reader = new BufferedReader(new FileReader(carsToReplaceFile));
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            carsToReplace.put(tokens[1], tokens[0]);  // This needed to be reversed.  Why?  Because we want to find the car _to replace_ and that's best placed as the key not the val.
        }
        reader.close();

        xway = 0;
        time = 0;
        lastTime = 0;
        reader = new BufferedReader(new FileReader(combinedFile));
        writer = new PrintWriter(new File(outfileBaseName+"-"+xway));
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            if (carsToReplace.containsKey(tokens[2])) {
                tokens[2] = carsToReplace.get(tokens[2]);
            }
            time = Integer.parseInt(tokens[1]);
            if (lastTime == 10784 && time == 0) {  // File transition
                writer.close();
                xway++;
                writer = new PrintWriter(new File(outfileBaseName+"-"+xway));
            }
            lastTime = time;
            // The files are sequential, so you can know where all the type 2's and 3's belong.
            tokens[4] = Integer.toString(xway);
            dataval.printTokensToFile(writer, out_token, tokens);
        }
        reader.close();
        writer.close();
    }
}
