package rs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import javafx.util.Pair;

import java.net.InetAddress;
import java.net.UnknownHostException;




public class MapReduce {

    public String getLocalIpAddress() {
    try {
        InetAddress inetAddress = InetAddress.getLocalHost();
        return inetAddress.getHostAddress();
    } catch (UnknownHostException e) {
        e.printStackTrace();
        return null;
    }
}

    public void sendFilesToServers(List<String> serverList, String localprefix, String sendPrefix) {
        int port = 3459;
        String username = "toto";
        String password = "tata";
        MyFTPClient[] myFTPClientArray = new MyFTPClient[serverList.size()];
    
        long timestamp = System.currentTimeMillis();
        String ipString = getLocalIpAddress();
    
        try {
            for (int i = 0; i < serverList.size(); i++) {
                myFTPClientArray[i] = new MyFTPClient(serverList.get(i), port, username, password);   
                String localFilePath = "/dev/shm/bueno-23/"+ localprefix + i + ".txt";
                String remoteFilePath = sendPrefix + i + "_" + ipString + ".txt";
                myFTPClientArray[i].uploadFile(remoteFilePath, localFilePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // disconnect
            for (MyFTPClient client : myFTPClientArray) {
                if (client != null) {
                    try {
                        client.logout();
                        client.disconnect();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                }
            }
        }
    }

    public void putFilesTogether(String prefix, String finalFile) {
        File dir = new File("/dev/shm/bueno-23");
        File[] files = dir.listFiles((d, name) -> name.startsWith(prefix));
        File outputFile = new File(dir, finalFile+".txt");
    
        Map<String, Integer> lineCounts = new HashMap<>();
    
        // get each line from each file and update the map
        try {
            if (files != null) {
                Arrays.sort(files);
                for (File file : files) {
                    List<String> lines = Files.readAllLines(file.toPath());
                    for (String line : lines) {
                        String[] parts = line.split(": ");
                        lineCounts.put(parts[0], Integer.parseInt(parts[1]));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        // sort the map entries by value in descending order, then by key in ascending order
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(lineCounts.entrySet());
        sortedEntries.sort((e1, e2) -> {
            int cmp = e2.getValue().compareTo(e1.getValue());
            if (cmp != 0) {
                return cmp;
            } else {
                return e1.getKey().compareTo(e2.getKey());
            }
        });
    
        // write the sorted lines to the final file
        try (FileWriter writer = new FileWriter(outputFile)) {
            for (Map.Entry<String, Integer> entry : sortedEntries) {
                writer.write(entry.getKey() + ": " + entry.getValue() + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Integer> reduce1() {
        File dir = new File("/dev/shm/bueno-23");
        File[] files = dir.listFiles((d, name) -> name.startsWith("shuffle1_server"));
        File outputFile = new File(dir, "shuffle1_final.txt");
    
        Map<String, Integer> counts = new HashMap<>();
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;

        try {
            if (files != null) {
                Arrays.sort(files);
                for (File file : files) {
                    List<String> lines = Files.readAllLines(file.toPath());
                    for (String line : lines) {
                        String[] parts = line.split(": ");
                        if (parts.length == 2) {
                            String key = parts[0];
                            int value = Integer.parseInt(parts[1]);
                            counts.put(key, counts.getOrDefault(key, 0) + value);
                            max = Math.max(max, counts.get(key));
                            min = Math.min(min, counts.get(key));
                        }
                    }
                }
            }
    
            try (FileWriter writer = new FileWriter(outputFile)) {
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    writer.write(entry.getKey() + ": " + entry.getValue() + System.lineSeparator());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Integer> minMax = new ArrayList<>();
        minMax.add(min);
        minMax.add(max);
        return minMax;
    }

    


    public void map1(List<String> serverList) throws IOException {

        int numberOfServers = serverList.size();
        BufferedWriter[] writers = new BufferedWriter[numberOfServers];
        for (int i = 0; i < numberOfServers; i++) {
            writers[i] = Files.newBufferedWriter(Paths.get("/dev/shm/bueno-23/map1server" + i + ".txt"));
        }

        File file = new File("/dev/shm/bueno-23/text_to_be_treated.txt");
        List<String> lines = Files.readAllLines(Paths.get(file.getAbsolutePath()));

        // Create a HashMap to store all words
        Map<String, Integer> wordCount = new HashMap<>();

        for (String line : lines) {
            // Split each line into words
            String[] words = line.split("\\s+");

            for (String word : words) {
                // If the word is already in the map, increment its count
                if (wordCount.containsKey(word)) {
                    wordCount.put(word, wordCount.get(word) + 1);
                } else {
                    // Otherwise, add the word to the map with a count of 1
                    wordCount.put(word, 1);
                }
            }
        }

        // Iterate over the wordCount map
        for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
            // Calculate the hash of each word
            int hash = entry.getKey().hashCode();

            // Calculate the server index for each word
            int serverIndex = Math.abs(hash % numberOfServers);

            // Write the word and its count to the corresponding server's file
            writers[serverIndex].write(entry.getKey() + ": " + entry.getValue());
            writers[serverIndex].newLine();
        }

        // Close all writers
        for (BufferedWriter writer : writers) {
            writer.close();
        }
    }

    public void map2(List<String> groupList, List<String> serverList) throws IOException {
        int numberOfServers = serverList.size();
        BufferedWriter[] writers = new BufferedWriter[numberOfServers];
        for (int i = 0; i < numberOfServers; i++) {
            writers[i] = Files.newBufferedWriter(Paths.get("/dev/shm/bueno-23/shuffle_group" + i + ".txt"));
        }
        
        // Read the shuffle_final.txt file
        File file = new File("/dev/shm/bueno-23/shuffle1_final.txt");
        List<String> lines = Files.readAllLines(Paths.get(file.getAbsolutePath()));
    
        for (String line : lines) {
            String[] parts = line.split(": ");
            String word = parts[0];
            int value = Integer.parseInt(parts[1]);
    
            //System.out.println("Word: " + word + ", Value: " + value);
            //System.out.println("Group List: " + groupList);
    
            for (int i = 0; i < serverList.size(); i++) {
                int min = Integer.parseInt(groupList.get(i));
                int max = (i < groupList.size() - 1) ? Integer.parseInt(groupList.get(i + 1)) - 1 : Integer.MAX_VALUE;
                //System.out.println("Interval : " + min + " - " + max);
                if (value >= min && value <= max) {
                    writers[i].write(word + ": " + value);
                    writers[i].newLine();
                }
            }
        }
    
        for (BufferedWriter writer : writers) {
            writer.close(); // close the writer after done writing
        }
    }

    public void reduce2() {
        String inputFile = "/dev/shm/bueno-23/shuffle2_final.txt";
        String outputFile = "/dev/shm/bueno-23/reduce2.txt";
    
        try {
            // Read lines from the input file
            List<String> lines = Files.readAllLines(Paths.get(inputFile));
    
            // Store entries in a TreeMap to maintain order
            Map<String, Integer> map = new TreeMap<>();
    
            // Parse each line and populate the map
            for (String line : lines) {
                String[] parts = line.split(": ");
                if (parts.length == 2) {
                    map.put(parts[0], Integer.parseInt(parts[1].trim()));
                }
            }
    
            // Convert map entries to a list and sort by value descending, then by key ascending
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());
            entries.sort((a, b) -> {
                int cmp = b.getValue().compareTo(a.getValue());
                if (cmp == 0) cmp = a.getKey().compareTo(b.getKey());
                return cmp;
            });
    
            // Write sorted entries to the output file
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile))) {
                for (Map.Entry<String, Integer> entry : entries) {
                    writer.write(entry.getKey() + ": " + entry.getValue());
                    writer.newLine();
                }
            }
    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteFilesContainingString(String searchString) {
        File dir = new File("/dev/shm/bueno-23/");
        File[] files = dir.listFiles((d, name) -> name.contains(searchString) && name.length() >= searchString.length());
        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    System.out.println("Failed to delete file: " + file.getName());
                }
            }
        }
    }
}