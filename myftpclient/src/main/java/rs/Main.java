package rs;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.apache.commons.net.ftp.FTPClient;

import rs.SimpleClient;

public class Main {

    public static List<List<String>> allocateSplitsToServers(List<String> contents, int numServers) {
        List<List<String>> serverSplits = new ArrayList<>();
        for (int i = 0; i < numServers; i++) {
            serverSplits.add(new ArrayList<>());
        }

        for (int i = 0; i < contents.size(); i++) {
            serverSplits.get(i % numServers).add(contents.get(i));
        }

        return serverSplits;
    }

    public static void updatePhaseSyncValue(String message, HashMap<String, Long> durationMap, List<String> serverList, SimpleClient simpleClient){
        HashMap<String, Long> collectedData = simpleClient.getCollectedData();
        long currentValue = durationMap.getOrDefault(message, 0L);
        if(collectedData.get(message)>currentValue){
            String key = serverList.size() + "_SYNC_" + message;
            durationMap.put(key,collectedData.get(message));
        }
    }

    public static void writeDurationMapToCSV(Map<String, Long> durationMap, String filePath) {
        File file = new File(filePath);
        boolean fileExists = file.exists();
    
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (!fileExists) {
                // Prepare headers
                String headers = "n_servers," + durationMap.keySet().stream()
                        .sorted()
                        .map(key -> key.substring(key.indexOf('_') + 1)) // Remove the number and underscore
                        .collect(Collectors.joining(","));
                writer.write(headers);
                writer.newLine();
            }
            
            // Prepare the data row
            // Assuming all keys have the same 'n_servers' value, extract it from the first key
            String firstKey = durationMap.keySet().iterator().next();
            String nServers = firstKey.split("_")[0];
            String dataRow = nServers + "," + durationMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getValue().toString()) // Directly use the value
                    .collect(Collectors.joining(","));
    
            writer.write(dataRow);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        
        
        // ******** SET VARIABLES ********
        
        List<String> serverList;
        serverList = new ArrayList<>();
        int port = 3459;
        String username = "toto";
        String password = "tata";
        int socketPort = 9976;
        HashMap<String, Long> durationMap = new HashMap<>();
        int numberOfServers = 2;
        
        if (args.length > 0) {
            numberOfServers = Integer.parseInt(args[0]);
        }

        try {
        
        // The python code sends the serverList.txt file to the remote machine, so it can get the list of machine IDs

        serverList = Files.readAllLines(Paths.get("serverList.txt"));
        // Inside your method or constructor
        try {
            serverList = Files.lines(Paths.get("serverList.txt"))
                                        .limit(numberOfServers)
                                        .collect(Collectors.toList());

            // Use serverList as needed
        } catch (IOException e) {
            e.printStackTrace();
        }

        }catch (IOException e) {
            e.printStackTrace();
            System.out.println("File Not Found");
        }

        // Create myftp client object to use its methods

        MyFTPClient[] myFTPClientArray = new MyFTPClient[serverList.size()];
        for (int i = 0; i < serverList.size(); i++) {
            myFTPClientArray[i] = new MyFTPClient(serverList.get(i), port, username, password);
        }

        // Download the contents of the following file 
        
        File file = new File("/cal/commoncrawl/CC-MAIN-20230320144934-20230320174934-00001.warc.wet");
        List<String> lines = Files.readAllLines(Paths.get(file.getAbsolutePath()));

        // ******** PREPARE THE REMOTE MACHINES ********
        // Check if the file text_to_be_treated.txt exists, if does, it deletes it

        for (MyFTPClient client : myFTPClientArray) {
            if (client.checkFileExists("text_to_be_treated.txt")) {
                client.deleteFile("text_to_be_treated.txt");
            }
        }

        // Delete files for which the name start with server or shuffle

        List<String> prefixes = Arrays.asList("server", "shuffle");

        for (MyFTPClient client : myFTPClientArray) {
            List<String> files = client.listFiles();  
   
            for (String fileName : files) {
             
                for (String prefix : prefixes) {
                    if (fileName.startsWith(prefix)) {
                      
                        client.deleteFile(fileName);
                 
                        break;
                    }
                }
            }
        }

        // ******** END PREPARE THE REMOTE MACHINES ********

