import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Sends login data to the server.
 */
public class DhkeTask extends Task
{
	
	private int _client_dh_secret;
	
	private long _dh_key;
	
    /**
     * Creates a new dhke task.
     * 
     * @param socketInputStream
     *            The socket input stream.
     * @param socketOutputStream
     *            The socket output stream.
     */
    public DhkeTask(DataInputStream socketInputStream, DataOutputStream socketOutputStream)
    {
        // Call superclass constructor
        super(socketInputStream, socketOutputStream);
    }

    /**
     * Executes the login.
     * 
     * @throws IOException
     */
    public void run() throws IOException
    {
        // Send request packet
        String dhRequestPacket = "HELO";
        Utility.sendPacket(_socketOutputStream, dhRequestPacket);

        // Wait for response packet
        String dhResponse = Utility.receivePacket(_socketInputStream);
        
        String[] dhInfo = dhResponse.split(",");
        int dh_base = Integer.parseInt(dhInfo[0]);
        int dh_modulo = Integer.parseInt(dhInfo[1]);
        long serverPart = Long.parseLong(dhInfo[2]);
        
        do
        _client_dh_secret = new Random().nextInt() % dh_modulo;
        while (_client_dh_secret <= 0);
        
        String dhkeClientPart = "" + (long)(Math.pow(dh_base, _client_dh_secret)) % dh_modulo;
        Utility.sendPacket(_socketOutputStream, dhkeClientPart);
        _dh_key = (long)((Math.pow(serverPart, _client_dh_secret) % dh_modulo));
    }
}