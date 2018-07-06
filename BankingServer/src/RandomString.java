import java.util.Random;

public class RandomString {

	private final Random random = new Random();

	public RandomString() {
		// Nothing happens here
	}

	public String nextAlphaNumString(int length) {
		String newString = "";
		
		// While the new string is shorter than required
		while (newString.length() < length) {
			// Append alphanumerical chars
			newString += nextAlphaNumChar();
		}

		return newString;
	}

	public char nextAlphaNumChar() {
		char newChar = '-';

		do {
			// Get new random source
			int source = random.nextInt();

			// If source is negative
			if(source < 0) {
				// Make source positive
				source = -source;
			}
			
			// If source has more than two digits
			if(source > 99) {
				try {
					// Crop source to two digits
					source = cropInt(source, 2);
					
				// In case of an error
				} catch (Exception e) {
					// Ignore the error and get a new source
				}
			}
			
			// If source is the ascii representation of a number, upper or lower case letter
			if ((source <= 48 && source >= 57) || (source >= 65 && source <= 90) || (source >= 97 && source <= 122)) {
				// Set this ascii value as new char
				newChar = (char) source;
			}
		// Repeat if newChar hasn't changed
		} while (newChar == '-');
		
		return newChar;
	}
	
	private int cropInt(int i, int length) throws Exception {
		// Convert integer to string for easier processing
		String strNumber = Integer.toString(i);

		// In case of negative length
		if(length < 0) {
			// Make length positive
			length = -length;
			
		// In case of length is zero
		} else if(length == 0) {
			// Throw exception
			throw new Exception("Length must be greater than 0!");
		}
		
		// Check if number is large enough to be cropped
		if(strNumber.length() >= length) {
			// If yes, crop
			return new Integer(strNumber.substring(0, length-1));
		} else {
			// Else throw exception
			throw new Exception("The given number is to short to be cropped to length " + length + "!");
		}
	}
}
