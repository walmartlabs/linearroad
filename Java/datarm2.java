import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * Created by Sung Kim on 3/2/2016.
 * datarm2.java: remove carid's with only one or two records
 * Usage: datarm2 <file>  <outfile>
 */
public class datarm2_1 {
    static final int MAX_CARID = 300000;

    public static void main(String[] args) throws Exception {
        long st = System.currentTimeMillis();
        String input_file_name = args[0];
        String output_file_name = args[1];
        int[] counts = new int[MAX_CARID];
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
            if (counts[carid] == 0) {
                counts[carid] = 1;
            } else {
                counts[carid]++;
            }
        }
        reader.close();
        System.err.println("Time for first read: " + (System.currentTimeMillis() - st));

        st = System.currentTimeMillis();
        reader = new BufferedReader(new FileReader(input_file));
        writer = new PrintWriter(output_file);
        while ((line = reader.readLine()) != null) {
            tokens = line.split(",");
            int carid = Integer.parseInt(tokens[2]);
            if (counts[carid] > 2) {
                if (!tokens[0].equals("4")) {
                    if (tokens[0].equals("3")) {
                        if (tokens[14].equals("0")) continue;
                    }
                   dataval.printTokensToFile(writer, out_token, tokens);
                }
            }
        }
        reader.close();
        writer.close();
        System.err.println("Time for second read: " + (System.currentTimeMillis() - st));
    }
}
