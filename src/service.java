import java.io.File;
import java.io.IOException;
import java.rmi.*;

public interface service extends Remote
{
    void shutdown() throws RemoteException, IOException;
    boolean createDirectory(String filePathOnServer) throws RemoteException, IOException;
    boolean removeFile(String existingFilePathOnServer) throws RemoteException, IOException;
    boolean removeDirectory(String existingFilePathOnServer) throws RemoteException, IOException;
    String[] listDirectoryItems(String filePathOnServer) throws RemoteException, IOException;
}