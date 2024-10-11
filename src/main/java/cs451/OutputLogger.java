package cs451;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class OutputLogger {
    private static BufferedWriter writer;

    public static void begin(Parser p) throws IOException {
        writer = new BufferedWriter(new FileWriter(p.output()), 32768);
    }

    public static void logDeliver(int senderId, byte[] data) {
        synchronized (writer) {
            try {
                writer.write("d " + senderId + " " + NetworkInterface.bytesToInt(data));
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void logBroadcast(byte[] data) {
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
