import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.smtp.SMTPTransport;

/**
 * Handles a client connection.
 */
public class ClientThread implements Runnable
{
    /**
     * The underlying client socket.
     */
    private Socket _clientSocket;

    /**
     * The input stream of the client socket.
     */
    private DataInputStream _clientSocketInputStream;

    /**
     * The output stream of the client socket.
     */
    private DataOutputStream _clientSocketOutputStream;

    /**
     * The database containing user data.
     */
    private Database _database;

    /**
     * The ID of the user that logged in.
     */
    private int _userId = -1;

    /**
     * True if a key has been exchanged between client and server
     */
    private boolean _keyExchanged = false;
    
    /**
     * Determines whether the client device has been authenticated.
     */
    private boolean _deviceAuthenticated = false;

    /**
     * Creates a new thread that processes the given client socket.
     * 
     * @param clientSocket
     *            The socket of the new client.
     * @param database
     *            The database containing user data.
     */
    public ClientThread(Socket clientSocket, Database database)
    {
        // Save parameters
        _clientSocket = clientSocket;
        _database = database;
    }

    /**
     * The thread entry point.
     */
    @Override
    public void run()
    {
        Utility.safePrintln("Client thread started on port " + _clientSocket.getLocalPort() + ".");
        try
        {
            // Get send and receive streams
            _clientSocketInputStream = new DataInputStream(_clientSocket.getInputStream());
            _clientSocketOutputStream = new DataOutputStream(_clientSocket.getOutputStream());

            // Repeat key exchange and login protocol until login is valid
            do {
            	dhke();
                _userId = runLogin();
            }
            while(_userId == -1 && !_keyExchanged);
            Utility.safePrintln("User " + _userId + " logged in.");

            // Run until connection is closed
            while(!_clientSocket.isClosed())
            {
                // Check for commands
                String command = Utility.receivePacket(_clientSocketInputStream);
                Utility.safePrintln("User " + _userId + " sent command '" + command + "'.");
                if(command.equals("balance"))
                    sendBalance();
                else if(command.equals("authentication"))
                {
                    // Run authentication
                    runAuthentication();
                    if(_deviceAuthenticated)
                        Utility.safePrintln("User " + _userId + " successfully authenticated.");
                }
                else if(command.equals("registration"))
                {
                    // Run registration
                    doRegistration();
                    if(_deviceAuthenticated)
                        Utility.safePrintln("User " + _userId + " successfully registered a new device (and authenticated).");
                }
                else if(command.equals("transaction"))
                {
                    // Check authentication
                    if(!_deviceAuthenticated)
                        Utility.safePrintln("User " + _userId + " requested transaction without device authentication.");
                    else
                        handleTransaction();
                }
            }
        }
        catch(EOFException e)
        {
            // Socket was closed
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            Utility.safePrintln("Doing cleanup...");
            try
            {
                // Clean up resources
                _clientSocketInputStream.close();
                _clientSocketOutputStream.close();
                _clientSocket.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            Utility.safePrintln("Cleanup complete.");
        }
    }

    /**
     * Performs a Diffie-Hellman Key Exchange with the client
     * To get a key for further encryption
     */
    public void dhke() throws IOException {
    	// Wait for dhke request
    	String dhkeRequest = Utility.receivePacket(_clientSocketInputStream);
    	
    	//Send dhke information
    	if (dhkeRequest.equals("HELO"))
    		Utility.sendPacket(_clientSocketOutputStream, _database.getDhkeMessage());
    	
    	//Wait for clients dhke part
    	String dhkeClientPart = Utility.receivePacket(_clientSocketInputStream);
    	
    	//save the generated key
    	if(Integer.parseInt(dhkeClientPart) >= 0 
    			&& Integer.parseInt(dhkeClientPart) < _database.getDhkeModulo()) {
    		_database.setDhkeKey(Integer.parseInt(dhkeClientPart));
    		_keyExchanged = true;
    	}
    }
    
    /**
     * Executes the login protocol and returns the ID of the user.
     * 
     * @return The ID of the user that logged in.
     * @throws IOException
     */
    public int runLogin() throws IOException
    {
        // Wait for login packet
        String loginRequest = Utility.receivePacket(_clientSocketInputStream);

        // Split packet
        String[] loginRequestParts = loginRequest.split(",");
        if(loginRequestParts.length < 2)
        {
            Utility.sendPacket(_clientSocketOutputStream, "Invalid login packet format.");
            return -1;
        }
        String name = loginRequestParts[0];
        String password = loginRequestParts[1];

        // Check login
        int userId = _database.verifyLogin(name, password);
        if(userId == -1)
            Utility.sendPacket(_clientSocketOutputStream, "Login invalid.");
        else
            Utility.sendPacket(_clientSocketOutputStream, "Login OK.");
        return userId;
    }

    /**
     * Sends the balance to the current user.
     * 
     * @throws IOException
     */
    public void sendBalance() throws IOException
    {
        // Build balance string
        String balance = "Amount of money: " + _database.getMoney(_userId) + "\n";
        balance += _database.getUserMoneyHistory(_userId);

        // Send balance string
        Utility.sendPacket(_clientSocketOutputStream, balance);
    }

    /**
     * Executes the authentication protocol.
     * 
     * @throws IOException
     */
    public void runAuthentication() throws IOException
    {
        // Wait for authentication packet
        String deviceCode = Utility.receivePacket(_clientSocketInputStream);

        // Check device code
        if(_database.userHasDevice(_userId, deviceCode))
        {
            // Send success message
            Utility.sendPacket(_clientSocketOutputStream, "Authentication successful.");
            _deviceAuthenticated = true;
        }
        else
            Utility.sendPacket(_clientSocketOutputStream, "Authentication failed.");
    }

    /**
     * Handles the registration of a client device for the current user.
     * 
     * @throws IOException
     */
    public void doRegistration() throws IOException
    {
        // Wait for registration ID part 1 packet
        String registrationIdPart1 = Utility.receivePacket(_clientSocketInputStream);
        if(registrationIdPart1.length() != 4)
            return;

        // Generate and send registration ID part 2 packet
        String registrationIdPart2 = Utility.getRandomString(4);
        Utility.sendPacket(_clientSocketOutputStream, registrationIdPart2);
        String registrationId = registrationIdPart1 + registrationIdPart2;
        _database.addUserDevice(_userId, registrationId);

        // Send confirmation code via e-mail or display it in server terminal
        String confirmationCode = registrationId.substring(2, 6);
        if(Utility.LAB_MODE)
        {
            try
            {
                // Create mail session
                Properties props = System.getProperties();
                props.put("mail.smtps.host", "localhost");
                props.put("mail.smtps.auth", "true");
                Session session = Session.getInstance(props, null);

                // Create message
                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress("its@its-bank"));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(_database.getUserEmail(_userId), false));
                msg.setSubject("Confirmation code");
                msg.setText("Your confirmation code: " + confirmationCode);
                msg.setHeader("X-Mailer", "ITS-BankServer");
                msg.setSentDate(new Date());

                // Send message
                SMTPTransport t = (SMTPTransport)session.getTransport("smtp");
                t.connect("localhost", "test@its-bank", "test");
                t.sendMessage(msg, msg.getAllRecipients());
                Utility.safePrintln("Confirmation code sent. [SMTP response: " + t.getLastServerResponse() + "]");
                t.close();
            }
            catch(MessagingException ex)
            {
                ex.printStackTrace();
            }
        }
        else
            Utility.safePrintln("Generated confirmation code: " + confirmationCode);

