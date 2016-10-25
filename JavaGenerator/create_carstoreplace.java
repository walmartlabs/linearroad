import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Sung Kim on 3/2/2016.
 * ***************************************************************************
 * create_carsandtimes.java must be fun first to create the carsandtimes file.
 * ***************************************************************************
 * Create the carstoreplace ***after reading the carsandtimes file from create_carsandtimes.java***.
 * java create_carstoreplace <carsandtimes_file> <carstoreplace_file> <numXways>
 */
public class create_carstoreplace {

    public static final int NUM_TRIES = 100;  // This is how many attempts we make for each car in carandtimes.

    public static void main(String[] args) throws Exception {
        File infile = new File(args[0]);
        File outfile = new File(args[1]);
        int numXways = Integer.parseInt(args[2]);

        // Hold carid's, and a HashMap of the corresponding carsandtimes table row.
        HashMap<Integer, HashMap<String, Integer>> cars = new HashMap<>();
        // We don't want to reconsider cars that have already been selected to be replaced.
        HashSet<Integer> used = new HashSet<>();
        // This array simply holds all the HashMaps found in cars (which should probably be named carsandtimes).
        // This allows later for a randomly generated index to pull a random car's information from all the cars.
        ArrayList<HashMap<String,Integer>> keySetToArray = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader(infile));
        PrintWriter writer = new PrintWriter(outfile);

        String line;
        String[] tokens;
        int carid, entertime, exittime, xway;
        int rTime, rCarIndex, rCar;
        int setSize;

        // Read in the carsandtimes file.
        // Key: carid.  Value: a HashMap with (carid, entertime, exittime, xway).
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            carid = Integer.parseInt(tokens[0]);
            entertime = Integer.parseInt(tokens[1]);
            exittime = Integer.parseInt(tokens[2]);
            xway = Integer.parseInt(tokens[3]);

            cars.put(carid, new HashMap<String, Integer>());
            cars.get(carid).put("CarId", carid);
            cars.get(carid).put("Enter", entertime);
            cars.get(carid).put("Exit", exittime);
            cars.get(carid).put("Xway", xway);
        }

        // Do a non-threaded version first.
        for (int cid : cars.keySet()) {
            keySetToArray.add(cars.get(cid));
        }
        setSize = cars.keySet().size();

        // Using the keys, which are the carids, from carsandtimes, for each key:
        // randomly try NUM_TRIES times to find a candidate replacement car from the available carsandtimes.
        for (int cid : cars.keySet()) {
            // Choose a random time distance that the candidate to-be replaced needs to be from the current car.
            rTime = (int) (Math.random() * 1000) + 61;
            // Try to find a candidate.
            for (int i = 0; i < NUM_TRIES; i++) {
                // Get a random index from the cars in carsandtimes.
                rCarIndex = (int) (Math.random() * setSize);
                // Get the car at this index in keySetToArray. (Really the values behind each key in the cars Map.)
                rCar = keySetToArray.get(rCarIndex).get("CarId");
                // Check if we need to take into account separate xways.
                if (numXways > 1) {
                    // If we do need to account for > 1 xway, but the candidate car is in the same xway, try again.
                    if (cars.get(rCar).get("Xway") == cars.get(cid).get("Xway")) continue;
                }
                // If the candidate hasn't been chosen yet,
                // and the Enter time for the candidate meets the time distance criteria from the Exit time of the
                // current car, then choose this candidate to be replaced by the current car; and the current car
                // will now re-enters the simulation on a different time, xway.
                // We also place both the current car and the candidate car into the 'used' Set so neither can be
                // chosen as candidate cars for future current cars.
                if ((!used.contains(rCar)) && rCar != cid && (cars.get(rCar).get("Enter") > (cars.get(cid).get("Exit") + rTime))) {
                    used.add(rCar);
                    used.add(cid);
                    writer.println(cid + "," + rCar);
                    break;
                }
            }
        }
        writer.close();
    }
}
