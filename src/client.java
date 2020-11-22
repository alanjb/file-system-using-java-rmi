import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class client implements Serializable {

    public static void main(String[] args) throws IOException {
        String serverName = System.getenv("PA2_SERVER");

        try {
            if (serverName != null) {
                String[] vars = System.getenv("PA2_SERVER").split(":");
                String hostName = vars[0];
                int portNumber = Integer.parseInt(vars[1]);

                if(hostName.equalsIgnoreCase("localhost") && portNumber == 8000){
                    Registry registry = LocateRegistry.getRegistry(hostName, portNumber);
                    service stub = (service)registry.lookup("remoteObject");
                    runCommand(stub, args);

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
                    download(remoteObj, args[1], args[2]);
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
                    System.out.println("Shutting down server. Goodbye!");
                    shutdown(remoteObj);
                }

                default -> System.out.println("Please enter a valid command");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void upload(service remoteObj, String filePathOnClient, String filePathOnServer) throws IOException {
        String executionPathOnClient = getExecutionPathOfCurrentClient();
        File file = new File(executionPathOnClient + File.separator + filePathOnClient);
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        String fileName = file.getName();
        long filePosition = 0;
        long fileSize = file.length();
        int count = 0;
        int bufferSize = 1024;

        try {
            boolean fileExistsAndClientIsOwner = remoteObj.handleFileCheck(executionPathOnClient, filePathOnServer);
            long[] fileInfoArray = remoteObj.handlePrepareUpload(fileName, executionPathOnClient, filePathOnServer, fileSize, fileExistsAndClientIsOwner);
            long newFilePosition = fileInfoArray[0];
            long newCounter = fileInfoArray[1];

            if(newCounter > 0 && newFilePosition > 0){
                System.out.println("Resuming upload for file: " + fileName);
                raf.seek(newCounter * bufferSize);
                count = (int) newCounter;
                filePosition = newFilePosition;
            } else {
                System.out.println("Starting a new upload for file: " + fileName);
            }

            int read = 0;
            int remaining = Math.toIntExact(fileSize);
            byte[] buffer = new byte[bufferSize];

            while((read = raf.read(buffer, 0, Math.min(buffer.length, remaining))) > 0){
                filePosition += read;
                remaining -= read;
                System.out.print(
                        "\r Uploading file..."
                        + (int)((double)(filePosition)/fileSize * 100)
                        + "%");
                remoteObj.upload(buffer, fileName, executionPathOnClient, filePathOnServer, fileSize, fileExistsAndClientIsOwner, count);
                count++;
            }

            if(filePosition >= fileSize){
                System.out.print(
                        "\r Uploading file...100%"
                );
                System.out.println("\n\n File Upload Complete");
            }
            raf.close();
        } catch(Exception e){
            System.out.println("There was an interruption when uploading file. Please retry to complete \n.");
            e.printStackTrace();
        }
    }

    private static void download(service remoteObj, String filePathOnServer, String filePathOnClient){
        try {
            boolean fileExistsOnServer = remoteObj.checkIfFileExistsOnServer(filePathOnServer);
            long fileSizeOnServer = remoteObj.getFileSize(filePathOnServer);
            int counter = 0;
            final int bufferSize = 1024;

            if(fileExistsOnServer){
                String executionPathOnClient = getExecutionPathOfCurrentClient();
                File file = new File(executionPathOnClient + File.separator + filePathOnClient);
                long fileSizeOnClient = file.length();

                //checking if user already started a download or has this file on the machine
                if(file.exists() && (file.length() < fileSizeOnServer)){
                    System.out.println("Incomplete file exists on client: " + filePathOnServer + "...Resuming download...");
                    int newCounter = (int) (fileSizeOnClient/bufferSize);
                    counter = newCounter;
                } else {
                    System.out.println("Starting new download for " + filePathOnServer + "...");
                }
                    try {
                        RandomAccessFile raf = new RandomAccessFile(file, "rw");
                        int totalCount = (int) (fileSizeOnServer/bufferSize);
                        System.out.println("Total Count: " + totalCount);

                        while(counter <= totalCount){
                            System.out.print(
                                    "\r Downloading file..."
                                            + (int) ((double) (counter) / totalCount * 100)
                                            + "%");

                            Object[] data = remoteObj.download(filePathOnServer, counter);
                            byte [] buf = (byte[]) data[0];
                            raf.seek(bufferSize * counter);
                            raf.write(buf);
                            counter++;
                        }
                        raf.close();
                    } catch(Exception e){
                        e.printStackTrace();
                    }

            } else {
                System.out.println("500 ERROR: Could not find file to download on server.");
            }

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void removeFile(service remoteObj, String filePathOnServer) throws IOException, FileNotFoundException {

        boolean fileExists = remoteObj.removeFile(filePathOnServer);

        try {

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

    private static void rmdir(service remoteObj, String filePathOnServer) throws IOException {

        try {

            System.out.println("Sending request to remove directory: " + filePathOnServer);

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
            System.out.println("Retrieving directory items for " + filePathOnServer);
            String[] list = remoteObj.listDirectoryItems(filePathOnServer);
            System.out.println("Directory items in " + filePathOnServer + ": \n\n");

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

    private static void shutdown(service remoteObj) throws IOException {
        remoteObj.shutdown();
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
}