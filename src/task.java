import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Map;

public class task extends UnicastRemoteObject implements service, Serializable {

    public task() throws RemoteException {
        super();
    }

    private static String getExecutionPathOfServer(){

        String executionPath = null;

        try {

            executionPath = System.getProperty("user.dir");

        } catch(Exception e){

            e.printStackTrace();
        }

        return executionPath;
    }

    private synchronized boolean checkIfFileStorageExists(){

        String executionPath = System.getProperty("user.dir");

        boolean exists;

        File file = new File(executionPath + File.separator + "unfinishedFiles.txt");

        exists = file.exists();

        System.out.println("File exists in storage:  " + exists);

        return exists;
    }

    private void createStorageFile() throws IOException {

        String serverExecutionPath = null;

        try {

            serverExecutionPath = System.getProperty("user.dir");

            System.out.print("Executing at => " + serverExecutionPath.replace("\\", "/"));

        } catch(Exception e){

            e.printStackTrace();
        }

        File storageFile = new File(serverExecutionPath + File.separator + "unfinishedFiles.txt");

        boolean fileCreated = storageFile.createNewFile();

        try {

            FileOutputStream fos = new FileOutputStream(storageFile);

            ObjectOutputStream oos = new ObjectOutputStream(fos);

            if (fileCreated) {

                System.out.println("Storage file created: " + storageFile.getName());

                //create new HashMap and write to text file
                HashMap<String, String> map = new HashMap<>();

                oos.writeObject(map);
            }

            fos.close();

            oos.close();


        } catch (IOException e) {

            System.out.println("An error occurred trying to create storage file: ");

            e.printStackTrace();
        }
    }

