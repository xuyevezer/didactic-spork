import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Helper class containing auxiliary functions.
 */
public class Utility
{
    /**
     * Controls whether we are in lab or testing mode. Lab mode enables
     * communication features like sending mails and tokens.
     */
    public final static boolean LAB_MODE = false;

    /**
     * Prints out the given message, while synchronizing between threads.
     * 
     * @param message
     *            The message to be printed.
     */
    public static void safePrintln(String message)
    {
        synchronized(System.out)
        {
            System.out.println(message);
        }
    }

    /**
     * Writes the given payload as a packet into the given output stream.
     * 
     * @param outputStream
     *            The stream the packet shall be written to.
     * @param payload
     *            The string payload to be sended.
     * @throws IOException
     */
    public static void sendPacket(DataOutputStream outputStream, String payload) throws IOException
    {
        // Encode payload
        byte[] payloadEncoded = payload.getBytes();

        // Write packet length
        outputStream.writeInt(payloadEncoded.length);

        // Write payload
        outputStream.write(payloadEncoded);
    }

    /**
     * Receives the next packet from the given input stream.
     * 
     * @param inputStream
     *            The stream where the packet shall be retrieved.
     * @return The payload of the received packet.
     * @throws IOException
     */
    public static String receivePacket(DataInputStream inputStream) throws IOException
    {
        // Prepare payload buffer
        byte[] payloadEncoded = new byte[inputStream.readInt()];
        inputStream.readFully(payloadEncoded);

        // Decode payload
        return new String(payloadEncoded);
    }

    /**
     * Returns a random alpha numeric string with the given length.
     * 
     * @param length
     *            The length of the requested string.
     * @return A random alpha numeric string with the given length.
     */
    public static String getRandomString(int length)
    {
        // Generate random string efficiently
        int randomIndex = new Random().nextInt(100 - length);
        return "Sl4idafEVk9X1efZFSAUANyQefaua8JnnAVVQbhuEwrcA4c85yrMaaVjv1TiDbmPdQAD5pfyqcsj1obyEJxGulmaV8ezWYEXpyUs".substring(randomIndex, randomIndex + length);
    }
}
