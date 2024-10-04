package rs;

public class Main {

   public static void main(String[] args) {

        /*MyFTPServer myFTPServer = new MyFTPServer();
        myFTPServer.setUpServer();*/

        // FTP credentials

        MyFTPServer myFTPServer = new MyFTPServer();
        SimpleServerProgram simpleServerProgram = new SimpleServerProgram(9976, myFTPServer);

        // Start the FTP server in a new thread
        new Thread(() -> {
            myFTPServer.setUpServer();
        }).start();

        // Start the Socket server in a new thread
        new Thread(() -> {
            try {
                simpleServerProgram.startServer();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }).start();

    }

}