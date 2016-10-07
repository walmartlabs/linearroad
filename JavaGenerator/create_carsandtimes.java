import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Sung Kim on 3/2/2016.
 * Create the carsandtimes after reading the single combined file
 * ********************************************************
 * This only creates the carsandtimes NOT the carstoreplace
 * ********************************************************
 * java create_carsandtimes <combined file> <overlap factor: 0 - 100> <carsandtimes_outfile>
 */
public class create_carsandtimes {
    public static void main(String[] args) throws Exception {
        File infile = new File(args[0]);
        int overlap = Integer.parseInt(args[1]);
        File outfile = new File(args[2]);
        HashMap<Integer, HashMap<String, Integer>> cars = new HashMap<>();
        HashSet<Integer> rejects = new HashSet<>();
        BufferedReader reader = new BufferedReader(new FileReader(infile));
        String line;
        String[] tokens;
        int carid, time, xway;
        int r;
        PrintWriter writer = new PrintWriter(outfile);

        // Built out the cars and times (carid [entertime, exittime, xway])
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            carid = Integer.parseInt(tokens[2]);
            time = Integer.parseInt(tokens[1]);
            xway = Integer.parseInt(tokens[4]);

            if (!rejects.contains(carid)) {
                if (cars.containsKey(carid)) cars.get(carid).put("Exit", time);
                else {
                    r = (int) (Math.random() * 1000) % 100 + 1;
                    if (r < overlap) {
                        cars.put(carid, new HashMap<String, Integer>());
                        cars.get(carid).put("Enter", time);
                        cars.get(carid).put("Exit", time);
                        cars.get(carid).put("Xway", xway);
                    } else rejects.add(carid);
                }
            }
        }

        for (int cid : cars.keySet()) {
            writer.println(cid +","+cars.get(cid).get("Enter")+","+cars.get(cid).get("Exit")+","+cars.get(cid).get("Xway"));
        }
        writer.close();
    }
}