    private void removeFromHashMap(String filePath, String clientName) throws FileNotFoundException {

        String executionPath = System.getProperty("user.dir");

        File storageFile = new File(executionPath + File.separator + "unfinishedFiles.txt");

        try  {
            FileInputStream fis = new FileInputStream(storageFile);

            ObjectInputStream ois = new ObjectInputStream(fis);

            @SuppressWarnings("unchecked")
            HashMap<String, String> map = (HashMap<String, String>) ois.readObject();

            String fullPath = executionPath + File.separator + filePath;

            map.remove(fullPath, clientName);

            ois.close();

            fis.close();

            System.out.println("Deleted " + fullPath + " | " + clientName + " from storage" + "\n");

            System.out.println("UNFINISHED FILES LIST AFTER DELETE: " + "\n");

            for(Map.Entry<String,String> m : map.entrySet()){

                System.out.println(m.getKey()+" : "+m.getValue());
            }

            FileOutputStream fos = new FileOutputStream(storageFile);

            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(map);

            fos.close();

            oos.close();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void updateHashMap(String filePathOnServer, String clientName) throws IOException, ClassNotFoundException  {

        String executionPath = System.getProperty("user.dir");

        File storageFile = new File(executionPath + File.separator + "unfinishedFiles.txt");

        try  {

            FileInputStream fis = new FileInputStream(storageFile);

            ObjectInputStream ois = new ObjectInputStream(fis);

            @SuppressWarnings("unchecked")
            HashMap<String, String> map = (HashMap<String, String>) ois.readObject();

            String fullPath = executionPath + File.separator + filePathOnServer;

            map.put(fullPath, clientName);

            ois.close();

            fis.close();

            System.out.println("Added " + fullPath + " | " + clientName + " to storage");

            System.out.println("UNFINISHED FILES LIST: " + "\n");

            for(Map.Entry<String,String> m : map.entrySet()){

                System.out.println(m.getKey()+" : "+m.getValue());
            }

            System.out.println("\n");

            FileOutputStream fos = new FileOutputStream(storageFile);

            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(map);

            fos.close();

            oos.close();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private boolean searchForUnfinishedFileInStorage(String filePath, String clientName) throws IOException, ClassNotFoundException {

        String executionPath = System.getProperty("user.dir");

        File storageFile = new File(executionPath + File.separator + "unfinishedFiles.txt");

        boolean unfinishedFileExistsForCurrentClient = false;

        String fullPath = executionPath + File.separator + filePath;

        FileInputStream fis = new FileInputStream(storageFile);

        ObjectInputStream ois = new ObjectInputStream(fis);

        try {

            if (storageFile.exists()) {

                System.out.println("Storage file exists...CHECK IF FILE EXISTS");

                @SuppressWarnings("unchecked")
                HashMap<String, String> hashmap = (HashMap<String, String>) ois.readObject();

                if (hashmap.containsKey(fullPath)) {

                    System.out.println("This path has an unfinished upload on server...Checking if client is owner...");

                    String client = hashmap.get(fullPath);

                    System.out.println("Client value: " + client + " ||| Client name: " + clientName);

                    if(client.equalsIgnoreCase(clientName)){

                        System.out.println("FileName does exist in hashmap and client matches...");

                        unfinishedFileExistsForCurrentClient = true;

                        System.out.println("UNFINISHED FILES LIST: " + "\n");

                        for(Map.Entry<String,String> m : hashmap.entrySet()){

                            System.out.println(m.getKey()+" : "+m.getValue());

                        }

                    } else {

                        System.out.println("Same file path but different client uploading. Replacing file with new upload from this client...");

                        unfinishedFileExistsForCurrentClient = false;

                        removeFromHashMap(filePath, clientName);
                    }

                } else {

                    System.out.println("FileName does not exist in hashmap");

                    unfinishedFileExistsForCurrentClient = false;
                }
            }

        } catch (Exception e) {

            System.out.println("There was an error finding the storage file: " +  "\n");

            e.printStackTrace();
        }

        return unfinishedFileExistsForCurrentClient;
    }

    public synchronized boolean handleFileCheck(String clientName, String filePathOnServer) throws IOException, ClassNotFoundException {

        String executionPath = System.getProperty("user.dir");

        String path = executionPath + File.separator + filePathOnServer;

        System.out.println("Now checking if storage file exists: " + path);

        boolean storageFileExists = checkIfFileStorageExists();

        if(!storageFileExists){

            System.out.println("No storage file...creating...");

            createStorageFile();
        }

        return searchForUnfinishedFileInStorage(filePathOnServer, clientName);
    }

    public synchronized long[] handlePrepareUpload(String fileName, String clientName, String filePathOnServer, long fileSize, boolean fileExistsAndClientIsOwner) throws IOException, ClassNotFoundException {

        long counter = 0;

        long position = 0;

        long[]fileInfoArray = new long[2];

        fileInfoArray[0] = counter;

        fileInfoArray[1] = position;

        int bufferSize = 1024;

        String executionPath = System.getProperty("user.dir");

        String path = executionPath + File.separator + filePathOnServer;

        File file = new File(path);

        if(!fileExistsAndClientIsOwner){

            System.out.println("Adding new file to hashmap in case of network, server or client crash...");

            //add entry into hash map with new client to upload new file or replace file
            updateHashMap(filePathOnServer, clientName);

        } else {

            System.out.println("You are owner of this unfinished file. Sending counter position back to client to resume upload...");

            long filePos = file.length();

            fileInfoArray[0] = filePos;

            counter = (filePos / bufferSize);

            fileInfoArray[1] = counter;

            System.out.println("Current position of file on server: " + filePos);

            System.out.println("Current counter of file on server: " + counter);
        }

        return fileInfoArray;
    }

    public synchronized boolean upload(byte[] buffer, String fileName, String clientName, String filePathOnServer, long fileSize, boolean fileExistsAndClientIsOwner, int count) throws RemoteException, IOException {

        String executionPath = System.getProperty("user.dir");

        File file = new File(executionPath + File.separator + filePathOnServer);

        RandomAccessFile raf = new RandomAccessFile(file, "rw");

        final int bufferSize = 1024;

        try {

            raf.seek(bufferSize * count);

            //TODO: how to determine if write was completed to send back if failure occurs on server.
            raf.write(buffer);

            if(file.length() >= fileSize){

                System.out.println("File Upload Complete");

                //remove from hashmap since the file completed
                removeFromHashMap(filePathOnServer, clientName);

            }

        } catch (Exception e) {

            System.out.println("\n Something went wrong as the client was uploading a file.");

            e.printStackTrace();
        }

        raf.close();

        return true;
    }

    public synchronized Object[] download(String filePathOnServer, int counter) throws FileNotFoundException {

        String executionPathOnServer = getExecutionPathOfServer();

        File file = new File(executionPathOnServer + File.separator + filePathOnServer);

        RandomAccessFile raf = new RandomAccessFile(file, "rw");

        long fileSize = file.length();

        int count = 0;

        final int bufferSize = 1024;

        Object[] data = new Object[2];

        try {

            byte[] buffer = new byte[bufferSize];

            raf.seek(bufferSize * counter);

            raf.read(buffer,0,bufferSize);

            data[0] = buffer;

            data[1] = count;

            raf.close();

        } catch(Exception e){

            System.out.println("There was an interruption when uploading file. Please retry to complete \n.");

            e.printStackTrace();
        }

        return data;
    }

    public long getFileSize(String filePathOnServer){
        String executionPath = getExecutionPathOfServer();
        File file = new File(executionPath + File.separator + filePathOnServer);

        return file.length();
    }

    public synchronized boolean checkIfFileExistsOnServer(String filePathOnServer){
        String executionPath = getExecutionPathOfServer();
        File file = new File(executionPath + File.separator + filePathOnServer);
        boolean doesExistOnServer = false;

        try {
            if(file.exists()){
                doesExistOnServer = true;
            }
        } catch(Exception e){
            e.printStackTrace();
        }

        return doesExistOnServer;
    }

    public synchronized boolean createDirectory(String filePathOnServer) throws RemoteException, IOException {
        System.out.println("Where to create this dir: " + filePathOnServer);
        String executionPath = getExecutionPathOfServer();
        boolean wasCreated = false;

        try {

            File dir = new File(executionPath + File.separator + filePathOnServer);

            if(!dir.exists()){
                boolean dirWasCreated = dir.mkdir();

                if(dirWasCreated){
                    wasCreated = true;
                    System.out.println("***Directory created successfully on SERVER...");
                }else{
                    System.out.println("Sorry could not create specified directory");
                }
            } else {
                System.out.println("ERROR: This directory already exists. Please try again...");
            }

        } catch(Exception e){
            e.printStackTrace();
        }

        return wasCreated;
    }

    public synchronized boolean removeFile(String existingFilePathOnServer) throws RemoteException, IOException {
        String executionPath = getExecutionPathOfServer();

        boolean wasRemoved = false;

        File file = new File(executionPath + File.separator + existingFilePathOnServer);

        try {

            if(file.exists()){

                if(file.delete()){

                    wasRemoved = true;

                    System.out.println("File deleted: " + file.getAbsolutePath());
                }

            } else {

                System.out.println("There was an error. No such file exists.");
            }
        } catch(Exception e){

            e.printStackTrace();
        }

        return wasRemoved;
    }

    public synchronized boolean removeDirectory(String existingFilePathOnServer) throws RemoteException, IOException {
        String executionPath = getExecutionPathOfServer();
        boolean wasRemoved = false;

        try{
            File file = new File(executionPath + File.separator + existingFilePathOnServer);

            if (file.isDirectory()) {
                String[] list = file.list();
                if (list != null && list.length == 0) {
                    System.out.println("Directory is empty! Deleting...");
                    if(file.delete()){
                        wasRemoved = true;
                    }
                } else {
                    System.out.println("Directory is not empty! Unable to delete.");
                    wasRemoved = false;
                }
            } else {
                System.out.println("This directory does not exist. Please try again...");
                wasRemoved = false;
            }
        } catch(Exception e){
            e.printStackTrace();
        }

        return wasRemoved;
    }

    public synchronized String[] listDirectoryItems(String existingFilePathOnServer) throws RemoteException, IOException {

        File serverPathDirectory = new File(existingFilePathOnServer);

        try {

            System.out.println("Retrieving directories and files within: " + existingFilePathOnServer);

        } catch(Exception e){
            e.printStackTrace();
        }

        return serverPathDirectory.list();
    }

    public synchronized void shutdown()throws RemoteException, IOException {
        System.out.println("Terminating program...goodbye.");
        System.exit(0);
    }
}