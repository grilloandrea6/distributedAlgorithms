package cs451;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;

public class Main {

    public static boolean running = true;
    static Parser parser;

    private static void handleSignal() {
        //immediately stop network packet processing
        running = false;
        System.out.println("Immediately stopping network packet processing.");


        
        System.err.printf("| %2d  | %12.0f us | %12.0f us | %10d times | %12.0f us | %12.0f us | %10d times | %12.0f us | %12.0f us | %10d times | %10d maxSize|\n", 
            parser.myId(),
            NetworkInterface.timeForAckReceived / 1000.0, 
            NetworkInterface.maximumTimeForAckReceived / 1000.0, 
            NetworkInterface.timesOverMillisecondAckReceived,
            NetworkInterface.timeForProcessPacket / 1000.0,
            NetworkInterface.maximumTimeForProcessPacket / 1000.0,
            NetworkInterface.timesOverMillisecondProcessPacket,
            PerfectLinks.timeForLockAckReceived / 1000.0,
            PerfectLinks.maximumTimeForLockAckReceived / 1000.0,
            PerfectLinks.nTimesOverMillisecond,
            PerfectLinks.maxQueueSize);

        

        //write/flush output file if necessary
        System.out.println("Writing output.");
        try {
            OutputLogger.end();
        } catch (IOException e) {
            System.err.println("Error closing output file: " + e.getMessage());
        }
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");

        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        for (Host host: parser.hosts()) {
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
        }
        System.out.println();

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");

        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.config() + "\n");

        System.out.println("Doing some initialization\n");

        int nMessages;
        try {
            OutputLogger.begin(parser);
        } catch (IOException e) {
            System.err.println("Error opening output file: " + e.getMessage());
            return;
        }

        System.out.println("Reading config file...");
        System.out.println("I am host " + parser.myId() + ".");
        try {
            BufferedReader configReader = new BufferedReader(new FileReader(parser.config()));
            String config[] = configReader.readLine().split("\\s");
            nMessages = Integer.parseInt(config[0]);
            configReader.close();
            System.out.println("I will broadcast " + nMessages + " messages.");

        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
            return;
        }

        try {
            NetworkInterface.begin(parser);
        } catch (SocketException e) {
            System.err.println("Error starting NetworkInterface: " + e.getMessage());
            return;
        }

        System.out.println("\nBroadcasting and delivering messages...\n");

        FIFOUniformReliableBroadcast.begin(parser);
        
        Thread.sleep(2000); //ToDo remove

        for(int i = 1; i <= nMessages; i++) {
            System.out.println("Main - Broadcasting message " + i);
            List<Byte> data = NetworkInterface.intToBytes(i);
            FIFOUniformReliableBroadcast.broadcast(data);
        }
       
        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
