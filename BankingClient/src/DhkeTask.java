import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Random;

/**
 * Sends login data to the server.
 */
public class DhkeTask extends Task
{
	
	private int _client_dh_secret;
	
	private BigDecimal _dh_key;
	
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
        int serverPart = Integer.parseInt(dhInfo[2]);
        System.out.println("ServerPart: " + serverPart);
        
        do
        _client_dh_secret = new Random().nextInt() % dh_modulo;
        while (_client_dh_secret <= 0);
        
        String dhkeClientPart = "" + (int)(Math.pow(dh_base, _client_dh_secret)) % dh_modulo;
        Utility.sendPacket(_socketOutputStream, dhkeClientPart);
        System.out.println("Client Part: " + dhkeClientPart);
        
        System.out.println("Modulo: " + dh_modulo);
        System.out.println("clientSecret: " + _client_dh_secret);
        System.out.println("serverPart: " + serverPart);
        _dh_key = BigDecimal.valueOf((Math.pow(serverPart, _client_dh_secret) % dh_modulo));
        System.out.println("Key: " + _dh_key);
    }
}