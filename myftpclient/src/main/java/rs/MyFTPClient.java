package rs;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;
import java.util.stream.IntStream;
// read file lines
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class MyFTPClient {

    // Functions
    private FTPClient ftpClient;

    public MyFTPClient(String host, int port, String username, String password) throws Exception {
        this.ftpClient = new FTPClient();
        ftpClient.connect(host, port);
        ftpClient.login(username, password);
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

    }

    // display files
public void displayFiles() throws Exception {
    if (this.ftpClient != null && this.ftpClient.isConnected()) {
        String[] files = this.ftpClient.listNames();
        if (files != null && files.length > 0) {
            for (String file : files) {
                System.out.println(file);
            }
        } else {
            System.out.println("No files found in the current directory.");
        }
    }
}

    public void printCurrentDirectory() throws Exception {
       System.out.println(this.ftpClient.printWorkingDirectory());
}


public void changeDirectory(String directory) throws Exception {
    if (this.ftpClient != null && this.ftpClient.isConnected()) {
        this.ftpClient.changeWorkingDirectory(directory);
    }
}

    // check if file exists

    public boolean checkFileExists(String filename) throws Exception {
        FTPFile[] files = ftpClient.listFiles();
        for (FTPFile file : files) {
            if (file.getName().equals(filename)) {
                return true;
            }
        }
        return false;

    }


    // append file

    public void appendLineToFile(String filename, String content) throws Exception {
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        ftpClient.appendFile(filename, inputStream);
        int errorCode = ftpClient.getReplyCode();
        if (errorCode != 226) {
            System.out.println("File append failed. FTP Error code: " + errorCode);
        } else {
            //System.out.println("File appended successfully.");
        }
    }

    // delete file
    public boolean deleteFile(String filename) throws Exception {
        if (this.ftpClient != null && this.ftpClient.isConnected()) {
            boolean deleted = this.ftpClient.deleteFile(filename);
            if (deleted) {
                //System.out.println("File deleted successfully.");
            } else {
                System.out.println("Could not delete file, please check the filename.");
            }
            return deleted;
        } else {
            throw new Exception("FTP Client not connected.");
        }
    }

    // upload file

    public void uploadFile(String remoteFilePath, String localFilePath) throws Exception {
    try (InputStream inputStream = new FileInputStream(localFilePath)) {
        ftpClient.storeFile(remoteFilePath, inputStream);
        int errorCode = ftpClient.getReplyCode();
        if (errorCode != 226) {
            System.out.println("File upload failed. FTP Error code: " + errorCode);
        } else {
            //System.out.println("File uploaded successfully.");
        }
    }
}

    // List files

        public List<String> listFiles() {
        List<String> fileList = new ArrayList<>();
        try {
            FTPFile[] files = ftpClient.listFiles();
            for (FTPFile file : files) {
                if (file.isFile()) {
                    fileList.add(file.getName());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileList;
    }

    // upload file

    public void uploadContentAsStringToFile(String filename, String content) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
        ftpClient.storeFile(filename, inputStream);
        int errorCode = ftpClient.getReplyCode();
        if (errorCode != 226) {
            System.out.println("File upload failed. FTP Error code: " + errorCode);
        } else {
            //System.out.println("File uploaded successfully.");
        }

    }

    public void logout() throws Exception {
    if (this.ftpClient != null && this.ftpClient.isConnected()) {
        this.ftpClient.logout();
    }
}

public void disconnect() throws Exception {
    if (this.ftpClient != null && this.ftpClient.isConnected()) {
        this.ftpClient.disconnect();
    }
}
}
