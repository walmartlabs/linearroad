import java.io.*;
import java.util.*;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by Sung Kim on 3/8/2016.
 * Multiple Threads
 * Generate the output to verify others' output
 * Usage: time java ValidateMT <datafile> <num XWays> <tollfile> <outputfile>
 */
public class ValidateMT extends Thread {
    public static HashMap<Integer, ArrayList<Integer>> tolls;
    public static HashMap<String, Integer> historical;

    static {
        tolls = new HashMap<>();
        historical = new HashMap<>();
    }

    private File file;
    private BufferedReader reader;
    private PrintWriter writer;
    private int xway;
    private int dir;
    private Car currentCar;
    private HashMap<Integer, Car> cars;  // K, carId: Int ; V, Car(lastTime, lastSpeed, lastXway, lastLane, lastDir, lastSeg, lastPos, xPos, lastToll)
    private HashMap<String, Integer> segSumSpeeds;  // K, seg: Int, min: Int ; V, sumOfSpeeds: Int
    private HashMap<String, Integer> segSumNumReadings;  // K, seg: Int, min: Int ; V, sumNumSpeedReadings: Int
    private HashMap<String, Set<Integer>> segCarIdSet;  // K, seg: Int, min: Int ; V, Set(carId: Int)
    private HashMap<String, List<Integer>> stopped;  // K, lane: Int, seg: Int, pos: Int ; V, Set(carId: Int)
    private HashMap<Integer, Accident> accidents;  // K, seg: Int ; V, Accident(time, clearTime, carId1, carId2)
    int type0Seen;
    int type2Seen;
    int type3Seen;
    int type0Processed;
    int type1Processed;
    int type2Processed;
    int type3Processed;

    public ValidateMT(File f, int xway, int dir) {
        this.xway = xway;
        this.dir = dir;
        cars = new HashMap<>();
        segSumSpeeds = new HashMap<>();
        segSumNumReadings = new HashMap<>();
        segCarIdSet = new HashMap<>();
        stopped = new HashMap<>();
        accidents = new HashMap<>();
        try {
            reader = new BufferedReader(new FileReader(f));
            writer = new PrintWriter(f.toString() + "-out");
        } catch (FileNotFoundException e) {
            System.err.println(e);
            System.exit(1);
        }
        type0Seen = 0;
        type2Seen = 0;
        type3Seen = 0;
        type0Processed = 0;
        type1Processed = 0;
        type2Processed = 0;
        type3Processed = 0;
    }


    private class Car {
        int carId;
        int lastTime;
        int lastSpeed;
        int lastXway;
        int lastLane;
        int lastDir;
        int lastSeg;
        int lastPos;
        int xPos;
        int lastToll;

        Car(int carId) {
            this.carId = carId;
            lastTime = lastSpeed = lastXway = lastLane = lastDir = lastSeg = lastPos = -1;
            xPos = lastToll = 0;
        }

        void reset() {
            lastTime = lastSpeed = lastXway = lastLane = lastDir = lastSeg = lastPos = -1;
            xPos = lastToll = 0;
        }
    }

    private class Accident {
        int time;
        int clearTime;
        List<Integer> accidentCars;

        Accident(int time, int carId1, int carId2) {
            this.time = time;
            this.clearTime = -1;
            accidentCars = new ArrayList<Integer>();
            accidentCars.add(carId1);
            accidentCars.add(carId2);
        }
    }

    @Override
    public void run() {
        System.out.println("Thread " + this.getName() + " starting");
        String line;
        Map<String, Integer> mt;
        try {
            while ((line = reader.readLine()) != null) {
                mt = createMT(line.split(","));
                int type = mt.get("type");
                switch (type) {
                    case 0:
                        type0Seen++;
                        t0(mt);
                        break;
                    case 2:
                        type2Seen++;
                        t2(mt);
                        break;
                    case 3:
                        type3Seen++;
                        t3(mt);
                        break;
                }
            }
            reader.close();
        } catch (IOException e) {
            System.err.println(e);
            System.exit(1);
        }
        writer.close();
    }

    private Map<String, Integer> createMT(String[] tokens) {
        Map<String, Integer> m = new HashMap<>();
        m.put("type", Integer.parseInt(tokens[0]));
        m.put("time", Integer.parseInt(tokens[1]));
        m.put("carId", Integer.parseInt(tokens[2]));
        m.put("speed", Integer.parseInt(tokens[3]));
        m.put("xway", Integer.parseInt(tokens[4]));
        m.put("lane", Integer.parseInt(tokens[5]));
        m.put("dir", Integer.parseInt(tokens[6]));
        m.put("seg", Integer.parseInt(tokens[7]));
        m.put("pos", Integer.parseInt(tokens[8]));
        m.put("qid", Integer.parseInt(tokens[9]));
        m.put("day", Integer.parseInt(tokens[14]));
        return m;
    }

