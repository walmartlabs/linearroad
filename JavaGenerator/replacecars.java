import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Sung Kim on 3/3/2016.
 *
 * A NON-database way to replacecars.
 * create_carsandtimes.java and create_carstoreplace.java can be used to create the files that the original Python
 * version uses to then do the actual replacements on the cars in the main data file, which would have been loaded
 * into a database. The whole database stop being too slow, and adding a level of required infrastructure, it is
 * better to simply to everything outside of the database, in files and scripts.
 *
 * This is performant since the simple act of loading a file into a db takes time.
 *
 * This replacecars.java also writes out each xway to a separate file to be combined later in time order by
 * combine_after_replace.java. This last step is actually <b>NECESSARY</b> as the non-database version of creating a
 * combined file does NOT order the cleaned, combined file.
 *
 * Usage: java replacecars <carstoreplace_file> <combined_file>  <outfile_base_name>
 */
public class replacecars {
    public static void main(String[] args) throws Exception {
        File carsToReplaceFile = new File(args[0]);
        File combinedFile = new File(args[1]);
        String outfileBaseName = args[2];

        BufferedReader reader;
        PrintWriter writer;

        String tokens[];
        String line;
        StringBuilder out_token = new StringBuilder();

        int xway, time, lastTime;

        reader = new BufferedReader(new FileReader(carsToReplaceFile));
        // Hold the carstoreplace file in a map.
        // The file will have the re-entrant car at pos. 0 and the to-be-replaced car at pos. 1.
        HashMap<String, String> carsToReplace = new HashMap<>();
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            // We're looking for the car to be replaced as the key, not the re-entrant car.
            // When we find the car to be replaced, tokens[1], we'll replace that car with the re-entrant car,
            // which is tokens[0].
            carsToReplace.put(tokens[1], tokens[0]);
        }
        reader.close();

        xway = 0;
        time = 0;
        lastTime = 0;
        // Read the cleaned, combined (but not yet time-ordered) main data file.
        reader = new BufferedReader(new FileReader(combinedFile));
        // Start with the initial file.
        writer = new PrintWriter(new File(outfileBaseName+"-"+xway));
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");

            // Make the replacement of the car if it's a car that needs to be replaced (by the value, whicih is the
            // car that will now be re-entrant.
            if (carsToReplace.containsKey(tokens[2])) {
                tokens[2] = carsToReplace.get(tokens[2]);
            }

            // We get the time to be able to check for file transitions.
            time = Integer.parseInt(tokens[1]);
            // File transition in the original file, one xway to the next.
            // If there is only one xway, this segment will not run, which is fine.
            if (lastTime == 10784 && time == 0) {
                writer.close();
                xway++;
                writer = new PrintWriter(new File(outfileBaseName+"-"+xway));
            }
            lastTime = time;

            // The files are sequential, so all the Type 2's and 3's simply flow with the proper xway and dont' have
            // to be accounted for here, in terms of placement.
            tokens[4] = Integer.toString(xway);
            dataval.printTokensToFile(writer, out_token, tokens);
        }
        reader.close();
        writer.close();
    }
}
