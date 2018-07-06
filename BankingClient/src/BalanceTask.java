import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Handles retrieval of the user's balance.
 */
public class BalanceTask extends Task
{
    /**
     * Creates a now balance retrieval task.
     * 
     * @param socketInputStream
     *            The socket input stream.
     * @param socketOutputStream
     *            The socket output stream.
     */
    public BalanceTask(DataInputStream socketInputStream, DataOutputStream socketOutputStream)
    {
        // Call superclass constructor
        super(socketInputStream, socketOutputStream);
    }

    @Override
    public void run() throws IOException
    {
        // Send request packet
        String requestPacket = "balance";
        System.out.println("Sending balance request packet...");
        Utility.sendPacket(_socketOutputStream, requestPacket);

        // Wait for response packet
        System.out.println("Waiting for balance response packet...");
        String balanceResponse = Utility.receivePacket(_socketInputStream);
        System.out.println("Server send the following balance:");
        System.out.println(balanceResponse);
    }

}