    private String getOrCreateSeg(Map<String, Integer> mt) {
        String segKey = mt.get("seg") + "-" + (mt.get("time") / 60 + 1);
        // Create a new record for a particular seg+min key for this xway+dir if it doesn't exist
        if (!segSumSpeeds.containsKey(segKey) && !segSumNumReadings.containsKey(segKey) && !segCarIdSet.containsKey(segKey)) {
            segSumSpeeds.put(segKey, mt.get("speed"));
            segSumNumReadings.put(segKey, 1);
            Set<Integer> newCarIdSet = new HashSet<>();
            newCarIdSet.add(mt.get("carId"));
            segCarIdSet.put(segKey, newCarIdSet);
        }
        return segKey;
    }

    private Car getOrCreateCar(Map<String, Integer> mt) {
        Car car;
        if (!cars.containsKey(mt.get("carId"))) {
            car = new Car(mt.get("carId"));
            cars.put(mt.get("carId"), car);
        } else {
            car = cars.get(mt.get("carId"));
            if (mt.get("lane") == 0 && mt.get("time") > (car.lastTime + 60)) {  // Check if the currentCar is a re-entrant car.  If it is reset its values.
                car.reset();
            }
        }
        return car;
    }

    private boolean createStoppedCar(String stoppedKey, Car c) {  // Return true if a new stopped car was added to 'this.stopped'
        if (!stopped.containsKey(stoppedKey)) {
            List<Integer> s = new ArrayList<>();
            s.add(c.carId);
            stopped.put(stoppedKey, s);
            return true;
        } else {
            if (stopped.get(stoppedKey).size() < 2 && !stopped.get(stoppedKey).contains(c.carId)) {  // Do we allow more than two cars at any stopped position?  Not for now.
                stopped.get(stoppedKey).add(c.carId);
                //System.out.println(stopped.get(stoppedKey));
                return true;
            }
        }
        return false;
    }

    private void createAccident(String stoppedKey, int seg, int time) {
        if (stopped.get(stoppedKey).size() == 2 && !accidents.containsKey(seg)) {
            Accident newAccident = new Accident(time, stopped.get(stoppedKey).get(0), stopped.get(stoppedKey).get(1));
            accidents.put(seg, newAccident);
            System.out.printf("%d, %d,%d,%d,%d\n", seg, newAccident.time, newAccident.clearTime, newAccident.accidentCars.get(0), newAccident.accidentCars.get(1));
        }
    }

    private int getNumV(String lastMinKey) {
        if (segCarIdSet.containsKey(lastMinKey)) {
            return segCarIdSet.get(lastMinKey).size();
        }
        return 0;
    }

    private int calcToll(int numv) {
        return (int) (2 * Math.pow(50 - numv, 2));
    }

    private int getLav(int seg, int min) {
        int totalSpeed = 0, totalSpeedReadings = 0;
        String lavKey;  // The last average velocity
        for (int i = 1; i < 6; i++) {
            lavKey = seg + "-" + (min - i);
            if (segSumSpeeds.containsKey(lavKey)) totalSpeed += segSumSpeeds.get(lavKey);
            if (segSumNumReadings.containsKey(lavKey)) totalSpeedReadings += segSumNumReadings.get(lavKey);
        }
        if (totalSpeedReadings > 0) return (int) Math.round(totalSpeed / (float) totalSpeedReadings);
        else return 0;
    }

    private int inAccidentZone(int seg, int min) {
        int k;
        Accident accident;
        for (int i = 0; i < 5; i++) {
            if (dir == 0) {
                k = seg + i;
            } else {
                k = seg - i;
            }
            if (accidents.containsKey(k)) {
                accident = accidents.get(k);
                int accNotiThresholdMin = accident.time / 60 + 2;
                int accClearMin = accident.clearTime / 60 + 1;
                if (accident.clearTime != -1 && accNotiThresholdMin > accClearMin) continue;
                if ((min >= accNotiThresholdMin && accident.clearTime == -1) ||
                        (min <= accClearMin && accident.clearTime != -1)) {
                    return k;
                }
            }
        }
        return -1;
    }

    private void assessToll(Car c, int time) {
        if (tolls.containsKey(c.carId)) {
            tolls.get(c.carId).add(time);  // The time
            tolls.get(c.carId).add(c.lastToll);  // The carId
        } else {
            ArrayList<Integer> newTollList = new ArrayList<>();
            newTollList.add(time);  // The time
            newTollList.add(c.lastToll);  // The carId
            tolls.put(c.carId, newTollList);
        }
    }

