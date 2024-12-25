package cs451;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

    public static boolean running = true;
    static Parser parser;
    static int maximumDifferentElements;

    private static void handleSignal() {
        //immediately stop network packet processing
        running = false;
        // if(!FIFOKeeper.pending.isEmpty()) {
        //     System.err.println("pending messages: " + FIFOKeeper.pending.keySet().size());
        // }
        // if(!LatticeAgreement.instances.isEmpty()) {
        //     System.err.println("open instances: " + LatticeAgreement.instances.keySet());
        // }
        // System.err.println("Stopping packet processing...");
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

        // long pid = ProcessHandle.current().pid();
        // System.out.println("My PID: " + pid + "\n");
        // System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");

        // System.out.println("My ID: " + parser.myId() + "\n");
        // System.out.println("List of resolved hosts is:");
        // System.out.println("==========================");
        // for (Host host: parser.hosts()) {
        //     System.out.println(host.getId());
        //     System.out.println("Human-readable IP: " + host.getIp());
        //     System.out.println("Human-readable Port: " + host.getPort());
        //     System.out.println();
        // }
        // System.out.println();

        // System.out.println("Path to output:");
        // System.out.println("===============");
        // System.out.println(parser.output() + "\n");

        // System.out.println("Path to config:");
        // System.out.println("===============");
        // System.out.println(parser.config() + "\n");

        // System.out.println("Doing some initialization\n");

        int nShots, maximumProposalSize;
        try {
            OutputLogger.begin(parser);
        } catch (IOException e) {
            System.err.println("Error opening output file: " + e.getMessage());
            return;
        }

        // System.out.println("Reading config file...");
        // System.out.println("I am host " + parser.myId() + ".");
        BufferedReader configReader;
        try {
            configReader = new BufferedReader(new FileReader(parser.config()));
            String config[] = configReader.readLine().split("\\s");
            nShots = Integer.parseInt(config[0]);
            maximumProposalSize = Integer.parseInt(config[1]);
            maximumDifferentElements = Integer.parseInt(config[2]);
        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
            return;
        }

        LatticeAgreement.begin(parser);

        try{
            for(int i = 0; i < nShots; i++) {
                    String[] line = configReader.readLine().split("\\s+");
                    Set<Integer> proposal = new HashSet<>(line.length);
                    for (String s : line) {
                        proposal.add(Integer.parseInt(s));
                    }
                    LatticeAgreement.propose(proposal);
            }
        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error proposing: " + e.getMessage());
        }

        try {
            configReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
