import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sung Kim on 3/29/2016.
 * Take an input file and split into n*2 files for multi-thread processing
 * The bulk of the work is to write types > 0 to the proper file.
 * We do this by storing the xway+dir of carids and assigning lines to the proper files accordingly.
 * Using the BlockingQueue version obviates this file splitting.
 */
public class SplitFiles {
    // Have auto-detection of number of xways
    public static void splitFiles(String input, int numXways) throws Exception {
        Map<String, PrintWriter> files = new HashMap<>();  // xway - dir: writer; xd is the key
        Map<String, String> carsAndXwayDir = new HashMap<>(); // carid: xway - dir
        Map<String, String> holding = new HashMap<>(); // carid - time: line;
        BufferedReader reader = new BufferedReader(new FileReader(input));
        String line;
        String[] tokens;
        int currTime = -1; // used in conjunction with holding to appropriately write t2 and t3
        for (int i = 0; i < numXways; i++) {
            String key0 = i + "-0"; // 0 - indexed
            String key1 = i + "-1";
            System.out.println(key0);
            System.out.println(key1);
            files.put(key0, new PrintWriter(new File(key0)));
            files.put(key1, new PrintWriter(new File(key1)));
        }

        while ((line = reader.readLine()) != null) {
            tokens = line.trim().split(",");
            //when the time changes we 'll process these in holding
            if (Integer.parseInt(tokens[1]) > currTime) {
                //  process all in holding
                for (String k : holding.keySet()) {
                    // Get the carid from the carid-time key to get the xway-dir to feed into files
                    files.get(carsAndXwayDir.get(k.split("-")[0])).printf("%s\n", holding.get(k)); // We 're just getting the carid, and remember to print the lines in holding, not the current line !
                }
                holding.clear();
            }
            currTime = Integer.parseInt(tokens[1]);
            // The xway[4], dir[ 6]
            String c = tokens[2];
            String xd = "0-0";  // Default xway-dir
            if (tokens[4].equals("-1") || tokens[6].equals("-1")) {  // No xway or dir, so we get the xd from a previously seen entry for ths car
                if (carsAndXwayDir.containsKey(c)) {
                    xd = carsAndXwayDir.get(c);
                } else {
                    holding.put(c + "-" + tokens[1], line);  // Since we don't know the xd for this type > 0, just store it until we find one.  And no, you won't get more than 1 type > 0 for any given second.  Type 0 and possibly 2, 3, or 4.
                    continue;
                }
            } else {
                xd = tokens[4] + "-" + tokens[6];
            }
            carsAndXwayDir.put(c, xd);  // we do this so we can give xways to types2 and 3 (and this should be done during datagen/combination.  Sup?
            files.get(xd).printf("%s\n", line);
        }
        for (String k : holding.keySet()) {
            // This block likely never gets called
            files.get(carsAndXwayDir.get(k.split("-")[0])).printf("%s\n", holding.get(k)); // We 're just getting the carid
        }
        for (PrintWriter w : files.values()) {
            w.close();
        }
    }

    public static void main(String[] args) {
        try {
            SplitFiles.splitFiles(args[0], Integer.parseInt(args[1]));
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