    public void t0(Map<String, Integer> mt) {
        long startTime = System.currentTimeMillis();
        int min = mt.get("time") / 60 + 1;
        String stoppedKey = mt.get("lane") + "-" + mt.get("seg") + "-" + mt.get("pos");
        String segKey = getOrCreateSeg(mt);  // Simply create a new seg-min combination if it doesn't exist
        currentCar = getOrCreateCar(mt);  // Create or fetch a car
        if ((currentCar.lastLane == 4) && (mt.get("lane")) != 0)
            return;  // Check this is an anomalous car, i.e. lastLane == 4 but it shows up again with a 'lane' != 0 and ignore
        /* SAME POSITION? */
        if (currentCar.lastPos == mt.get("pos") && currentCar.lastLane == mt.get("lane")) { // This thread only operates on a single xway-dir // && currentCar.lastXway == mt.get("xway") && currentCar.lastDir == mt.get("dir"))
            if (currentCar.xPos == 3) {  // Already seen three times at this pos+lane, so create a STOPPED car
                if (createStoppedCar(stoppedKey, currentCar)) {
                    createAccident(stoppedKey, mt.get("seg"), mt.get("time"));
                }
            }
            currentCar.xPos++; // Update currentCar's xPos  
        /* NEW POSITION */
        } else {
            String prevStoppedKey = currentCar.lastLane + "-" + currentCar.lastSeg + "-" + currentCar.lastPos;
            if (stopped.containsKey(prevStoppedKey)) { // Remove this carId from stopped if it's there
                stopped.get(prevStoppedKey).remove(mt.get("carId"));
            }
            if (accidents.containsKey(currentCar.lastSeg) && accidents.get(currentCar.lastSeg).accidentCars.contains(currentCar.carId) && accidents.get(currentCar.lastSeg).clearTime == -1) {  // Clear accident involving this car if any
                accidents.get(currentCar.lastSeg).clearTime = mt.get("time");
                Accident oldAccident = accidents.get(currentCar.lastSeg);
                System.out.printf("%d, %d,%d,%d,%d\n", currentCar.lastSeg, oldAccident.time, oldAccident.clearTime, oldAccident.accidentCars.get(0), oldAccident.accidentCars.get(1));
            }
            currentCar.xPos = 1;  // Reset current car's number of times at this position
            /* NEW POSITION BUT SAME SEGMENT */
            if (mt.get("seg") == currentCar.lastSeg) {  // I don't know if we really need to do anything here.  I guess a car could move to an exit lane.
                if (mt.get("lane") == 4) {
                    currentCar.lastLane = 4;
                }
            /* NEW POSITION NEW SEGMENT */
            } else {
                int currToll = 0;
                int numv = 0;
                int lav = 0;
                if (mt.get("lane") != 4) {
                    /* NUMV */
                    String lastMinKey = mt.get("seg") + "-" + (min - 1);
                    numv = getNumV(lastMinKey);
                    if (numv > 50) currToll = calcToll(numv);
                    /* LAV */
                    lav = getLav(mt.get("seg"), min);
                    if (lav >= 40) currToll = 0;
                    /* ACCIDENTS */
                    int accSeg = inAccidentZone(mt.get("seg"), min);
                    if (accSeg >= 0) {
                        currToll = 0;
                        writer.printf("1,%d,%d,%d,%d,%d,%d\n", mt.get("time"), (System.currentTimeMillis() - startTime), this.xway, accSeg, this.dir, currentCar.carId);
                        type1Processed += 1;
                    }
                    writer.printf("0,%d,%d,%d,%d,%d\n", mt.get("carId"), mt.get("time"), (System.currentTimeMillis() - startTime), lav, currToll);
                    type0Processed += 1;
                }
                //System.out.printf("%d,%d,%d\n", numv, lav, currToll);
                /* PREVIOUS TOLL */
                if (currentCar.lastToll > 0) {
                    assessToll(currentCar, mt.get("time"));
                }
                currentCar.lastToll = currToll;  // New segment yields new toll
            }
        }
        // Update car and segment info.  Car info should already be partially updated.
        currentCar.lastDir = mt.get("dir"); // Not necessary, BUT wasn't there something funky with the data where a car would jump directions or lanes?
        currentCar.lastLane = mt.get("lane");
        currentCar.lastPos = mt.get("pos");
        currentCar.lastSeg = mt.get("seg");
        currentCar.lastSpeed = mt.get("speed");
        currentCar.lastTime = mt.get("time");
        // currentCar.lastToll  // Updated above as needed
        currentCar.lastXway = mt.get("xway");  // Not necessary
        // currentCar.xPos  // Updated above as needed
        segSumSpeeds.put(segKey, segSumSpeeds.get(segKey) + mt.get("speed"));
        segSumNumReadings.put(segKey, segSumNumReadings.get(segKey) + 1);
        segCarIdSet.get(segKey).add(mt.get("carId"));

    }

