import java.io.IOException;
import java.rmi.*;

public interface service extends Remote {

    boolean handleFileCheck(String clientName, String filePathOnServer) throws RemoteException, IOException, ClassNotFoundException;

    long[] handlePrepareUpload(String fileName, String clientName, String filePathOnServer, long fileSize, boolean fileExistsAndClientIsOwner) throws RemoteException, IOException, ClassNotFoundException;

    boolean upload(byte[] buffer, String fileName, String clientName, String filePathOnServer, long fileSize, boolean fileExistsAndClientIsOwner, int count) throws RemoteException, IOException;

    Object[] download(String filePathOnServer, int counter) throws RemoteException, IOException;

    boolean checkIfFileExistsOnServer(String filePathOnServer) throws RemoteException, IOException;

    void shutdown() throws RemoteException, IOException;

    boolean createDirectory(String filePathOnServer) throws RemoteException, IOException;

    boolean removeFile(String existingFilePathOnServer) throws RemoteException, IOException;

    boolean removeDirectory(String existingFilePathOnServer) throws RemoteException, IOException;

    Object[] listDirectoryItems(String filePathOnServer) throws RemoteException, IOException;

    long getFileSize(String filePathOnServer) throws RemoteException, IOException;
}