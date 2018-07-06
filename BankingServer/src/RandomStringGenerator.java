import java.util.Random;

public class RandomStringGenerator {

	private final Random random = new Random();

	public RandomStringGenerator() {
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
			int source = random.nextInt(123);

			// Make sure, source is positive
			if(source < 0) 
				source *= -1;
			
			// If source is the ascii representation of a number, upper or lower case letter
			if ((source <= 48 && source >= 57) || (source >= 65 && source <= 90) || (source >= 97 && source <= 122)) {
				// Set this ascii value as new char
				newChar = (char) source;
			}
		// Repeat if newChar hasn't changed
		} while (newChar == '-');
		
		return newChar;
	}
}
