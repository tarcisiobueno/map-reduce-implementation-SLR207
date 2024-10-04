package rs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;



public class SimpleServerProgram {

    private static final int String = 0;
    private ServerSocket listener;
    private BufferedReader is;
    private BufferedWriter os;
    private Socket socketOfServer;
    private MyFTPServer myFTPServer;
    private boolean isServerRunning = false;
    
    private List<String> serverList;

    private List<String> groupList;

    public SimpleServerProgram(int port, MyFTPServer myFTPServer) {
        this.myFTPServer = myFTPServer;
        try {
            listener = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    public void startServer() throws Exception {
        while (true) {
            try {
                System.out.println("Server is waiting to accept user...");
    
                // Accept client connection request
                socketOfServer = listener.accept();
                System.out.println("Accept a client!");
    
                // Open input and output streams
                is = new BufferedReader(new InputStreamReader(socketOfServer.getInputStream()));
                os = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));
    
                String line;
                while (true) {
                    //System.out.println("Server in the while...");
                    // Read data to the server (sent from client).
                    line = is.readLine();
    
                    // If line is null, break out of the loop
                    if (line == null) {
                        os.write("OK from server: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();
                        break;
                    }
    
                    System.out.println("Received: " + line);
                    

                    if (line.startsWith("SERVERS,")){
                        serverList = Arrays.asList(line.substring(8).split(",")); 
                        System.out.println("Server list: " + serverList);
                        // Respond with "OK from server: " + serverHost to take client out of loop
                        os.write("OK from server: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();
                    }

                    // if the message equals MAP1
                    if (line.equals("MAP1")) {
                        os.write("TIME_DATA: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();
                        // do map 1
                        MapReduce mapReduce = new MapReduce();
                        mapReduce.map1(serverList);

                        // Respond with "OK from server: " + serverHost to take client out of loop
                        os.write("OK from server: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();
                    }

                    if (line.equals("SHUFFLE1")){

                        os.write("TIME_DATA: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();

                        MapReduce mapReduce = new MapReduce();

                        // add mapReduce.sendFiles
                        mapReduce.sendFilesToServers(serverList, "map1server", "shuffle1_server");

                        // delete the file text_to_be_treated.txt

                        mapReduce.deleteFilesContainingString("text_to_be_treated");
                        mapReduce.deleteFilesContainingString("map1");


                        // Respond with "OK from server: " + serverHost to take client out of loop
                        os.write("OK from server: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();
                    }

                    if (line.equals("REDUCE1")){

                        os.write("TIME_DATA: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();

                        MapReduce mapReduce = new MapReduce();
                        List<Integer> result = mapReduce.reduce1();

                        // Delete files starting with shuffle_server as they have been merged into shuffle_final

                        mapReduce.deleteFilesContainingString("shuffle1_server");


                        String message = "OK from server: " + socketOfServer.getLocalAddress().getHostName() + ", Result: " + result;
                        os.write(message);
                        os.newLine();
                        os.flush();
                    }

                    if(line.startsWith("GROUPS,")){
                        groupList = Arrays.asList(line.substring(7).split(",")); 

                        System.out.println("Groups: " + groupList);
                        // Respond with "OK from server: " + serverHost to take client out of loop
                        os.write("OK from server: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();
                    }

                    if(line.startsWith("MAP2")){

                        os.write("TIME_DATA: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();

                        MapReduce mapReduce = new MapReduce();
                        mapReduce.map2(groupList, serverList);

                        os.write("OK from server: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();
                    }

                    if(line.equals("SHUFFLE2")){

                        os.write("TIME_DATA: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();

                        MapReduce mapReduce = new MapReduce();

                        mapReduce.sendFilesToServers(serverList, "shuffle_group", "shuffle_group_from_server");

                        mapReduce.putFilesTogether("shuffle_group_from_server", "shuffle2_final");

                        mapReduce.deleteFilesContainingString("shuffle1_final");
                        
                        os.write("OK from server: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();
                    }

                    if(line.equals("REDUCE2")){

                        os.write("TIME_DATA: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();
                        
                        MapReduce mapReduce = new MapReduce();
                        mapReduce.reduce2();

                        mapReduce.deleteFilesContainingString("shuffle_group_from");
                        
                        os.write("OK from server: " + socketOfServer.getLocalAddress().getHostName());
                        os.newLine();
                        os.flush();
                    }
    
                    // If users send STOP (To end server).
                    if (line.equals("STOP")) {
                        System.out.println("Stopping server as per client request...");
                        System.exit(0);
                    }
    
                    // If users send QUIT (To end conversation).
                    if (line.equals("QUIT")) {
                        break;
                    }
                }
    
            } catch (IOException e) {
                System.out.println(e);
                e.printStackTrace();
            }
            
            System.out.println("Client disconnected, waiting for another client...");
        }
    }
}