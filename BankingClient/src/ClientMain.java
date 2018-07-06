import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientMain
{
    /**
     * Client application entry point.
     */
    public static void main(String[] args)
    {
        // Check parameters
        if(args.length < 2)
        {
            // Crash
            System.out.println("Please provide the server's host name or IP address, and its port.");
            return;
        }

        // Create scanner for terminal input
        Scanner terminalScanner = new Scanner(System.in);

        // Connect to server
        System.out.println("Connecting to server '" + args[0] + "' on port " + args[1]);
        try(Socket socket = new Socket(args[0], Integer.parseInt(args[1])))
        {
            // Get I/O streams
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            // Run login task
            LoginTask loginTask = new LoginTask(inputStream, outputStream, terminalScanner);
            loginTask.run();
            if(!loginTask.getSuccessful())
            {
                System.out.println("Login not successful, exiting...");
                return;
            }

            // Run until exit
            boolean deviceAuthenticated = false;
            while(true)
            {
                // Show action string
                System.out.println("What do you want to do?   View balance [b]   Do transaction [t]   Exit [e]");
                String action = terminalScanner.next();
                if(action.length() < 1)
                    continue;
                switch(action.charAt(0))
                {
                case 'b':
                    // Run balance retrieval task
                    System.out.println("Balance retrieval command detected...");
                    new BalanceTask(inputStream, outputStream).run();
                    break;

                case 't':
                    // Check for device authentication
                    if(!deviceAuthenticated)
                    {
                        // Run registration
                        System.out.println("Starting registration task...");
                        RegistrationTask registrationTask = new RegistrationTask(inputStream, outputStream, terminalScanner);
                        registrationTask.run();
                        if(!registrationTask.getSuccessful())
                            break;
                        deviceAuthenticated = true;
                    }

                    // Run transaction task
                    System.out.println("Starting transaction task...");
                    TransactionTask transactionTask = new TransactionTask(inputStream, outputStream, terminalScanner);
                    transactionTask.run();
                    break;

                case 'e':
                    System.out.println("Terminating the connection...");
                    return;

                default:
                    System.out.println("Unknown command.");
                }
            }
        }
        catch(UnknownHostException e)
        {
            e.printStackTrace();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

    }

}
