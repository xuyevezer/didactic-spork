import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Registers the current device on the server.
 */
public class RegistrationTask extends Task
{
    /**
     * Tells whether the transaction was successful.
     */
    private boolean _successful = false;

    /**
     * A scanner object to read terminal input.
     */
    private Scanner _terminalScanner;

    /**
     * Creates a registration task.
     * 
     * @param socketInputStream
     *            The socket input stream.
     * @param socketOutputStream
     *            The socket output stream.
     * @param terminalScanner
     *            A scanner object to read terminal input.
     */
    public RegistrationTask(DataInputStream socketInputStream, DataOutputStream socketOutputStream, Scanner terminalScanner)
    {
        // Call superclass constructor
        super(socketInputStream, socketOutputStream);

        // Save parameters
        _terminalScanner = terminalScanner;
    }

    /**
     * Executes the registration.
     * 
     * @throws IOException
     */
    public void run() throws IOException
    {
        // Check whether an device code has been generated in the past
        final String deviceCodeFilename = "banking_device.txt";
        if(new File(deviceCodeFilename).exists())
        {
            // Read device authentication code from file
            System.out.println("Authentication file detected, reading device code...");
            String authenticationCode = "";
            try(Scanner deviceCodeFileScanner = new Scanner(new FileReader(deviceCodeFilename)))
            {
                authenticationCode = deviceCodeFileScanner.next();
            }
            catch(IOException e)
            {
                e.printStackTrace();
                return;
            }

            // Inform server about authentication
            String prePacket = "authentication";
            System.out.println("Sending authentication header packet...");
            Utility.sendPacket(_socketOutputStream, prePacket);

            // Send authentication code
            System.out.println("Sending authentication code...");
            Utility.sendPacket(_socketOutputStream, authenticationCode);

            // Wait for confirmation by server
            System.out.println("Waiting for server confirmation...");
            String serverConfirmation = Utility.receivePacket(_socketInputStream);
            System.out.println("Server response: " + serverConfirmation);
            if(!serverConfirmation.equals("Authentication successful."))
            {
                // Show error
                System.out.println("Authentication failed. Maybe the device code file is too old or invalid?");
                return;
            }
        }
        else
        {
            // Inform server about registration
            String prePacket = "registration";
            System.out.println("Sending registration header packet...");
            Utility.sendPacket(_socketOutputStream, prePacket);

            // Generate half of registration code
            System.out.println("Generating and sending registration code part 1/2...");
            String registrationCodePart1 = Utility.getRandomString(4);
            Utility.sendPacket(_socketOutputStream, registrationCodePart1);

            // Receive other half of registration code from server
            System.out.println("Waiting for registration code part 2/2...");
            String registrationCodePart2 = Utility.receivePacket(_socketInputStream);
            if(registrationCodePart2.length() != 4)
            {
                // Output response and stop registration process
                System.out.println("Received invalid registration code part from server: " + registrationCodePart2);
                return;
            }
            String registrationCode = registrationCodePart1 + registrationCodePart2;
            System.out.println("Received full registration code.");

            // Read confirmation code that the server should have sent via email
            System.out.print("Confirmation code (check your email): ");
            String confirmationCode = _terminalScanner.next();

            // Send confirmation code
            System.out.println("Sending confirmation code...");
            Utility.sendPacket(_socketOutputStream, confirmationCode);

            // Wait for confirmation by server
            System.out.println("Waiting for server confirmation...");
            String serverConfirmation = Utility.receivePacket(_socketInputStream);
            System.out.println("Server response: " + serverConfirmation);
            if(!serverConfirmation.equals("Registration successful."))
                return;

            // Save registration code
            try(FileWriter deviceCodeFileWriter = new FileWriter(deviceCodeFilename))
            {
                deviceCodeFileWriter.write(registrationCode);
            }
            catch(IOException e)
            {
                e.printStackTrace();
                return;
            }
        }
        _successful = true;
    }

    /**
     * Returns whether the registration was successful.
     * 
     * @return Whether the registration was successful.
     */
    public boolean getSuccessful()
    {
        return _successful;
    }
}