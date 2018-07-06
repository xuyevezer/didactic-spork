import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain
{
    /**
     * Server application entry point.
     */
    public static void main(String[] args)
    {
        // Check parameters
        if(args.length < 1)
        {
            // Show usage
            Utility.safePrintln("Please provide the JSON database file name.");
            return;
        }
        
        // Generate new database?
        if(args.length >= 2 && args[1].equalsIgnoreCase("generate"))
        {
            // Create database file with given name and exit
            Database.generate(args[0]);
            Utility.safePrintln("Generating database file completed.");
            return;
        }

        // Read database
        Utility.safePrintln("Reading database file '" + args[0] + "'...");
        Database database = new Database(args[0]);

        // Create server socket
        Utility.safePrintln("Creating server socket...");
        try(ServerSocket serverSocket = new ServerSocket(database.getServerPort()))
        {
            // Listen for clients
            Utility.safePrintln("Enter client listen loop.");
            while(true)
            {
                // Accept new client
                Socket clientSocket = serverSocket.accept();
                Utility.safePrintln("Client accepted on port " + clientSocket.getLocalPort());

                // Start new thread to handle client
                new Thread(new ClientThread(clientSocket, database)).start();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}