    public void t2(Map<String, Integer> mt) {
        // A type 2 could feasibly yield at least two (original says three) numbers
        long startTime = System.currentTimeMillis();
        int bal0 = 0, bal1 = 0, rt0 = mt.get("time"), rt1 = 0;
        ArrayList<Integer> charges;
        if (tolls.containsKey(mt.get("carId"))) {
            charges = tolls.get(mt.get("carId"));
            //System.out.println(charges.size());
            for (int i = 1; i < charges.size(); i += 2) {
                bal0 += charges.get(i);
            }
            if (charges.size() > 2) {
                bal1 = bal0 - charges.get(charges.size() - 1);
                rt1 = rt0 - 30; // mt.get("time") - charges.get(charges.size() - 2);  // Or simply -30
            }
        }
        // We either need to convert millis since epoch to something else or simply give the millis till completion as we do here
        writer.printf("2,%d,%d,%d,%d,%d\n", mt.get("time"), System.currentTimeMillis() - startTime, rt0, mt.get("qid"), bal0);
        writer.printf("5,%d,%d,%d,%d,%d\n", mt.get("time"), System.currentTimeMillis() - startTime, rt1, mt.get("qid"), bal1);
        type2Processed++;
    }

    public void t3(Map<String, Integer> mt) {
        long startTime = System.currentTimeMillis();
        String k = mt.get("carId") + "-" + mt.get("day") + "-" + mt.get("xway");
        int toll = 0;
        if (historical.containsKey(k) && mt.get("day") != 0) {
           toll = historical.get(k);
            writer.printf("3,%d,%d,%d,%d\n", mt.get("time"), System.currentTimeMillis() - startTime, mt.get("qid"), toll);
            type3Processed++;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: java ValidateMT <main data file> <num xways> <toll file>");
            System.exit(1);
        }

        int totalType0Seen = 0;
        int totalType2Seen = 0;
        int totalType3Seen = 0;
        int totalType0Processed = 0;
        int totalType1Processed = 0;
        int totalType2Processed = 0;
        int totalType3Processed = 0;

        BufferedReader reader;
        String line;
        String[] tokens;

        String inputFile = args[0];  // Split the file separately
        int numXWays = Integer.parseInt(args[1]);
        String tollFileName = args[2];  // Never use the full size file but use the matching file created by CreateMatchingTolls

        // Split a single input file into xway xway-dir files
        SplitFiles.splitFiles(inputFile, numXWays);

        // Create matching toll file
        String matchingTolls = "matchTollsOnly.dat";
        CreateMatchingTolls.createMatchingTollsFile(inputFile, tollFileName, matchingTolls);
        // Load historical toll file
        reader = new BufferedReader(new FileReader(new File(matchingTolls)));
        String key;
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            // [0]carId-[1]day-[2]xway  :  [3]value
            key = tokens[0] + "-" + tokens[1] + "-" + tokens[2];
            historical.put(key, Integer.parseInt(tokens[3]));
        }
        System.out.println("Finished loading historical files...");

        // The following section assumes separated file creation by using SplitJava
        List<String> files = new ArrayList<>();
        //String numXWays = 1 ; //hinputFile.substring(inputFile.indexOf("h")+1, inputFile.indexOf("x"));
        int iNumFiles = numXWays;
        for (int i = 0 ; i < iNumFiles ; i++) {
            files.add(i + "-0");
            files.add(i + "-1");
        }

        //String[] files = args;
        ArrayList<ValidateMT> threads = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            System.out.println("Do I get here?");
            tokens = files.get(i).split("-");
            ValidateMT v = new ValidateMT(new File(files.get(i)), Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
            v.start();
            threads.add(v);
        }
        for (ValidateMT vt : threads) {
            vt.join();
            // Does this mean we only halt on thread-1 and wait for the rest?  I.e. it works because although we wait on 1, if that finishes early we can wait on any remaining.
        }
        // We should recombine files here so we don't have to do it in a shell

        for (ValidateMT vt : threads) {
            totalType0Seen += vt.type0Seen;
            totalType2Seen += vt.type2Seen;
            totalType3Seen += vt.type3Seen;
            totalType0Processed += vt.type0Processed;
            totalType1Processed += vt.type1Processed;
            totalType2Processed += vt.type2Processed;
            totalType3Processed += vt.type3Processed;
        }
        System.out.printf("Total type 0 seen:\t\t%d\n", totalType0Seen);
        System.out.printf("Total type 2 seen:\t\t%d\n", totalType2Seen);
        System.out.printf("Total type 3 seen:\t\t%d\n", totalType3Seen);
        System.out.printf("Total type 0 processed:\t\t%d\n", totalType0Processed);
        System.out.printf("Total type 1 processed:\t\t%d\n", totalType1Processed);
        System.out.printf("Total type 2 processed:\t\t%d\n", totalType2Processed);
        System.out.printf("Total type 3 processed:\t\t%d\n", totalType3Processed);
    }
}

