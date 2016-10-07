import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Sung Kim on 3/8/2016.
 * Use multiple threads.
 * Use a BlockingQueue to handle issues with some threads getting ahead of others.
 * Using the BlockingQueue means longer needing to create split files.
 * Process by the 'second's, and send when all have finished each second.
 * Trim the segment maps to only hold the most current minute and the past 5 simulation minutes.
 * Use Aerospike to hold all tolls.
 * Usage: time java ValidateMTBQEven3AeroTolls <datafile> <num XWays> <tollfile>
 * FYI:
 * MT (MultiThreaded)
 * BQ (BlockingQueue)
 * Even (every thread finished processing the current second before moving on to the next second)
 * 3 (version)
 * AeroTolls (use Aerospike to hold the global toll information)
 * Non-Aerospike, or "other" database versions can be created by simply replacing the Aerospike portions with
 * a different database of choice. A Java Map can also be used but this limits the number of expressways for which a
 * validation file can be created.
 */
public class ValidateMTBQEven3AeroTolls extends Thread {
    public static HashMap<String, Integer> historical;

    AerospikeClient client;
    WritePolicy policy;

    static {
        historical = new HashMap<>();
    }

    private LinkedBlockingDeque<String> q;
    private LRReader r;
    private boolean paused;
    private PrintWriter writer;
    private PrintWriter writerDEBUG;
    private long tollAssessmentCountDEBUG;
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
    int type0SeenDEBUG;
    int type2Seen;
    int type3Seen;
    int type0Processed;
    int type1Processed;
    int type2Processed;
    int type3Processed;

    public static class LRReader extends Thread {
        BufferedReader reader;
        Map<String, ValidateMTBQEven3AeroTolls> lrs;
        int numNotifications;
        boolean done;
        // DEBUG
        int LRRestartsDEBUG;

        LRReader(BufferedReader reader, Map<String, ValidateMTBQEven3AeroTolls> lrs) {
            this.reader = reader;
            this.lrs = lrs;
            numNotifications = 0;
            done = false;
            LRRestartsDEBUG = 0;
        }

        /**
         * ValidateMTBQEven3... thread will notify LRReader that it is done with its set of records.
         * When the number of notifications equals the number of threads, LRReader will restart.
         */
        public void notifyDone() {
            synchronized (this) {
                numNotifications++;
                if (numNotifications == lrs.size()) {
                    restart();
                    LRRestartsDEBUG++;
                }
            }
        }

        private void restart() {
            synchronized (this) {
                notify();
                numNotifications = 0;
            }
        }

        /**
         * This needs to somehow run periodically because if the processing actually finishes before the reader is done
         * then it gets stuck.
         */
        public void checkRestart() {
            for (ValidateMTBQEven3AeroTolls rt : lrs.values()) {
                if (!rt.isPaused()) {
                    return;
                }
            }
            restart(); // We should only reach this if all processing threads are paused.
        }

        public boolean isDone() {
            return done;
        }

        @Override
        public void run() {
            String line;
            String[] tokens;
            int counter = 0;
            int currTime = 0;
            int lineTime = 0;
            Map<Integer, String> lastSeen = new HashMap<>(); // To handle type 2 and 3 without xway and/or dir
            List<String> twoThree = new ArrayList<>();
            String xwayDir;// = "0-0";
            int carId;// = 0;
            try {
                while ((line = reader.readLine()) != null) {
                    tokens = line.split(",");
                    lineTime = Integer.parseInt(tokens[1]);
                    carId = Integer.parseInt(tokens[2]);
                    if (currTime != lineTime) {
                        //System.out.println(currTime + " to " + lineTime);
                        synchronized (this) {
                            //System.out.println("Pausing reader");
                            wait();
                            //System.out.println("Re-starting reader");
                            for (String l : twoThree) {
                                String[] tokens2 = l.split(",");
                                xwayDir = lastSeen.get(Integer.parseInt(tokens2[2]));
                                lrs.get(xwayDir).addToQ(l);
                            }//
                            twoThree.clear();
                        }
                    }
                    //System.out.println(line);
                    if (tokens[0].equals("0")) {
                        xwayDir = tokens[4] + "-" + tokens[6];
                        lastSeen.put(carId, xwayDir);
                        lrs.get(xwayDir).addToQ(line);
                    }
                    if (!tokens[0].equals("0")) {
                        twoThree.add(line);
                        //System.out.println(twoThree.size());
                    }
                    currTime = lineTime;
                }

                // Clean up any 2/3's remaining
                for (String l : twoThree) {
                    tokens = l.split(",");
                    carId = Integer.parseInt(tokens[2]);
                    xwayDir = lastSeen.get(carId);
                    //System.out.println("Cleanup 2,3's: " + xwayDir);
                    lrs.get(xwayDir).addToQ(l);
                }
                twoThree.clear();
            } catch (InterruptedException e) {
                System.err.println(e);
            } catch (IOException e) {
                System.err.println(e);
            }
            done = true;
            for (ValidateMTBQEven3AeroTolls rt : lrs.values()) {
                rt.addToQ("quit");
            }
        }
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

