import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class server {

    protected server() throws RemoteException {
        super();
    }

    public static void main(String[] args) throws IOException {
        boolean errorAtStart = args.length != 2;
        System.out.println("*** Starting File System ***");
        String serverPortNumber = args[1];
        int port = Integer.parseInt(serverPortNumber);

        if (errorAtStart) {
            System.out.println("ERROR: You must enter two arguments - 'start' and a port number" + "\n");
            System.exit(1);

        } else {
            if (args[0].equalsIgnoreCase("start") && port == 8000) {

                try {
                    task remoteObject = new task();
                    Registry registry = LocateRegistry.createRegistry(port);
                    registry.rebind("remoteObject", remoteObject);
                    System.err.println("Server added to registry on port " + port + " and listening to requests...");

                } catch (Exception e) {
                    System.out.println("Server failed to start: " + "\n\n");
                    e.printStackTrace();
                }

            } else {
                System.out.println("ERROR: Entered wrong arguments (command: start, port: 8000). Please try again" + "\n");
                System.exit(1);
            }
        }
    }
}