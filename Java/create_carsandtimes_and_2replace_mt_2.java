import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by cb on 3/2/2016.
 * Create the carstoreplace after reading the carsandtimes file
 * ********************************************************
 * This only creates the carstoreplace AND NEEDS the carsandtimes file from *_1
 * ********************************************************
 * java create_carsandtimes_and_2replace_mt_2 <carsandtimes_file> <overlap factor: 0 - 100> <carstoreplace_file>
 */
public class create_carsandtimes_and_2replace_mt_2 {

    public static final int NUM_TRIES = 100;  // This is how many attempts we make for each car in carandtimes

    public static void main(String[] args) throws Exception {
        File infile = new File(args[0]);
        int overlap = Integer.parseInt(args[1]);
        File outfile = new File(args[2]);
        // Start with a HashMap then consider moving to an array (although it would be HUUUUUGE)
        HashMap<Integer, HashMap<String, Integer>> cars = new HashMap<>();
        HashSet<Integer> rejects = new HashSet<>();
        HashSet<Integer> used = new HashSet<>();
        HashSet<Integer> will_double = new HashSet<>();
        int maxCarId = 0;  // We look for it here because we can, but if we make sure to record it after combining the files we could feasibly use an array instead of the maps and sets
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
        //System.out.println(keySetToArray.size());
        //System.out.println(keySetToArray.get(0));

        for (int cid : cars.keySet()) {
            //System.out.println("cid: " + cid);
            rTime = (int) (Math.random() * 1000) + 61;
            //System.out.println("rTime: " + rTime);
            for (int i = 0; i < NUM_TRIES; i++) {
                rCarIndex = (int) (Math.random() * setSize);
                //System.out.println("rCarIndex: " + rCarIndex);
                rCar = keySetToArray.get(rCarIndex).get("CarId");
                //System.out.println("rCar: " + rCar);
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
