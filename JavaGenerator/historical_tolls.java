import java.io.File;
import java.io.PrintWriter;

/**
 * Created by Sung Kim on 3/2/2016.
 *
 * Usage: java historical_tolls <num xways> <max carid> <outfile>
 * NOTE: num xway is the number of xways, not the 0-indexed largest xway.  So, 3 files => java historical_tolls 3 <max carid> <outfile>
 */

public class historical_tolls {
    final static int NUM_DAYS_IN_HISTORY = 70;

    public static void main(String[] args) throws Exception {
        int maxXway = Integer.parseInt(args[0]);
        int maxCarId = Integer.parseInt(args[1]);
        File outfile = new File(args[2]);
        int i, day, toll, xway;
        maxCarId++; // we want to include the max carid in the result set
        PrintWriter writer = new PrintWriter(outfile);
        for (i = 0; i < maxCarId; i++) {
            for (day = 1; day < NUM_DAYS_IN_HISTORY; day++) {
                toll = (int) (Math.random() * 1000) % 90 + 10;
                xway = (int) (Math.random() * 1000) % maxXway;
                writer.println(i + "," + day + "," + xway + "," + toll);
            }
        }
        writer.close();
    }
}
