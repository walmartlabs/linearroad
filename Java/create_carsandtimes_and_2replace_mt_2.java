import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Sung Kim on 3/2/2016.
 * Create the carstoreplace after reading the carsandtimes file
 * ********************************************************
 * This only creates the carstoreplace and needs the carsandtimes file from *_1
 * ********************************************************
 * java create_carsandtimes_and_2replace_mt_2 <carsandtimes_file> <carstoreplace_file>
 */
public class create_carsandtimes_and_2replace_mt_2 {

    public static final int NUM_TRIES = 100;  // This is how many attempts we make for each car in carandtimes

    public static void main(String[] args) throws Exception {
        File infile = new File(args[0]);
        File outfile = new File(args[1]);
        HashMap<Integer, HashMap<String, Integer>> cars = new HashMap<>();
        HashSet<Integer> used = new HashSet<>();
        BufferedReader reader = new BufferedReader(new FileReader(infile));
        String line;
        String[] tokens;
        int carid, entertime, exittime, xway;
        int rTime, rCarIndex, rCar;
        PrintWriter writer = new PrintWriter(outfile);
        int setSize;
        ArrayList<HashMap<String,Integer>> keySetToArray = new ArrayList<>();

        // Read in the cars and times (carid [entertime, exittime, xway])
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

        // Do a non-threaded version first
        for (int cid : cars.keySet()) {
            keySetToArray.add(cars.get(cid));
        }
        setSize = cars.keySet().size();

        for (int cid : cars.keySet()) {
            rTime = (int) (Math.random() * 1000) + 61;
            for (int i = 0; i < NUM_TRIES; i++) {
                rCarIndex = (int) (Math.random() * setSize);
                rCar = keySetToArray.get(rCarIndex).get("CarId");
                // TO DO: account for single xway scenario
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
