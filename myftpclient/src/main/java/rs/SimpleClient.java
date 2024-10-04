package rs;
import java.io.*;
import java.net.*;

import java.util.List;
import java.util.Scanner;
import java.util.Arrays;
import java.util.HashMap;

public class SimpleClient {

    private Socket socketOfClient;
    private BufferedWriter os;
    private BufferedReader is;


    private String serverHost;
    private int serverPort;

    private Integer minNumber = null;
    private Integer maxNumber = null;

    private long initialTime;
    private long finalTime;
    private long sectionDuration;
    private HashMap<String, Long> durationMap = new HashMap<>(); 

    public SimpleClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void connectToServer() throws IOException {
        socketOfClient = new Socket(serverHost, serverPort);
        os = new BufferedWriter(new OutputStreamWriter(socketOfClient.getOutputStream()));
        is = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));
    }
    


    public void sendMessage(String message) throws IOException {

        initialTime = System.nanoTime();
    
        os.write(message);
        os.newLine();
        os.flush();
    
        String responseLine;
        while ((responseLine = is.readLine()) != null) {
            //System.out.println("Server: " + responseLine);
            if (responseLine.startsWith("OK from server: " + serverHost)) {
                if (responseLine.contains(", Result: ")) {

                    String result = responseLine.split(", Result: ")[1];
            
                    String[] numbers = result.replaceAll("[\\[\\]]", "").split(",");
                    int firstNumber = Integer.parseInt(numbers[0].trim());
                    int secondNumber = Integer.parseInt(numbers[1].trim());

                    // Read minNumber and maxNumber from file
                    File file = new File("minMaxNumbers.txt");
                    if (file.exists()) {
                        Scanner scanner = new Scanner(file);
                        this.minNumber = scanner.hasNextInt() ? scanner.nextInt() : null;
                        this.maxNumber = scanner.hasNextInt() ? scanner.nextInt() : null;
                        scanner.close();
                    }

                    if (minNumber == null || firstNumber < this.minNumber) {
                        this.minNumber = firstNumber;
                    }
    
                    if (maxNumber == null || secondNumber > this.maxNumber) {
                        this.maxNumber = secondNumber;
                    }

                    // Save minNumber and maxNumber to file
                    FileWriter writer = new FileWriter("minMaxNumbers.txt");
                    writer.write(String.valueOf(this.minNumber) + "\n");
                    writer.write(String.valueOf(this.maxNumber));
                    writer.close();

                    
                }
                break;
            }else if(responseLine.contains("TIME_DATA: ")){
                finalTime = System.nanoTime();
                sectionDuration = (finalTime - initialTime);
                
                long currentValue = durationMap.getOrDefault(message, 0L);

                if (sectionDuration>currentValue){
                    currentValue = sectionDuration;
                }

                durationMap.put(message, currentValue);
            }
        }
    }

    public void closeConnection() throws IOException {
        os.close();
        is.close();
        socketOfClient.close();
    }

    public HashMap<String, Long> getCollectedData(){
        return durationMap;
    }


}