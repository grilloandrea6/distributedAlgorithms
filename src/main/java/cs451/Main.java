package cs451;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.TimerTask;

public class Main {

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        //write/flush output file if necessary
        System.out.println("Writing output.");
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
        Parser parser = new Parser(args);
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
        try {
            NetworkInterface networkInterface = new NetworkInterface(parser);
            networkInterface.addPacket(123, 0);
        } catch (SocketException e) {
            System.err.println("Error creating NetworkInterface: " + e.getMessage());
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("Broadcasting and delivering messages...\n");

        // new java.util.Timer().schedule(new TimerTask(){
        //     @Override
        //     public void run() {
        //         System.out.println("Executed...");
        //        //your code here 
        //        //1000*5=5000 millisec. i.e. 5 seconds. you can change accordingly 
        //     }
        // },1000*5,500); 
        // AckKeeper ackKeeper = new AckKeeper();
        // ackKeeper.addAck(1);
        // ackKeeper.addAck(2);
        // ackKeeper.addAck(3);
        // ackKeeper.addAck(4);
        // ackKeeper.addAck(6);
        // ackKeeper.addAck(5);
        // ackKeeper.addAck(7);
        // ackKeeper.addAck(10);
        // ackKeeper.addAck(9);
        // ackKeeper.addAck(8);
       
        System.out.println(Long.BYTES);

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
