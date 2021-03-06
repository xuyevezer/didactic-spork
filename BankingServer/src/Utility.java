import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
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
	private static RandomStringGenerator rndStrGen;
	
	/**
	 * Used for random numbers
	 */
	private static Random random;
	
	/**
	 * Used for creating macs
	 */	
	private static Mac mac;
	
	/**
	 * Used to get the time
	 */
	private static Calendar calendar;
	
	/**
	 * Used to keep track of messages
	 */
	private static int msgOutCounter;
	
	/**
	 * Mac key String used for generate mac Key object
	 */
	private static String rawMacKey;
	
	/**
	 * Mac key that was generated from DHKE
	 */
	private static Key macKey;
	
	/**
	 * Encryption key that was generated from DHKE
	 */
	private static String encKey;
	
	/**
	 * Init Vector for encryption
	 */
	private static String initVec;
	
	/**
	 * List of Countervalues of ingoing messages
	 */
	private static List<Integer> msgInCounter = new ArrayList<>();
	
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
    	// Make a new MAC generator
    	try {
			mac = Mac.getInstance("HmacSHA256");
			mac.init(macKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	// Get a new Timestamp
    	Timestamp timestamp = new Timestamp(calendar.getTimeInMillis());
    	
    	// Assemble data
    	// payload + timestamp + counter
    	String data = payload + "::" + timestamp.getTime() + "::" + msgOutCounter;
    	
    	// Increase Counter for outgoing messages, so it will not be use again
    	if(msgOutCounter + 1 >= Integer.MAX_VALUE) {
    		msgOutCounter = 0;
    	}
    	msgOutCounter ++;
    	
    	// Encrypt data
    	String cipher = Encryptor.encrypt(encKey, initVec, data);
    	
    	// Authenticate cipher
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
        
        String payLoad = new String(payloadEncoded);
        
        // Split package in cipher and HMAC
        String[] split = new String[2];
        String rcvCipher = "";
        String rcvHmac = "";
        
        try {
            split = payLoad.split("::");
            rcvCipher = split[0];
            rcvHmac = split[1];
        } catch(Exception e) {
        	e.printStackTrace();
        	return "Error: Bad Package.";
        }
        
        // Check HMAC
        // Make a new MAC generator
    	try {
			mac = Mac.getInstance("HmacSHA256");
			mac.init(macKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	// Authenticate cipher
    	mac.update(rcvCipher.getBytes());
    	String hmac = Base64.getEncoder().encodeToString(mac.doFinal());
    	
    	// If hmac is diffrent from the recieved one
    	if(!(hmac.equals(rcvHmac))) {
    		// Someone tamperd with the package
    		// Error
    		return "Error: Bad HMAC.";
    	}
    	
    	// Decrypt
    	String data = Encryptor.decrypt(encKey, initVec, rcvCipher);
    	
    	// Dissassamble data
    	try {
        	split = data.split("::");
    	} catch (Exception e) {
    		
    	}
    	
    	String payload = split [0];
    	Timestamp timestamp = new Timestamp(Long.parseLong(split [1]));
    	int msgCounter = Integer.parseInt(split [2]);
    	
//    	if(timestamp.compareTo(calendar.getTime()) < 0) {
//    		long diffrence = calendar.getTimeInMillis() - timestamp.getTime();
//    		if(diffrence < 300000) {
//    			// TODO: Close Input Stream
//    			return "Error: Recieved Package is too old.";
//    		}
//    	}
    	
    	// Check if messageCounter was already used
    	if(msgInCounter.contains(msgCounter)) {
    		return "Error: Package counter already used.";
    	}
    	
    	// Check if msgCounter might overflow
    	if(msgCounter + 1 >= Integer.MAX_VALUE) {
    		msgInCounter.clear();
    	}
        
    	// Add messageCounter to list
    	msgInCounter.add(msgCounter);
        
        // Decode payload
        return payload;
    }
    
    public static void sendUnencPacket(DataOutputStream outputStream, String payload) throws IOException
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
    public static String receiveUnencPacket(DataInputStream inputStream) throws IOException
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
    	setup();
    	return rndStrGen.nextAlphaNumString(length);
    }
    
    /**
     * Generates a longer key from DHKE
     * @throws Exception 
     */
    private static String generateKey(long dhke, int part) {
    	if(part < 0 || part > 1) {
    		return null;
    	}
		try {
			MessageDigest hasher = MessageDigest.getInstance("SHA-256");
	    	byte[] rawKey = hasher.digest(String.valueOf(dhke).getBytes());
	    	String strKey = Base64.getEncoder().encodeToString(rawKey);
		
    	
    	String key = "";
    	if(part == 0) {
    		if((strKey.length() % 2) == 1) {
    			key = strKey.substring(0, ((strKey.length() - 1) / 2));
    		} else {
    			key = strKey.substring(0, ((strKey.length()) / 2));
    		}
    	} else {
    		if((strKey.length() % 2) == 1) {
    			key = strKey.substring(((strKey.length() - 1) / 2), strKey.length() - 1);
    		} else {
    			key = strKey.substring(((strKey.length()) / 2), strKey.length() - 1);
    		}
    	}
    	
    	while(key.getBytes().length < 24) {
    		key += "0";
    	}
    	
    	return key;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return null;
    }
    
    public static void setup() {
    	rndStrGen = new RandomStringGenerator();
    	random = new Random();
    	calendar = Calendar.getInstance();
    	msgOutCounter = random.nextInt();
    	initVec = "1234567890123456";
    }
    
    public static void setup(long dhke) {
    	rawMacKey = generateKey(dhke, 0);
    	encKey = generateKey(dhke, 1);
    	macKey = new SecretKeySpec( rawMacKey.getBytes(), "AES");
    }
}
