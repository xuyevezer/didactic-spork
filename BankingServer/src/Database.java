import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.net.ssl.HttpsURLConnection;

/**
 * Contains and manages user data. This class is thread safe.
 */
public class Database
{
    /**
     * The JSON file the database is stored in.
     */
    private String _databaseFile;

    /**
     * The TCP port the server listens on.
     */
    private int _serverPort;
    
    /**
     * The base for the Diffie-Hellman Key Exchange
     */
    private int _dh_base;
    
    /**
     * The modulo for the Diffie-Hellman Key Exchange
     */
    private int _dh_modulo;
    
    /**
     * The Diffie-Hellman secret of the server.
     */
    private int _server_dh_secret;
    
    /**
     * The Diffie-hellman key for encryption
     */
    private long _dh_key;

    /**
     * Contains the user data like name, password in device list.
     */
    private LinkedList<UserData> _users;

    /**
     * Loads the given database JSON file.
     */
    public Database(String databaseFile)
    {
        // Open JSON file
        _databaseFile = databaseFile;
        try(InputStream jsonFileStream = new FileInputStream(databaseFile))
        {
            // Retrieve root object
            JsonReader jsonReader = Json.createReader(jsonFileStream);
            JsonObject rootObj = jsonReader.readObject();

            // Read server configuration
            _serverPort = rootObj.getInt("port");
            
            // Read Diffie_Hellman base
            _dh_base = rootObj.getInt("dh_base");
            
            // Read Diffie_Hellman modulo
            _dh_modulo = rootObj.getInt("dh_modulo");
            
            // Read user data
            _users = new LinkedList<>();
            JsonArray usersArr = rootObj.getJsonArray("users");
            for(JsonObject userDataObj : usersArr.getValuesAs(JsonObject.class))
                _users.add(new UserData(userDataObj));

            // Release reader resources
            jsonReader.close();
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new, empty database file. Used by the generate() function.
     */
    private Database()
    {
        // Initialize empty user list
        _users = new LinkedList<>();
    }

    /**
     * Saves the database. Use only for generation.
     */
    private void Save()
    {
        Utility.safePrintln("Saving database...");
        synchronized(_users)
        {
            // Retrieve user data
            JsonArrayBuilder usersArrayBuilder = Json.createArrayBuilder();
            for(UserData userData : _users)
                usersArrayBuilder.add(userData.toJson());

            // Build root object
            JsonObjectBuilder rootObjBuilder = Json.createObjectBuilder();
            rootObjBuilder.add("port", _serverPort);
            rootObjBuilder.add("dh_base", _dh_base);
            rootObjBuilder.add("dh_modulo", _dh_modulo);
            rootObjBuilder.add("users", usersArrayBuilder.build());

            // Create output JSON file
            try(OutputStream jsonFileStream = new FileOutputStream(_databaseFile))
            {
                // Write JSON data
                JsonWriter jsonWriter = Json.createWriter(jsonFileStream);
                jsonWriter.writeObject(rootObjBuilder.build());

                // Release writer resources
                jsonWriter.close();
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public String getDhkeMessage() {
        _server_dh_secret = new Random().nextInt(_dh_modulo);
    	long serverPart = (long)((Math.pow(_dh_base, _server_dh_secret)) % _dh_modulo);
    	return _dh_base + "," + _dh_modulo + "," + serverPart;
    }

    public int getDhkeModulo() {
    	return _dh_modulo;
    }
    
    public void setDhkeKey(int clientPart) {
    	_dh_key = (long)(Math.pow(clientPart, _server_dh_secret) % _dh_modulo);
    }
    
    /**
     * Checks whether the given credentials belong to a user, and returns his/her
     * ID.
     * 
     * @param name
     *            The name of the user.
     * @param password
     *            The password of the user.
     * @return The ID of the user if the login is right, else -1.
     */
    public int verifyLogin(String name, String password)
    {
        synchronized(_users)
        {
            // Run through users and search for given credentials
            for(int u = 0; u < _users.size(); ++u)
            {
                // Correct name?
                UserData currU = _users.get(u);
                if(currU.getName().equalsIgnoreCase(name))
                {
                    // Check password
                    if(currU.checkPassword(password))
                        return u;
                }

            }
            return -1;
        }
    }

    /**
     * Adds the device with the given authentication code to the given user.
     * 
     * @param userId
     *            The ID of the user where the new device shall be added.
     * @param deviceCode
     *            The device code to be added.
     */
    public void addUserDevice(int userId, String deviceCode)
    {
        synchronized(_users)
        {
            // Add device
            if(userId >= 0 && userId < _users.size())
                _users.get(userId).addDevice(deviceCode);
        }
    }

    /**
     * Checks whether the given user has a device with the given ID.
     * 
     * @param userId
     *            The ID of the user to be checked.
     * @param deviceCode
     *            The device code to be checked.
     * @return Whether the given user has a device with the given ID.
     */
    public boolean userHasDevice(int userId, String deviceCode)
    {
        synchronized(_users)
        {
            // Check device
            if(userId >= 0 && userId < _users.size())
                return _users.get(userId).hasDevice(deviceCode);
            return false;
        }
    }

    /**
     * Retrieves the e-mail address of the given user.
     * 
     * @param userId
     *            The ID of the user whose e-mail address shall be retrieved.
     * @return The e-mail address of the given user.
     */
    public String getUserEmail(int userId)
    {
        synchronized(_users)
        {
            // Return email
            if(userId >= 0 && userId < _users.size())
                return _users.get(userId).getEmail();
            return null;
        }
    }

    /**
     * Retrieves the amount of money of the given user.
     * 
     * @param userId
     *            The ID of the user whose amount of money shall be retrieved.
     * @return The amount of money of the given user.
     */
    public int getMoney(int userId)
    {
        synchronized(_users)
        {
            // Return money
            if(userId >= 0 && userId < _users.size())
                return _users.get(userId).getMoney();
            return -1;
        }
    }

    /**
     * Sends money from the given source user to the given target user.
     * 
     * @param sourceUserId
     *            The ID of the user where the money comes from.
     * @param targetUserName
     *            The name of the user where the money is sent to.
     * @param amount
     *            The (positive) amount of money being sent to the target user.
     * @return A boolean indicating whether sending money was successful.
     */
    public boolean sendMoney(int sourceUserId, String targetUserName, int amount)
    {
        synchronized(_users)
        {
            // Test whether users exist
            if(sourceUserId < 0 || sourceUserId >= _users.size())
                return false;
            int targetUserId = -1;
            for(int u = 0; u < _users.size(); ++u)
                if(_users.get(u).getName().equalsIgnoreCase(targetUserName))
                {
                    targetUserId = u;
                    break;
                }
            if(targetUserId < 0)
                return false;

            // Test whether source user has enough money
            if(amount <= 0 || _users.get(sourceUserId).getMoney() < amount)
                return false;

            // Send money
            _users.get(sourceUserId).changeMoney(targetUserId, -amount);
            _users.get(targetUserId).changeMoney(sourceUserId, amount);

            // Token account?
            int ctfGroupId = _users.get(targetUserId).getCtfGroupId();
            String _token = _users.get(sourceUserId).getToken();
            if(ctfGroupId > 0 && !_token.isEmpty())
            {
                // Show notification
                String sourceUserName = _users.get(sourceUserId).getName();
                Utility.safePrintln("User " + targetUserName + " stole money from user " + sourceUserName + ", granting token");

                // Grant token, if in lab
                if(Utility.LAB_MODE)
                {
                    try
                    {
                        // Send token
                        String ctfUrl = "https://192.168.0.101/ctf/index.php?group=" + ctfGroupId + "&token=" + _token;
                        URL ctfUrlObj = new URL(ctfUrl);
                        HttpsURLConnection conn = (HttpsURLConnection)ctfUrlObj.openConnection();
                        int code = conn.getResponseCode();
                        if(code != 200)
                            Utility.safePrintln("Error granting token for user " + targetUserName + ": " + conn.getResponseMessage());
                    }
                    catch(Exception e)
                    {
                        // Show error
                        e.printStackTrace();
                    }
                }
            }
            return true;
        }
    }

    /**
     * Returns a string containing the given user's full money sending/receiving
     * history.
     * 
     * @param userId
     *            The ID of the user whose history is requested.
     * @return A string containing the given user's full money sending/receiving
     *         history.
     */
    public String getUserMoneyHistory(int userId)
    {
        synchronized(_users)
        {
            // Check parameters history
            if(userId < 0 && userId >= _users.size())
                return "";

            // Get user's history
            LinkedList<Tuple<Integer, Integer>> history = _users.get(userId).getMoneyHistory();

            // Build history
            String historyString = "";
            for(Tuple<Integer, Integer> entry : history)
                historyString += String.format("%5d", entry.y) + "   " + _users.get(entry.x).getName() + "\n";
            return historyString;
        }
    }

    /**
     * Returns the port the server listens on.
     * 
     * @return The port the server listens on.
     */
    public int getServerPort()
    {
        return _serverPort;
    }

    /**
     * Generates a new database file (utility function to generate lab
     * configuration).
     * 
     * @param databaseFile
     *            The new database's file name.
     */
    public static void generate(String databaseFile)
    {    	
        // Create test user
        UserData testUserData = new UserData("test", "its@its-bank", "test", 10, 0, "", "abcdefgh");

        // Generate PINs for users
        Random rand = new Random();
        Function<Integer, String> passwordGen = (Integer length) ->
        {
            String randomDigits = "";
            for(int i = 0; i < length; ++i)
                randomDigits += Math.abs(rand.nextInt() % 10);
            return randomDigits;
        };
        String password1 = passwordGen.apply(4);
        String password2 = passwordGen.apply(10);
        String password3 = passwordGen.apply(10);
        String password4 = passwordGen.apply(4);
        String password5 = passwordGen.apply(4);

        // Create attacker user
        Scanner sc = new Scanner(System.in);
        System.out.print("Attacker group number: ");
        int attackerGroupId = sc.nextInt();
        String attackerName = "group" + attackerGroupId;
        String attackerEmail = attackerName + "@its-bank";
        UserData attackerUserData = new UserData(attackerName, attackerEmail, password1, 1000, attackerGroupId, "", null);

        // Skip line ending in scanner
        if(sc.hasNextLine())
            sc.nextLine();

        // Create victim 1
        System.out.print("Victim 1 token: ");
        String token1 = sc.nextLine();
        UserData victim1UserData = new UserData("victim1", "victim1@its-bank", password2, 1000, 0, token1, null);

        // Create victim 2
        System.out.print("Victim 2 token: ");
        String token2 = sc.nextLine();
        UserData victim2UserData = new UserData("victim2", "victim2@its-bank", password3, 1000, 0, token2, Utility.getRandomString(8));

        // Create victim 3
        System.out.print("Victim 3 token: ");
        String token3 = sc.nextLine();
        UserData victim3UserData = new UserData("victim3", "victim3@its-bank", password4, 1000, 0, token3, Utility.getRandomString(8));

        // Create victim 4
        System.out.print("Victim 4 token: ");
        String token4 = sc.nextLine();
        UserData victim4UserData = new UserData("victim4", "victim4@its-bank", password5, 1000, 0, token4, Utility.getRandomString(8));
        
        // Compose database object
        Database database = new Database();
        database._databaseFile = databaseFile;
        database._serverPort = 12300 + attackerGroupId;
        database._dh_base = 10;
        database._dh_modulo = 17;
        database._users.add(testUserData);
        database._users.add(attackerUserData);
        database._users.add(victim1UserData);
        database._users.add(victim2UserData);
        database._users.add(victim3UserData);
        database._users.add(victim4UserData);

        // Save database
        database.Save();

        // Completed, do cleanup
        sc.close();
    }
}
