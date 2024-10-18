package cs451;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class OutputLogger {
    private static BufferedWriter writer;

    public static void begin(Parser p) throws IOException {
        writer = new BufferedWriter(new FileWriter(p.output()), 32768);
    }

    public static void logDeliver(int senderId, List<Byte> data) {
        synchronized (writer) {
            try {
                //System.out.println("should deliver lenght " + data.size());
                while(!data.isEmpty()) {
                    data.remove(0);
                    writer.write("d " + senderId + " " + NetworkInterface.bytesToInt(data));
                    writer.newLine();
                    data.remove(0);
                    data.remove(0);
                    data.remove(0);
                    data.remove(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void logBroadcast(List<Byte> data) {
        synchronized (writer) {
            try {
                writer.write("b " + NetworkInterface.bytesToInt(data));
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void end() throws IOException {
        synchronized (writer) {
            System.out.println("OutputLogger closing file");
            writer.close();
        }
    }
}