        // ******** SEPARATE LINES OF THE FILE BASED ON THE NUMBER OF NODES ********

        List<List<String>> serverSplits = allocateSplitsToServers(lines, serverList.size());

        // ******** END SEPARATE LINES OF THE FILE BASED ON THE NUMBER OF NODES ********
        
        // ******** SEND FILES TO NODES ********

        long initialTime = System.nanoTime();

        for (int serverIndex = 0; serverIndex < serverSplits.size(); serverIndex++) {
            List<String> serverLines = serverSplits.get(serverIndex);
            StringBuilder contentBuilder = new StringBuilder();
            for (String line : serverLines) {
                contentBuilder.append(line).append("\n");
            }
            String content = contentBuilder.toString();
            try {
                boolean fileExists = myFTPClientArray[serverIndex].checkFileExists("text_to_be_treated.txt");
        
                if (fileExists) {
                    myFTPClientArray[serverIndex].appendLineToFile("text_to_be_treated.txt", content);
                } else {
                    myFTPClientArray[serverIndex].uploadContentAsStringToFile("text_to_be_treated.txt", content);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

       
       for (MyFTPClient client : myFTPClientArray) {
        client.logout();
        client.disconnect();
    }

    long finalTime = System.nanoTime();
    long sectionDuration = (finalTime - initialTime);
    String keyName = serverList.size() + "_COMMUN_SEND_SPLITS";
    durationMap.put(keyName, sectionDuration);

    // ******** END SEND FILES TO NODES ********

    // ******** SEND_IP ********

    System.out.println("Sending Servers");

    initialTime =  System.nanoTime();

    for (String server : serverList) {
        SimpleClient simpleClient = new SimpleClient(server+".enst.fr", socketPort);
        simpleClient.connectToServer();
        simpleClient.sendMessage("SERVERS," + serverList.stream().collect(Collectors.joining(",")));        
        simpleClient.closeConnection();
    }

    finalTime = System.nanoTime();
    sectionDuration = (finalTime - initialTime);
    keyName = serverList.size() + "_SYNC_SEND_IP";
    durationMap.put(keyName, sectionDuration);
    
    // ******** END SEND_IP ********

    // ******** MAP1 ********

    System.out.println("Map1");

    initialTime = System.nanoTime();
  
    for (String server : serverList) {

        String message = "MAP1";

        SimpleClient simpleClient = new SimpleClient(server+".enst.fr", socketPort);
        simpleClient.connectToServer();
        simpleClient.sendMessage(message);

        updatePhaseSyncValue(message, durationMap, serverList, simpleClient);

        simpleClient.closeConnection();
    }

    finalTime = System.nanoTime();
    sectionDuration = (finalTime - initialTime);
    keyName = serverList.size() + "_COMP_MAP1_WITH_SYNC_MAP1";
    durationMap.put(keyName, sectionDuration);

    // ******** END MAP1 ********

    // ******** SHUFFLE1 ********

    System.out.println("SHUFFLE1");

    initialTime = System.nanoTime();

    for (String server : serverList) {

        String message = "SHUFFLE1";

        SimpleClient simpleClient = new SimpleClient(server+".enst.fr", socketPort);
        simpleClient.connectToServer();
        simpleClient.sendMessage(message);

        updatePhaseSyncValue(message, durationMap, serverList, simpleClient);

        simpleClient.closeConnection();
    }

    finalTime = System.nanoTime();
    sectionDuration = (finalTime - initialTime);
    keyName = serverList.size() + "_COMMUN_SHUFFLE1_WITH_SYNC_SHUFFLE1";
    durationMap.put(keyName, sectionDuration);

    // ******** END SHUFFLE1 ********

    // ******** REDUCE1 ******** 

    System.out.println("REDUCE1");

    initialTime = System.nanoTime();

    for (String server : serverList) {

        String message = "REDUCE1";

        SimpleClient simpleClient = new SimpleClient(server+".enst.fr", socketPort);
        simpleClient.connectToServer();
        simpleClient.sendMessage(message);

        updatePhaseSyncValue(message, durationMap, serverList, simpleClient);

        simpleClient.closeConnection();
    }

    finalTime = System.nanoTime();
    sectionDuration = (finalTime - initialTime);
    keyName = serverList.size() + "_COMP_REDUCE1_WITH_SYNC_REDUCE1";
    durationMap.put(keyName, sectionDuration);

    // ******** END REDUCE1 ******** 

    // ******** READ MIN MAX AND COMPUTE GROUPS ******** 

    File fileName = new File("minMaxNumbers.txt");
    int[] groups = new int[serverList.size()];

    if (fileName.exists()) {
        Scanner scanner = new Scanner(fileName);
        Integer minNumber = scanner.hasNextInt() ? scanner.nextInt() : null;
        Integer maxNumber = scanner.hasNextInt() ? scanner.nextInt() : null;

        // compute groups based on number of servers 

        int groupSize = (maxNumber - minNumber) / serverList.size();
        groups = IntStream.range(0, serverList.size()).map(i -> minNumber + i * groupSize).toArray();
        scanner.close();

    }

    // ******** END READ MIN_MAX AND COMPUTE GROUPS ********


    // ******** SENDING GROUPS ******** 

    System.out.println("GROUP");

    initialTime = System.nanoTime();

    for (String server : serverList) {
        SimpleClient simpleClient = new SimpleClient(server+".enst.fr", socketPort);
        simpleClient.connectToServer();
        String groupMessage = Arrays.stream(groups)
                                    .mapToObj(Integer::toString)
                                    .collect(Collectors.joining(","));
        simpleClient.sendMessage("GROUPS," + groupMessage);
        simpleClient.closeConnection();
    }

    finalTime = System.nanoTime();
    sectionDuration = (finalTime - initialTime);
    keyName = serverList.size() + "_SYNC_SEND_GROUPS";
    durationMap.put(keyName, sectionDuration);

    // ******** END SENDING GROUPS ********


    // ******** MAP2 ******** 

    System.out.println("MAP2");

    initialTime = System.nanoTime();

    for (String server : serverList) {

        String message = "MAP2";

        SimpleClient simpleClient = new SimpleClient(server+".enst.fr", socketPort);
        simpleClient.connectToServer();
        simpleClient.sendMessage(message);

        updatePhaseSyncValue(message, durationMap, serverList, simpleClient);

        simpleClient.closeConnection();
    }

    finalTime = System.nanoTime();
    sectionDuration = (finalTime - initialTime);
    keyName = serverList.size() + "_COMP_MAP2_WITH_SYNC_MAP2";
    durationMap.put(keyName, sectionDuration);

    // ******** END MAP2 ******** 


    // ******** SHUFFLE 2 ******** //

    System.out.println("SHUFFLE2");

    initialTime = System.nanoTime();

    for (String server : serverList) {

        String message = "SHUFFLE2";

        SimpleClient simpleClient = new SimpleClient(server+".enst.fr", socketPort);
        simpleClient.connectToServer();
        simpleClient.sendMessage("SHUFFLE2");

        updatePhaseSyncValue(message, durationMap, serverList, simpleClient);

        simpleClient.closeConnection();
    }

    finalTime = System.nanoTime();
    sectionDuration = (finalTime - initialTime);
    keyName = serverList.size() + "_COMMUN_SHUFFLE2_WITH_SYNC_SHUFFLE2";
    durationMap.put(keyName, sectionDuration);

    // ******** REDUCE 2 ******** 

    System.out.println("REDUCE2");

    initialTime = System.nanoTime();

    for (String server : serverList) {

        String message = "REDUCE2";
        
        SimpleClient simpleClient = new SimpleClient(server+".enst.fr", socketPort);
        simpleClient.connectToServer();
        simpleClient.sendMessage("REDUCE2");

        updatePhaseSyncValue(message, durationMap, serverList, simpleClient);

        simpleClient.closeConnection();
    }

    finalTime = System.nanoTime();
    sectionDuration = (finalTime - initialTime);
    keyName = serverList.size() + "_COMP_REDUCE2_WITH_SYNC_REDUCE2";
    durationMap.put(keyName, sectionDuration);

    // ******** END REDUCE 2 ********

    durationMap.forEach((key, value) -> System.out.println(key + ": " + value));

    String filePath = "durations.csv";
    writeDurationMapToCSV(durationMap, filePath);

    }
}