        // Wait for client confirmation code
        String clientConfirmationCode = Utility.receivePacket(_clientSocketInputStream);
        if(clientConfirmationCode.equals(confirmationCode))
        {
            // Update database, send success message
            Utility.sendPacket(_clientSocketOutputStream, "Registration successful.");
            _deviceAuthenticated = true;
        }
        else
            Utility.sendPacket(_clientSocketOutputStream, "Registration failed.");
    }

    /**
     * Handles a transaction issued by the current user.
     * 
     * @throws IOException
     */
    public void handleTransaction() throws IOException
    {
        // Wait for transaction packet
        String transactionRequest = Utility.receivePacket(_clientSocketInputStream);

        // Split packet
        String[] transactionRequestParts = transactionRequest.split(",");
        if(transactionRequestParts.length != 2)
        {
            Utility.sendPacket(_clientSocketOutputStream, "Invalid transaction packet format.");
            return;
        }
        String recipient = transactionRequestParts[0];

        // Parse and check money amount parameter
        int amount = 0;
        try
        {
            // Parse
            amount = Integer.parseInt(transactionRequestParts[1]);

            // Check range
            if(amount < 0 || amount > 10)
                amount = 10;
        }
        catch(NumberFormatException e)
        {
            Utility.sendPacket(_clientSocketOutputStream, "Invalid number format.");
            return;
        }

        // Send money
        if(_database.sendMoney(_userId, recipient, amount))
            Utility.sendPacket(_clientSocketOutputStream, "Transaction successful.");
        else
            Utility.sendPacket(_clientSocketOutputStream, "Transaction failed.");
    }
}
