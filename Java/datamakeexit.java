import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * Created by Sung Kim on 3/2/2016.
 # datamakeexit.java: ensure that all vehicles get off the xway
 # run after dataval.java and datarm2.java
 # Usage: java datamakeexit <file>  <outfile>
 # Note: NOT using redirection since System.out.println() is VERY slow

 */
public class datamakeexit {
    static final int MAX_CARID = 300000;

    public static void main(String[] args) throws Exception {
        long st = System.currentTimeMillis();
        String input_file_name = args[0];
        String output_file_name = args[1];
        int[] lasttimes = new int[MAX_CARID];
        File input_file = new File(input_file_name);
        File output_file = new File(output_file_name);
        BufferedReader reader;
        PrintWriter writer;
        String line;
        String[] tokens;
        StringBuilder out_token = new StringBuilder();

        reader = new BufferedReader(new FileReader(input_file));

        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            int carid = Integer.parseInt(tokens[2]);
            int time = Integer.parseInt(tokens[1]);
            lasttimes[carid] = time;
        }
        reader.close();
        System.err.println("Time for first read: " + (System.currentTimeMillis() - st));

        st = System.currentTimeMillis();
        reader = new BufferedReader(new FileReader(input_file));
        writer = new PrintWriter(output_file);
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            int carid = Integer.parseInt(tokens[2]);
            int time = Integer.parseInt(tokens[1]);
            if (time == lasttimes[carid] && tokens[0].equals("0")) {
                tokens[3] = "10";
                tokens[5] = "4";
            }
            dataval.printTokensToFile(writer, out_token, tokens);
        }
        reader.close();
        writer.close();
        System.err.println("Time for second read: " + (System.currentTimeMillis() - st));
    }
}
