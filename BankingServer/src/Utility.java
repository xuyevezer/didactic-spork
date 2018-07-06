import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Calendar;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper class containing auxiliary functions.
 */
public class Utility
{
	/**
	 * Used for generation of random strings
	 */
	private static RandomStringGenerator rndStrGen = new RandomStringGenerator();
	
	/**
	 * Used for random numbers
	 */
	private static Random random = new Random();
	
	/**
	 * Used for creating macs
	 */	
	private static Mac mac;
	
	/**
	 * Used to get the time
	 */
	private static Calendar calendar = Calendar.getInstance();
	
	/**
	 * Used to keep track of messages
	 */
	private static int msgOutCounter = random.nextInt();
	
	/**
	 * Mac key that was generated from DHKE
	 */
	private static Key macKey; //= new SecretKeySpec( key, "AES");
	
	/**
	 * Encryption key that was generated from DHKE
	 */
	private static String encKey;
	
	/**
	 * Init Vector for encryption
	 */
	private static String initVec;
	
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
    	try {
			mac = Mac.getInstance("HmacSHA256");
			mac.init(macKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	Timestamp timestamp = new Timestamp(calendar.getTimeInMillis());
    	
    	//payload + timestamp + counter
    	String data = payload + "::" + timestamp.getTime() + "::" + msgOutCounter;
    	
    	String cipher = Encryptor.encrypt(encKey, initVec, data);
    	
    	mac.update(cipher.getBytes());
    	String hmac = Base64.getEncoder().encodeToString(mac.doFinal());
    	
        // Encode payload
        byte[] payloadEncoded = (cipher + "::" + hmac).getBytes();

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
    	return rndStrGen.nextAlphaNumString(length);
    }
}
