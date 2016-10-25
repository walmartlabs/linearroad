import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Sung Kim on 3/2/2016.
 *
 * Create the carsandtimes after reading the single, clean, combined file.
 * The carsandtimes is simply each carid with its entry time, its exit time, and its xway.
 * This will be used by create_carstoreplace.java to create cars (by removing others) that re-enter the simulation on
 * a different xway, sometime later in the simulation.
 *
 * This is important as it prevents easy optimization via the assumption that a car's data can be localized to a single
 * xway-dir.
 *
 * We're only choosing cars that will _potentially_ be re-entrant based on a percentage of the total cars.
 * Thus, if the overlap factor is 10, then ~10% of the cars will be placed into the carsandtimes files.
 *
 * *********************************************************
 * This only creates the carsandtimes NOT the carstoreplace.
 * *********************************************************
 * java create_carsandtimes <combined file> <overlap factor: 0 - 100> <carsandtimes_outfile>
 */
public class create_carsandtimes {

    public static void main(String[] args) throws Exception {

        File infile = new File(args[0]);
        int overlap = Integer.parseInt(args[1]);
        File outfile = new File(args[2]);

        // Hold the [carid, ["Enter"|"Exit"|"Xway":value]].
        HashMap<Integer, HashMap<String, Integer>> cars = new HashMap<>();
        // Since we're choosing only a percentage of cars to be candidate cars.
        HashSet<Integer> rejects = new HashSet<>();

        BufferedReader reader = new BufferedReader(new FileReader(infile));
        String line;
        String[] tokens;
        int carid, time, xway;
        int r;
        PrintWriter writer = new PrintWriter(outfile);

        // Read th combined file and build out the cars and times (carid [entertime, exittime, xway]).
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            carid = Integer.parseInt(tokens[2]);
            time = Integer.parseInt(tokens[1]);
            xway = Integer.parseInt(tokens[4]);

            // Don't bother to check the cars that are already rejected.
            if (!rejects.contains(carid)) {
                // Update each exit time with the latest, if the car is not previously rejected.
                if (cars.containsKey(carid)) cars.get(carid).put("Exit", time);
                // A new car, never before seen. Check if this car will be a candiate.
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

        // For all the created candidate cars, print out the carsandtimes file.
        for (int cid : cars.keySet()) {
            writer.println(cid +","+cars.get(cid).get("Enter")+","+cars.get(cid).get("Exit")+","+cars.get(cid).get("Xway"));
        }
        writer.close();
    }
}
