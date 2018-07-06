import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 * Sends login data to the server.
 */
public class LoginTask extends Task
{
    /**
     * Tells whether the login was successful.
     */
    private boolean _successful = false;

    /**
     * A scanner object to read terminal input.
     */
    private Scanner _terminalScanner;

    /**
     * Creates a new login task.
     * 
     * @param socketInputStream
     *            The socket input stream.
     * @param socketOutputStream
     *            The socket output stream.
     * @param terminalScanner
     *            A scanner object to read terminal input.
     */
    public LoginTask(DataInputStream socketInputStream, DataOutputStream socketOutputStream, Scanner terminalScanner)
    {
        // Call superclass constructor
        super(socketInputStream, socketOutputStream);

        // Save parameters
        _terminalScanner = terminalScanner;
    }

    /**
     * Executes the login.
     * 
     * @throws IOException
     */
    public void run() throws IOException
    {
        // Read credentials
        String name;
        String password;
        System.out.print("User: ");
        name = _terminalScanner.next();
        System.out.print("Password: ");
        password = _terminalScanner.next();

        // Send login packet
        String loginPacket = name + "," + password;
        Utility.sendPacket(_socketOutputStream, loginPacket);

        // Wait for response packet
        String loginResponse = Utility.receivePacket(_socketInputStream);
        System.out.println("Server response: " + loginResponse);
        _successful = loginResponse.equals("Login OK.");
    }

    /**
     * Returns whether the login was successful.
     * 
     * @return Whether the login was successful.
     */
    public boolean getSuccessful()
    {
        return _successful;
    }
}
