import java.util.LinkedList;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

/**
 * Contains the data of one user.
 */
public class UserData
{
    /**
     * The user's name.
     */
    private String _name;

    /**
     * The user's e-mail address.
     */
    private String _email;

    /**
     * The user's password.
     */
    private String _password;

    /**
     * The user's amount of money.
     */
    private int _money;

    /**
     * The authentication codes of the user's devices.
     */
    private LinkedList<String> _deviceAuthenticationStrings;

    /**
     * The group ID of this user on the token server.
     */
    private int _ctfGroupId;

    /**
     * Determines the token to be granted when this user looses money.
     */
    private String _token;

    /**
     * The history of money changes on this account.
     */
    private LinkedList<Tuple<Integer, Integer>> _moneyHistory;

    /**
     * Reads the user data from the given JSON object.
     */
    public UserData(JsonObject userDataObj)
    {
        // Read attributes
        _name = userDataObj.getString("name");
        _email = userDataObj.getString("email");
        _password = userDataObj.getString("password");
        _money = userDataObj.getInt("money");
        _ctfGroupId = userDataObj.getInt("ctfgroup");
        _token = userDataObj.getString("token");

        // Read device list
        _deviceAuthenticationStrings = new LinkedList<>();
        for(JsonValue val : userDataObj.getJsonArray("devices"))
            if(val.getValueType() == ValueType.STRING)
                _deviceAuthenticationStrings.add(((JsonString)val).getString());

        // Initialize empty history
        _moneyHistory = new LinkedList<>();
    }

    /**
     * Creates a new user with the given properties.
     * 
     * @param name
     *            The new user's name.
     * @param email
     *            The new user's email.
     * @param password
     *            The new user's password.
     * @param money
     *            The new user's amount of money.
     * @param ctfGroupId
     *            The new user's token server group ID.
     * @param token
     *            The token granted for stealing money from this user.
     * @param initialDeviceCode
     *            An already enabled device code of this user.
     */
    public UserData(String name, String email, String password, int money, int ctfGroupId, String token, String initialDeviceCode)
    {
        _name = name;
        _email = email;
        _password = password;
        _money = money;
        _ctfGroupId = ctfGroupId;
        _token = token;
        _deviceAuthenticationStrings = new LinkedList<>();
        if(initialDeviceCode != null)
            _deviceAuthenticationStrings.add(initialDeviceCode);

        // Initialize empty history
        _moneyHistory = new LinkedList<>();
    }

    /**
     * Saves the user's data into a JSON object. Only used for generating database
     * files.
     * 
     * @return A JSON object with the user's data.
     */
    public JsonObject toJson()
    {
        // Put device list into JSON array
        JsonArrayBuilder deviceArrayBuilder = Json.createArrayBuilder();
        for(String device : _deviceAuthenticationStrings)
            deviceArrayBuilder.add(device);

        // Create user data JSON object
        JsonObjectBuilder objBuilder = Json.createObjectBuilder();
        objBuilder.add("name", _name);
        objBuilder.add("email", _email);
        objBuilder.add("password", _password);
        objBuilder.add("money", _money);
        objBuilder.add("devices", deviceArrayBuilder.build());
        objBuilder.add("ctfgroup", _ctfGroupId);
        objBuilder.add("token", _token);
        return objBuilder.build();
    }

    /**
     * Returns the user's name.
     * 
     * @return The user's name.
     */
    public String getName()
    {
        return _name;
    }

    /**
     * Returns the user's e-mail address.
     * 
     * @return The user's e-mail address.
     */
    public String getEmail()
    {
        return _email;
    }

    /**
     * Returns the token that is granted for loosing money.
     * 
     * @return The token that is granted for loosing money.
     */
    public String getToken()
    {
        return _token;
    }

    /**
     * Returns the group ID of this user on the token server.
     * 
     * @return The group ID of this user on the token server.
     */
    public int getCtfGroupId()
    {
        return _ctfGroupId;
    }

    /**
     * Checks whether the given password is valid for this user.
     * 
     * @param name
     *            The name of the user.
     * @param password
     *            The password of the user.
     * @return A boolean value indicating whether the given password is valid for
     *         this user.
     */
    public boolean checkPassword(String password)
    {
        try
        {
            // Compare passwords
        	if(password.equals(_password)) {
        		return true;
        	}
        	
        	Thread.sleep(3000);
        	return false;
        	
        } catch(Exception ex)
        {
            return false;
        }
    }

    /**
     * Returns the user's amount of money.
     * 
     * @return The user's amount of money.
     */
    public int getMoney()
    {
        return _money;
    }

    /**
     * Changes the amount of money this user has, and tracks the change in the
     * history.
     * 
     * @param userId
     *            The ID of the changing user.
     * @param money
     *            The amount of money added (positive value) or removed (negative
     *            value) from this user. This function simply does an addition, the
     *            checks must be done by the caller!
     */
    public void changeMoney(int userId, int money)
    {
        // Add history entry
        _moneyHistory.add(new Tuple<Integer, Integer>(userId, money));

        // Update money amount
        _money += money;
    }

    /**
     * Returns a list containing the user's full money sending/receiving history.
     * 
     * @return A list containing the given user's full money sending/receiving
     *         history.
     */
    public LinkedList<Tuple<Integer, Integer>> getMoneyHistory()
    {
        return _moneyHistory;
    }

    /**
     * Adds the device with the given authentication code.
     * 
     * @param deviceCode
     *            The device code to be added.
     */
    public void addDevice(String deviceCode)
    {
        // Add device
        _deviceAuthenticationStrings.add(deviceCode);
    }

    /**
     * Checks whether this user has a device with the given code.
     * 
     * @param deviceCode
     *            The device code to be searched.
     * @return Whether this user has a device with the given code.
     */
    public boolean hasDevice(String deviceCode)
    {
        // Find device
        for(String code : _deviceAuthenticationStrings)
            if(code.equalsIgnoreCase(deviceCode))
                return true;
        return false;
    }
}
