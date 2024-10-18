package cs451;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.TimerTask;

public class Main {

    public static boolean running = true;

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        running = false;

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

        int nMessages;
        int deliveryHost;
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
            deliveryHost = Integer.parseInt(config[1]);
            configReader.close();
            if(deliveryHost != parser.myId()) {
                System.out.println("I will send " + nMessages + " messages to host " + deliveryHost + ".");
            } else {
                System.out.println("I expect to deliver " + nMessages + " messages from all other hosts.");
            }
        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
            return;
        }

        try {
            NetworkInterface.begin(parser, deliveryHost != parser.myId());
        } catch (SocketException e) {
            System.err.println("Error starting NetworkInterface: " + e.getMessage());
            return;
        }

        System.out.println("\nBroadcasting and delivering messages...\n");

        if(deliveryHost != parser.myId()) {
            PerfectLinksSender.begin(parser);
            for (int i = 0; i < nMessages; i++) {
                System.out.println("Main - Sending message " + i + " to host " + deliveryHost);
                List<Byte> data = NetworkInterface.intToBytes(i);

                PerfectLinksSender.perfectSend(data, deliveryHost);
            }
        }

        System.out.println("Main - Finished broadcasting and delivering messages.");
        

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
        
        // ackKeeper.addAck(4);
        // ackKeeper.addAck(5);
        // ackKeeper.addAck(6);
        // ackKeeper.addAck(7);
        // ackKeeper.addAck(8);
        // ackKeeper.addAck(9);
        // ackKeeper.addAck(10);
        // ackKeeper.addAck(3);
        // for (int i = 250; i < 300; i++) {
        //     ackKeeper.addAck(i);
        // }
        
       
        //System.out.println(Long.BYTES);

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        // while (true) {
        //     // Sleep for 1 hour
        //     Thread.sleep(60 * 60 * 1000);
        // }
    }
}
