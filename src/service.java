import java.io.IOException;
import java.rmi.*;

public interface service extends Remote {

    boolean handleFileCheck(String fileName, String clientName, String filePathOnServer, long fileSize) throws RemoteException, IOException, ClassNotFoundException;

    long handlePrepareUpload(String fileName, String clientName, String filePathOnServer, long fileSize, boolean fileExistsAndClientIsOwner) throws RemoteException, IOException, ClassNotFoundException;

    boolean upload(byte[] buffer, String fileName, String clientName, String filePathOnServer, long fileSize, boolean fileExistsAndClientIsOwner) throws RemoteException, IOException;

    void shutdown() throws RemoteException, IOException;

    boolean createDirectory(String filePathOnServer) throws RemoteException, IOException;

    boolean removeFile(String existingFilePathOnServer) throws RemoteException, IOException;

    boolean removeDirectory(String existingFilePathOnServer) throws RemoteException, IOException;

    String[] listDirectoryItems(String filePathOnServer) throws RemoteException, IOException;
}