    public ValidateMTBQEven3AeroTolls(String f, int xway, int dir, LRReader r) {
        client = new AerospikeClient("127.0.0.1", 3000);
        policy = new WritePolicy();
        policy.timeout = 100;

        q = new LinkedBlockingDeque<>();
        this.r = r;
        paused = false;
        this.xway = xway;
        this.dir = dir;
        cars = new HashMap<>();
        segSumSpeeds = new HashMap<>();
        segSumNumReadings = new HashMap<>();
        segCarIdSet = new HashMap<>();
        stopped = new HashMap<>();
        accidents = new HashMap<>();
        try {
            //reader = new BufferedReader(new FileReader(f));
            writer = new PrintWriter(f + "-out");
            writerDEBUG = new PrintWriter(f + "-out-DEBUG");
        } catch (FileNotFoundException e) {
            System.err.println(e);
            System.exit(1);
        }
        type0Seen = 0;
        type0SeenDEBUG = 0;
        tollAssessmentCountDEBUG = 0;
        type2Seen = 0;
        type3Seen = 0;
        type0Processed = 0;
        type1Processed = 0;
        type2Processed = 0;
        type3Processed = 0;
    }

    // Add an element to a given thread's queue for processing.
    public void addToQ(String line) {
        try {
            q.put(line);
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    public void run() {
        System.out.println("Thread " + this.getName() + " starting");
        String line;
        Map<String, Integer> mt;

        try {
            while (true) {
                if (q.peek() == null) {
                    paused = true;
                    r.notifyDone();
                }
                line = q.take();
                paused = false;
                if (line.equals("quit")) {
                    writer.flush();
                    writerDEBUG.flush();
                    break; // This is the 'poison pill' to kill the thread
                }
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
        } catch (InterruptedException e) {
            System.err.println(e);
        } finally {
            writer.close();
            writerDEBUG.close();
        }
    }

    /**
     * Create a Map of the input line
     *
     * @param tokens The tokenized line.
     * @return A Map<String, Integer> of the input line, split into its constituent parts.
     */
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

    /**
     * Each xway and dir has its own Maps of segment key with speeds, number of speed readings, and a Set of carid's in that segment key.
     * The segment key is: (The segment number) + (The simulation time, the minute).
     *
     * @param mt The Map-ized input line
     * @return The segment key
     */
    private String getOrCreateSeg(Map<String, Integer> mt) {
        String segKey = mt.get("seg") + "-" + (mt.get("time") / 60 + 1);  // Oh, duh, of COURSE you need the parens
        // Create a new record for a particular seg+min key for this xway+dir if it doesn't exist
        if (!segSumSpeeds.containsKey(segKey) && !segSumNumReadings.containsKey(segKey) && !segCarIdSet.containsKey(segKey)) {
            //segSumSpeeds.put(segKey, mt.get("speed"));
            segSumSpeeds.put(segKey, 0);
            //segSumNumReadings.put(segKey, 1);
            segSumNumReadings.put(segKey, 0);
            Set<Integer> newCarIdSet = new HashSet<>();
            newCarIdSet.add(mt.get("carId"));
            segCarIdSet.put(segKey, newCarIdSet);
        }
        return segKey;
    }

    /**
     * Each xway and dir has its own Map of cars.
     * The car key is: (The carid).
     *
     * @param mt The Map-ized input line.
     * @return The Car (Object) whether newly created for this xway and dir, or the currently existing one.
     */
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

    /**
     * Each xway and dir has its own Map of stopped cars.
     * The stopped key is: (The Lane) + (The Seg) + (The Position)
     * Take a "Stopped Key" and a Car (Object) and potentially include the Car into the "stopped" Map.
     *
     * @param stoppedKey A String (lane+"-"+seg+"-"+pos).
     * @param c          The Car.
     * @return Whether a car was inserted into this stopped Map at the stoppedKey.  The current rule is that only two cars (enough to track an accident) will be tracked at a given stoppedKey.
     */
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

    /**
     * Each xway and dir has its own Map of accidents.
     * The accidents key is: (The Seg)
     * The seg is the only element of the key as accident notifications are sent based on the segment.
     * Of course the xway and dir would be part of the key if this were single-threaded.
     * This creates an accident if one doesn't already exist.
     * The print statement is for debugging when accidents occur.
     *
     * @param stoppedKey The stoppedKey of the segment in question.
     * @param seg        The segment.
     * @param time       The simulation time (which is Floor(time/60) + 1)
     */
    private void createAccident(String stoppedKey, int seg, int time) {
        if (stopped.get(stoppedKey).size() == 2 && !accidents.containsKey(seg)) {
            Accident newAccident = new Accident(time, stopped.get(stoppedKey).get(0), stopped.get(stoppedKey).get(1));
            accidents.put(seg, newAccident);
            System.out.printf("%d,%d,%d,%d,%d\n", seg, newAccident.time, newAccident.clearTime, newAccident.accidentCars.get(0), newAccident.accidentCars.get(1));
        }
    }

    /**
     * For potential toll calculation purposes get the number of cars seen in a segment in the last minute.
     * Note, this is means a car is not double-counted if it doesn't make it out of a segment within the 30 seconds of the next notification.
     * The key is a segment key, which is the segment number and a simulation minute.
     *
     * @param lastMinKey The seg + the previous min.
     * @return The number of vehicles.
     */
    private int getNumV(String lastMinKey) {
        if (segCarIdSet.containsKey(lastMinKey)) {
            return segCarIdSet.get(lastMinKey).size();
        }
        return 0;
    }

    /**
     * A simple toll calculation.
     *
     * @param numv
     * @return
     */
    private int calcToll(int numv) {
        return (int) (2 * Math.pow(50 - numv, 2));
    }

    /**
     * Take the segment and minute (last) and find that last average velocity using that seg-min's speed readings.
     *
     * @param seg
     * @param min
     * @return
     */
    private int getLav(int seg, int min) {
        int totalSpeed = 0, totalSpeedReadings = 0;
        String lavKey;  // The last average velocity
        for (int i = 1; i < 6; i++) {
            lavKey = seg + "-" + (min - i);
            if (segSumSpeeds.containsKey(lavKey)) totalSpeed += segSumSpeeds.get(lavKey);
            if (segSumNumReadings.containsKey(lavKey)) totalSpeedReadings += segSumNumReadings.get(lavKey);
        }
        //if (totalSpeedReadings > 0) writer.printf("lav: %d %f\n", totalSpeed, ((float) totalSpeedReadings));
        if (totalSpeedReadings > 0) return Math.round(totalSpeed / ((float) totalSpeedReadings));
        else return 0;
    }

    /**
     * As cars travel from one segment to another see if they are in accident zones
     *
     * @param seg
     * @param min
     * @return
     */
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

    /**
     * Add a toll to the global tolls Map. This, along with, the historical Map, are the only two global data stores/structures. Thus, these two are the ones that can be placed into an external store as well. However, this will only solve one of the scaling issues if we keep all other Maps in memory.  Each xway-dir thread will still require memory to hold all of its individual data, including many Maps.
     *
     * @param c
     * @param time
     */
    private void assessToll(Car c, int time) {
        Key key = new Key("test", "myset", c.carId);
        Record record = client.get(policy, key);
        List<Integer> tollList = null;
        if (record != null) {
            tollList = (List<Integer>) record.getList("tolls");
        } else {
            tollList = new ArrayList<Integer>();
        }
        //System.out.printf("Tolls for %d: %d\n", c.carId, tollList.size());
        tollList.add(time);  // The time
        tollList.add(c.lastToll);  // The last toll
        Bin bin = new Bin("tolls", tollList);
        client.put(policy, key, bin);
    }

    /**
     * Process a type 0 input line
     *
     * @param mt
     */
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
            currentCar.xPos++; // Update currentCar's xPos  // Is this a reference to the object in the Hashmap?  I think so ...
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
                        writer.printf("1,%d,%d,%d,%d,%d,%d\n", mt.get("time"), mt.get("time") + (System.currentTimeMillis() - startTime), this.xway, accSeg, this.dir, currentCar.carId);
                        type1Processed += 1;
                    }
                    writer.printf("0,%d,%d,%d,%d,%d\n", mt.get("carId"), mt.get("time"), mt.get("time") + (System.currentTimeMillis() - startTime), lav, currToll);
                    type0Processed += 1;
                }
                //System.out.printf("%d,%d,%d\n", numv, lav, currToll);
                /* PREVIOUS TOLL */
                if (currentCar.lastToll > 0) {
                    assessToll(currentCar, mt.get("time"));
                    tollAssessmentCountDEBUG++;
                    writerDEBUG.printf("assessToll,%d,%d,%d,%d\n", mt.get("time"), mt.get("carId"), tollAssessmentCountDEBUG, currentCar.lastToll);
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

        // Clean up segments > 6 minutes away from the current minute
            // We do this before the alteration of the segment hashes
        //String segKey = mt.get("seg") + "-" + (mt.get("time") / 60 + 1); // This is here simply as a reference to remember what constitutes the segKey.
        int removeMin = min - 6;
        segSumSpeeds.remove(mt.get("seg") + "-" + removeMin);
        segSumNumReadings.remove(mt.get("seg") + "-" + removeMin);
        segCarIdSet.remove(mt.get("seg") + "-" + removeMin);

        segSumSpeeds.put(segKey, segSumSpeeds.get(segKey) + mt.get("speed"));
        segSumNumReadings.put(segKey, segSumNumReadings.get(segKey) + 1);
        segCarIdSet.get(segKey).add(mt.get("carId"));
        //type0SeenDEBUG++;
        //writerDEBUG.println(type0SeenDEBUG + ":" + segKey + ":" + segSumSpeeds.get(segKey) + "," + segSumNumReadings.get(segKey));


    }

    /**
     * Process a type 2 intra-day account balance query line
     *
     * @param mt
     */
    public void t2(Map<String, Integer> mt) {
        // A type 2 could feasibly yield at least two (original says three) numbers
        long startTime = System.currentTimeMillis();
        long bal0 = 0, bal1 = 0, rt0 = mt.get("time"), rt1 = 0;
        List<Long> charges = null;
        Key key = new Key("test", "myset", mt.get("carId"));
        Record record = client.get(policy, key);
        if (record != null) {
            charges = (List<Long>)record.getList("tolls");
            for (int i = 1; i < charges.size(); i += 2) {
                long t = charges.get(i);
                bal0 += t;
            }
            if (charges.size() > 2) {
                bal1 = bal0 - charges.get(charges.size() - 1);
                rt1 = rt0 - 30;
            }
        }
        // We either need to convert millis since epoch to something else or simply give the millis till completion as we do here
        writer.printf("2,%d,%d,%d,%d,%d\n", mt.get("time"), mt.get("time") + System.currentTimeMillis() - startTime, rt0, mt.get("qid"), bal0);
        writer.printf("5,%d,%d,%d,%d,%d\n", mt.get("time"), mt.get("time") + System.currentTimeMillis() - startTime, rt1, mt.get("qid"), bal1);
        writerDEBUG.printf("%d:", mt.get("carId"));
        if (record != null && charges != null) {
            for (long i : charges) {
                writerDEBUG.printf("%d,", i);
            }
        }
        writerDEBUG.printf("\n");
        type2Processed++;
    }

    /**
     * Process a type 3 historical query line
     *
     * @param mt
     */
    public void t3(Map<String, Integer> mt) {
        long startTime = System.currentTimeMillis();
        String k = mt.get("carId") + "-" + mt.get("day") + "-" + mt.get("xway");
        int toll = 0;
        synchronized (historical) {
            if (historical.containsKey(k) && mt.get("day") != 0) {
                toll = historical.get(k);
                writer.printf("3,%d,%d,%d,%d\n", mt.get("time"), mt.get("time") + System.currentTimeMillis() - startTime, mt.get("qid"), toll);
                type3Processed++;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: java ValidateMTBQEven... <main data file> <num xways> <toll file>");
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

        String inputFile = args[0];  //
        int numXWays = Integer.parseInt(args[1]);
        String tollFileName = args[2];  // Never use the full size file but use the matching file created by CreateMatchingTolls

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
        reader.close();
        System.out.println("Finished loading historical files...");

        //
        List<String> files = new ArrayList<>();
        int iNumFiles = numXWays;
        for (int i = 0; i < iNumFiles; i++) {
            files.add(i + "-0");
            files.add(i + "-1");
        }

        Map<String, ValidateMTBQEven3AeroTolls> threads = new HashMap<>();
        LRReader lrReader = new LRReader(new BufferedReader(new FileReader(inputFile)), threads);
        // Create and initialize the threads, one for each xway-dir
        for (int i = 0; i < files.size(); i++) {
            System.out.println("Do I get here?");
            tokens = files.get(i).split("-");
            ValidateMTBQEven3AeroTolls v = new ValidateMTBQEven3AeroTolls(files.get(i), Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), lrReader);
            v.start();
            threads.put(tokens[0] + "-" + tokens[1], v);
        }
        lrReader.start();

        while (!lrReader.isDone()) {
            sleep(100);
            lrReader.checkRestart();
        }
        for (ValidateMTBQEven3AeroTolls rt : threads.values()) {
            rt.join();
        }


        // We should recombine files here so we don't have to do it in a shell
        PrintWriter writer = new PrintWriter("out");
        for (String f : files) {
            reader = new BufferedReader(new FileReader(f + "-out"));
            while ((line = reader.readLine()) != null) {
                writer.println(line);
            }
            reader.close();
            writer.flush();
        }
        writer.close();

        //Set<String> threadKeys = threads.keySet();
        for (ValidateMTBQEven3AeroTolls vt : threads.values()) {
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

