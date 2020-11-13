import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class server {

    protected server(String name) throws RemoteException {

        super();

        System.out.println(name + " server created");

    }

    public static void main(String[] args) throws IOException {

        boolean errorAtStart = args.length != 2;

        System.out.println("*** Starting File System ***");

        //        if (System.getSecurityManager() == null) {
//
//            System.setSecurityManager(new SecurityManager());
//
//        }

        try {

            String serverPortNumber = args[1];

            //new socket creation here
            Socket clientSocket = null;

            int port = Integer.parseInt(serverPortNumber);

            ServerSocket serverSocket = new ServerSocket(port);

            if (errorAtStart) {

                System.out.println("ERROR: You must enter two arguments - 'start' and a port number" + "\n");

                System.exit(1);
            }

            if (args[0].equalsIgnoreCase("start") && port == 8000) {

                clientSocket = serverSocket.accept();

                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());

                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

                task remoteObject = new task(clientSocket, dis, dos);

                Registry registry = LocateRegistry.createRegistry(port);

                registry.rebind("remoteObject", remoteObject);

                System.err.println("Server added to registry and listening on port " + port);

            } else {

                System.out.println("ERROR: Entered wrong arguments (command: start, port: 8000). Please try again" + "\n");

                System.exit(1);
            }

        } catch(RemoteException e) {

            System.err.println("Server failed to register and start...");

            e.printStackTrace();
        }
    }
}