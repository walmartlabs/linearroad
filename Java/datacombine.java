import java.io.*;

/**
 * Created by cb on 3/2/2016.
 */
public class datacombine {
    final static int NUM_FIELDS = 15;
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        File outfile = new File(args[1]);
        PrintWriter writer = new PrintWriter(outfile);
        BufferedReader reader;
        String line;
        String[] tokens;
        int[] itokens = new int[15];
        int i;
        StringBuilder out_token = new StringBuilder();

        int maxCarId = 0;
        int maxQId = 0;
        int fileCount = 0;
        int curMaxCarId;
        int curMaxQId;

        if (!dir.isDirectory()) {
            System.err.println(dir.getName() + " is not a directory.");
            System.exit(1);
        }
        for (File f : dir.listFiles()) {
            //System.out.println(f);
            reader = new BufferedReader(new FileReader(f));
            curMaxCarId = 0;
            curMaxQId = 0;
            while ((line = reader.readLine()) != null) {
                tokens = line.split(",");
                i = 0;
                for (String t : tokens) {
                    itokens[i++] = Integer.parseInt(t);
                }
                if (itokens[2] > curMaxCarId) curMaxCarId = itokens[2];
                if (itokens[9] > curMaxQId) curMaxQId = itokens[9];
                if (fileCount > 0) {
                    itokens[2] += maxCarId;
                    if (itokens[0] != 0) itokens[9] += maxQId;
                    if (itokens[0] == 0) itokens[4] = fileCount;
                }
                dataval.printITokensToFile(writer, out_token, itokens);
            }
            maxCarId += curMaxCarId+1;
            maxQId += curMaxQId+1;
            fileCount++;
        }
        writer.close();
        System.out.println(maxCarId-1);
    }
}
