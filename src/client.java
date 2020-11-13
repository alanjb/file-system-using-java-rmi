import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class client implements Serializable {

    private static DataInputStream inFromServer = null;

    private static DataOutputStream outToServer = null;

    private static Socket clientSocket = null;

    public static void main(String[] args) throws IOException {

        String serverName = System.getenv("PA2_SERVER");

        System.out.println("Server name: " + serverName);

        try {

            if (serverName != null) {

                String[] vars = System.getenv("PA2_SERVER").split(":");

                String hostName = vars[0];

                int portNumber = Integer.parseInt(vars[1]);

                if(hostName.equalsIgnoreCase("localhost") && portNumber == 8000){

                    Registry registry = LocateRegistry.getRegistry(hostName, portNumber);

                    service stub = (service)registry.lookup("remoteObject");

                    init(stub, hostName, portNumber, args);

                } else {

                    System.out.println("Wrong parameters entered. Please try again.");

                    System.exit(1);

                }
            } else {
                System.out.println("PA1_SERVER environment variable not set...");
            }
        }
        catch (Exception error) {
            System.out.println("ERROR: Cannot connect to Server" + error.getMessage());
        }
    }

    private static void init(service remoteObj, String server, int port, String[] args) {
        try {
            clientSocket = new Socket(server, port);

            initStreams();

            runCommand(remoteObj, args);

        } catch (Exception error) {
            System.out.println("503 Service Unavailable: there was an issue connecting to the server: " + error);
        }
    }

    private static String getExecutionPathOfCurrentClient(){
        String executionPath = null;

        try {
            executionPath = System.getProperty("user.dir");
        } catch(Exception e){
            System.out.println("There was an error getting execution for this system.");
            e.printStackTrace();
        }

        return executionPath;
    }

    private static void initStreams() throws IOException {
        inFromServer = new DataInputStream(clientSocket.getInputStream());
        outToServer = new DataOutputStream(clientSocket.getOutputStream());
    }

    private static void runCommand(service remoteObj, String[] args) throws IOException {
        String userCommand = args[0];

        try {
            switch (userCommand) {
                case "upload" -> {
                    System.out.println("Upload: Sending file to server...");
                    upload(remoteObj, args[1], args[2]);
                }

                case "download" -> {
                    System.out.println("Download: Calling server to retrieve file...");
                    download(args[1], args[2]);
                }

                case "dir" -> {
                    System.out.println("List: Calling server to retrieve directory items...");
                    dir(remoteObj, args[1]);
                }

                case "mkdir" -> {
                    System.out.println("Create Directory: Calling server to remove file...");
                    mkdir(remoteObj, args[1]);
                }

                case "rmdir" -> {
                    System.out.println("Remove Directory: Calling server to remove file...");
                    rmdir(remoteObj, args[1]);
                }

                case "rm" -> {
                    System.out.println("Remove file: Calling server to remove file...");
                    removeFile(remoteObj, args[1]);
                }

                case "shutdown" -> {
                    System.out.println("Shutting down server. Goodbye.");
                    shutdown(remoteObj);
                }

                default -> System.out.println("Please enter a valid command");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (clientSocket != null) {
                clientSocket.close();
                inFromServer.close();
                outToServer.close();
            }
        }
    }

    private static void shutdown(service remoteObj) throws IOException {
        System.out.println("Shutting down server...");
        remoteObj.shutdown();
    }

    private static void removeFile(service remoteObj, String filePathOnServer) throws IOException, FileNotFoundException {

        boolean fileExists = remoteObj.removeFile(filePathOnServer);

        try {
            //if file exists on server
            if (!fileExists) {
                System.err.println("404 ERROR: File does not exist on server. Please try again.");
            } else {
                System.out.println(filePathOnServer + " has been removed.");
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void upload(service remoteObj, String filePathOnClient, String filePathOnServer) throws IOException {
        String executionPathOnClient = getExecutionPathOfCurrentClient();
        File file = new File(executionPathOnClient + File.separator + filePathOnClient);
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        long filePosition = 0;
        long fileSize = file.length();
        String fileName = file.getName();
        String clientName = getExecutionPathOfCurrentClient();

        try {

            boolean fileExistsAndClientIsOwner = remoteObj.handleFileCheck(fileName, clientName, filePathOnServer, fileSize);

            long filePos = remoteObj.handlePrepareUpload(fileName, clientName, filePathOnServer, fileSize, fileExistsAndClientIsOwner);

            boolean wasUploaded = remoteObj.upload(fileName, clientName, filePathOnServer, fileSize, fileExistsAndClientIsOwner);

            if(filePos > 0){

                System.out.println("Resuming upload for file: " + fileName);

                long position = inFromServer.readLong();

                System.out.println("File position: " + position);

                raf.seek(position);

                filePosition = position;

            } else {
                System.out.println("Starting a new upload for file: " + fileName);
            }

            int read = 0;
            int remaining = Math.toIntExact(fileSize);
            byte[] buffer = new byte[1024];

            while((read = raf.read(buffer, 0, Math.min(buffer.length, remaining))) > 0){
                filePosition += read;
                remaining -= read;
                System.out.print(
                        "\r Uploading file..."
                        + (int)((double)(filePosition)/fileSize * 100)
                        + "%");
                outToServer.write(buffer);
            }

            if(filePosition >= fileSize){
                System.out.print(
                        "\r Uploading file...100%"
                );
                System.out.println("\n\n File Upload Complete");
            }

            if(!wasUploaded){
                System.out.println("THERE WAS AN ERROR ON THE SERVER UPLOADING THE FILE. PLEASE TRY AGAIN.");
            }

            raf.close();

        } catch(Exception e){
            System.out.println("There was an interruption when uploading file. Please retry to complete \n.");
            e.printStackTrace();
        }
    }

    private static void download(String filePathOnServer, String filePathOnClient) throws IOException {
        String command = "download";
        String executionPath = getExecutionPathOfCurrentClient();

        //send command to server
        outToServer.writeUTF(command);

        //send file path to server
        outToServer.writeUTF(filePathOnServer);

        //get file size to compare
        long fileSizeOfFileOnServer = inFromServer.readLong();

        long filePosition = 0;

        boolean shouldResumeDownload = false;

        try {
            if(inFromServer.readBoolean()) {
                System.out.println("File exists on server...");

                File file = new File(executionPath + File.separator +  filePathOnClient);

                if(file.exists() && (file.length() < fileSizeOfFileOnServer)){
                    outToServer.writeBoolean(true);

                    long filePositionForSeek = file.length();

                    outToServer.writeLong(filePositionForSeek);

                    filePosition = filePositionForSeek;

                    System.out.println("Resuming download for: " + file.getName());

                    shouldResumeDownload = true;

                } else {
                    outToServer.writeBoolean(false);
                }

                try {
                    byte[] buffer = new byte[1024];
                    int read = 0;
                    int remaining = Math.toIntExact(fileSizeOfFileOnServer);
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");

                    if(shouldResumeDownload){
                        raf.seek(filePosition);
                    }

                    while ((read = inFromServer.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
                        filePosition += read;
                        remaining -= read;
                        System.out.print(
                                "\r Downloading file..." +
                                        (int) ((double) (filePosition) / fileSizeOfFileOnServer * 100) +
                                        "%");
                        raf.write(buffer, 0, read);
                    }

                    if (filePosition >= fileSizeOfFileOnServer) {
                        System.out.println("\n File Download Complete");
                        //remove from hashmap since the file completed
                    } else {
                        System.out.println("\n There was an interruption when uploading file. Please retry to complete.");
                    }

                    raf.close();

                } catch (Exception e) {
                    System.out.println("\n Something went wrong as the client was uploading a file.");
                    e.printStackTrace();
                }

            } else {
                System.out.println("404 ERROR. The file you requested to download does not exist on server.");
            }

        } catch (Exception e) {
            System.out.println("An error occurred attempting to receive file on server.");
            e.printStackTrace();
        }
    }

    private static void rmdir(service remoteObj, String filePathOnServer) throws IOException {

        try {
            System.out.println("Sending request to remove directory ...");

            boolean wasRemoved = remoteObj.removeDirectory(filePathOnServer);

            if(wasRemoved){
                System.out.println("SUCCESS! The directory was removed at..." + filePathOnServer);
            } else {
                System.err.println("404 ERROR: Directory could not be removed. Please try again");
            }
        } catch(Exception e){
            System.err.println("404 ERROR: There was an error trying to remove the directory.");
            e.printStackTrace();
        }
    }

    private static void dir(service remoteObj, String filePathOnServer) {

        try {
            System.out.println("Retrieving directory items...");

            //call remoteObj listDirectoryItems method
            String[] list = remoteObj.listDirectoryItems(filePathOnServer);

            System.out.println("Directory items in " + filePathOnServer + ": \n");

            for (String s : list) {
                System.out.println(s);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void mkdir(service remoteObj, String filePathOnServer) throws IOException {

        try {

            System.out.println("Sending directory creation request to server...");

            boolean wasRemoved = remoteObj.createDirectory(filePathOnServer);

            if (wasRemoved) {

                System.out.println("Successfully created directory at: " + filePathOnServer);

            } else {

                System.out.println("400 ERROR: There was an issue creating directory at: " + filePathOnServer + ".  Please try again.");
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}