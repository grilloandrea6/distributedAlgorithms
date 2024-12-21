package cs451;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public class Main {

    public static boolean running = true;
    static Parser parser;
    static int maximumDifferentElements;

    private static void handleSignal() {
        //immediately stop network packet processing
        running = false;
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

        int nShots, maximumProposalSize;
        Set<Integer>[] proposals;
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
            nShots = Integer.parseInt(config[0]);
            maximumProposalSize = Integer.parseInt(config[1]);
            maximumDifferentElements = Integer.parseInt(config[2]);
            proposals = new HashSet[nShots];

            for(int i = 0; i < nShots; i++) {
                String[] line = configReader.readLine().split("\\s");
                proposals[i] = Arrays.stream(line).map((String s) -> Integer.parseInt(s)).collect(Collectors.toSet());
            }
            configReader.close();
        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
            return;
        }

        System.out.println("Number of shots: " + nShots);
        System.out.println("Maximum proposal size: " + maximumProposalSize);
        System.out.println("Maximum different elements: " + maximumDifferentElements);
        System.out.println("Proposals:");
        for (int i = 0; i < nShots; i++) {
            System.out.println("Proposal " + (i + 1) + ": " + proposals[i]);
        }

        LatticeAgreement.begin(parser);


        try {
            for(int i = 0; i < nShots; i++) {
                LatticeAgreement.propose(proposals[i]);
            }
        } catch (Exception e) {
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
