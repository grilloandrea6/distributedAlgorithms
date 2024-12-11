package cs451;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class OutputLogger {
    private static BufferedWriter writer;

    public static void begin(Parser p) throws IOException {
        writer = new BufferedWriter(new FileWriter(p.output()), 32768);
    }

    public static void logDecide(Set<Integer> data) throws IOException {
        synchronized (writer) {
            if(Main.running) {
                for(int i : data) {
                    writer.write(i); // todo check if faster concat or double write
                    writer.write(" ");
                }
                
                writer.newLine();
            }
        }
    }

    public static void end() throws IOException {
        if(writer == null) return;

        synchronized (writer) {
            // System.out.println("OutputLogger closing file");
            writer.flush();
            writer.close();
        }
    }
}
