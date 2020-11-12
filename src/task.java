import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.*;
import java.rmi.server.*;

public class task extends UnicastRemoteObject implements service, Serializable {

    public task() throws RemoteException {
        super();
    }

    private static String getExecutionPathOfCurrentClient(){
        String executionPath = null;

        try {
            executionPath = System.getProperty("user.dir");
        } catch(Exception e){
            e.printStackTrace();
        }

        return executionPath;
    }

    @Override
    public synchronized boolean createDirectory(String filePathOnServer) throws RemoteException, IOException {
        System.out.println("Where to create this dir: " + filePathOnServer);
        String executionPath = getExecutionPathOfCurrentClient();
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

    @Override
    public synchronized boolean removeFile(String existingFilePathOnServer) throws RemoteException, IOException {
        String executionPath = getExecutionPathOfCurrentClient();

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

    @Override
    public synchronized boolean removeDirectory(String existingFilePathOnServer) throws IOException {
        String executionPath = getExecutionPathOfCurrentClient();
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

    @Override
    public synchronized String[] listDirectoryItems(String existingFilePathOnServer) throws IOException {

        File serverPathDirectory = new File(existingFilePathOnServer);

        try {

            System.out.println("Retrieving directories and files within: " + existingFilePathOnServer);

        } catch(Exception e){
            e.printStackTrace();
        }

        return serverPathDirectory.list();
    }
}