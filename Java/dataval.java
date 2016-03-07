import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * Created by Sung Kim on 3/2/2016.
   dataval.py: with a raw mitsim file perform the following:
   check for position reports that are not 30 secs apart, and simply report
   ensure car does not reappear after exiting
   remove negative positions and segments
   remove type 3 queries with a  day of '0' if any

   Usage: java dataval <raw_file> <cleaner_file>
*/
public class dataval {
    static final int MAX_CARID = 300000;

    static void printTokens(StringBuilder out_token, String[] tokens) {
        out_token.setLength(0);
        for (String token : tokens) {
            out_token.append(token+",");
        }
        System.out.println(out_token.deleteCharAt(out_token.length()-1));
    }
    static void printTokensToFile(PrintWriter writer, StringBuilder out_token, String[] tokens) {
        out_token.setLength(0);
        for (String token : tokens) {
            out_token.append(token+",");
        }
        writer.println(out_token.deleteCharAt(out_token.length()-1));
    }
    static void printITokensToFile(PrintWriter writer, StringBuilder out_token, int[] tokens) {
        out_token.setLength(0);
        for (int token : tokens) {
            out_token.append(token+",");
        }
        writer.println(out_token.deleteCharAt(out_token.length()-1));
    }

    public static void main(String[] args) throws Exception {
        long st = System.currentTimeMillis();
        String raw_file_name = args[0];
        String out_file_name = args[1];
        System.out.println("Validating data file: " + raw_file_name);
        int[] cars = new int[MAX_CARID];
        int[] exited = new int[MAX_CARID];
        File raw_file = new File(raw_file_name);
        File out_file = new File(out_file_name);
        BufferedReader reader;
        PrintWriter writer;
        reader = new BufferedReader(new FileReader(raw_file));
        writer = new PrintWriter(out_file);
        String line;
        StringBuilder out_token = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split(",");
            if (tokens[0].equals("0")) {
                int time = Integer.parseInt(tokens[1]);
                int carid = Integer.parseInt(tokens[2]);
                int lane = Integer.parseInt(tokens[5]);
                int seg = Integer.parseInt(tokens[7]);

                if (exited[carid] != 0) {
                    continue;
                }
                if (cars[carid] == 0) {
                    cars[carid] = time;
                } else {
                    if (cars[carid] != time - 30) {
                        System.out.println(cars[carid] + "-" + time);
                        System.out.println("Time error for car " + carid + " at time " + time);
                    }
                    cars[carid] = time;
                }
                if (tokens[5].equals("4")) {
                    exited[carid] = time;
                }
                if (seg < 0) {
                    printTokens(out_token, tokens);
                    tokens[7] = "0";
                    tokens[8] = "0";
                }
            } else if (tokens[0].equals("2")) {
            } else if (tokens[0].equals("3")) {
               if (tokens[14].equals("0")) {
                   continue;
               }
            }
            printTokensToFile(writer, out_token, tokens);
        }
        reader.close();
        writer.close();
        System.out.println("Time to run dataval.java: " + (System.currentTimeMillis() - st));
    }
}

