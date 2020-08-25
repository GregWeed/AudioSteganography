import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Steg-o-nography v1.1 is a program for hiding a file type of your choice within a wave sound file.
 * @author gregweed
 *
 */
public class Main {
	//Stores a wave file that will be modified
	static byte[] waveFile = null;
	//Stores a file of any type that will be hidden in waveFile
	static byte[] hiddenFile = null;
	//Contains all indices of the original wave file that will be modified.  Starts at waveFile[47], increments by 75 per index to save sound quality. 
	static String[] waveFileChunks;
	//Contains all bits of the hidden file in a string.  These will be iterated and replace each least significant bit in the waveFileChunks array.
	static String hiddenFileBinary = "";
	//Bit spacing for the hidden file.  When set to 4, every fourth byte in the wave file has its least significant bit manipulated
	static int BIT_SPACE = 4;
	//holds the file extension when encoding a file so that it can be stored along with all of the data
	static byte[] fileExtension = null;
	//Delimiters are added to the stored hidden file so that separation during the decoding process is simple.
	static byte[] delimiter = {'*','*','*'};
	//Holds a copy of the waveFile[].  This will be changed when we add modified chunks containing the hidden bits from the file that is being hidden.
	static byte[] modifiedFile = null;
	
	
	/**
	 * The main method contains dialog so the user may input files for manipulation in the console.
	 * @param args
	 */
	public static void main(String[] args) {
		//Cheesy ascii art.
		System.out.println("                     , \\\n" + 
				"                     :  \\  /\\/ |   _\n" + 
				"                 .\\  |   :: /  | ,' |\n" + 
				"                 | \\ |   ||'/| ;'   |\n" + 
				"                 :  \\:   '// | |    |\n" + 
				"                 :   \\`.//:  ;\\;    :\n" + 
				"                 '    : : | /  `.  /___\n" + 
				"                  \\   | | `' ,   \\;'   |\n" + 
				"                 / `._; . /\\      \\    ;\n" + 
				"                :      / `. \\   `  ._,'\n" + 
				"                ;      `-.'.',\\/`.,:-.\n" + 
				"                )   `       '-.\\-/ \\\\__>\n" + 
				"                >                `--'  ``---.\n" + 
				"               /  .    ,              ,    * `-.\n" + 
				"              :               ,'      `  ______<\n" + 
				"              : ,   \\    .____..-;``--,-'\n" + 
				"               ) -. ,'>-..,-.__,' \\.-'(\\\n" + 
				"              :    : / _ /   \\  - :\\ _ \\\\,    . ,\n" + 
				"              :  _ | \\   )   / _  |:   : `._)_,)/\n" + 
				"               )   | :`  |   :    ;| . ).__'_.-'\n" + 
				"               :   ) ; _ ;   |   / ; _ ;\n" + 
				"               ) - \\ `^^^'   ; - \\ `^^'    Steg-o-nography v1.1\n" + 
				"               '^^^`         `^^^'");//Ascii art courtesy of: http://www.ascii-art.de/ascii/s/stegosaurus.txt



		System.out.print("Menu options are as follows: \n"
				+ "1.) Encode a .wav file with a hidden file.\n"
				+ "2.) Recover a hidden file.\n"
				+ "0.) Exit program.\n"
				+ ">>> ");

		//Main dialog
		while(true) {
			Scanner userInput = new Scanner(System.in);
			int menuSelection;
			try {
				menuSelection = userInput.nextInt();
			} catch (Exception e) {
				System.out.println("Please enter a value from the menu...");
				break;
			}
			switch (menuSelection) {
			case 1:  menuSelection = 1;
				System.out.println("Please enter a .wav file that you wish to hide a file within:");
				System.out.print(">>> ");
				userInput.nextLine();
				String waveFileInput;
				waveFileInput = userInput.nextLine();
				System.out.println("Please enter any file that you wish to hide, include the extension so that it can be stored within the wave file:");
				System.out.print(">>> ");
				String hideFile = userInput.nextLine();

				//Uploads the files that will be manipulated.
				uploadFiles(waveFileInput, hideFile);

				//Runs check on the input file to determine if they are sized appropriately and that the file that is hiding
				//the inner file is .wav format
				checkFileSize(waveFileInput);

				//Gets the file extension and stores in a byte array.
				getFileExt(hideFile);

				System.out.println("Encoding... Please wait a moment.");
				//Converts both files into binary string arrays using string builder
				encode();

				//Adds ever bit from hiddenFileBinary to the least significant bit of the waveFileChunks array.  If the HiddenFileBinary + fileExtention string is larger than the 
				//hidden file chunks array, then the file will not fit and should be handled appropriately
				mergeHiddenFile();

				//Writes the modified file
				writeModified("out.wav");
				System.out.println("Your file was written to the project folder as out.wav.");
				System.out.println();
				System.out.print("Menu options are as follows: \n"
						+ "1.) Encode a .wav file with a hidden file.\n"
						+ "2.) Recover a hidden file.\n"
						+ "0.) Exit program.\n");
				System.out.println("Please enter a menu item.");
				System.out.print(">>> ");
				break;
			case 2:  menuSelection = 2;

				System.out.println("Please enter a .wav file that you wish to decode:");
				System.out.print(">>> ");
				userInput.nextLine();
				String decodeMe;
				decodeMe = userInput.nextLine();
				System.out.println("Decoding... Please wait a moment.");
				//Decodes an input .wav file
				decode(decodeMe);
				
				System.out.println("Your file was written to the project folder as out.fileExtensionOfHiddenFile.");
				System.out.println();
				System.out.print("Menu options are as follows: \n"
						+ "1.) Encode a .wav file with a hidden file.\n"
						+ "2.) Recover a hidden file.\n"
						+ "0.) Exit program.\n");
				System.out.println("Please enter a menu item.");
				System.out.print(">>> ");
			break;
			case 0:  menuSelection = 0;
			System.out.println("Thanks for using Steg-o-nography v1.1.\n"
					+ "Program ended...");
			System.exit(0);
			default: System.out.println("Please enter a menu item.");
			}
		}
	}


	/**
	 * Determines that the input files are in the appropriate format and that the hidden file isn't too large for the wave file.
	 * @param filePath
	 */
	public static void checkFileSize(String filePath) {
		System.out.println("HiddenFile size: " + hiddenFile.length);
		System.out.println("WaveFile size: " + waveFile.length);
		System.out.println("HiddenFile converted to bits: " + (hiddenFile.length * 8));
		System.out.println("WaveFile divided by bitspace: " + (((waveFile.length - 500)) / BIT_SPACE) / BIT_SPACE);
		
		//Ensures that the file being embedded is .wav
		String[] extenstion = filePath.split("\\.(?=[^\\.]+$)");
		if(!extenstion[1].equals("wav")) {
			System.out.println(extenstion[1]);
			System.err.println("This program is only designed to hide files within a .wav file.  Please try uploading a "
					+ "file with a .wav extension.");
			System.exit(0);
		}
		
		/* Here we handle the file size.  The wave file is divided by the bit space twice.  The reason for doing so is to ensure that 
		 * only a small portion of the wave file is occupied with hidden bits. For instance, if I skip every 4 bytes of the wave file, then a quarter of the
		 * file contains a hidden bit.  Of that quarter, only a quarter of those bytes are modified.  Therefore only 1/16th of the file is occupied with hidden bits.
		 * If we increase the BIT_SPACE to 6, then the same goes for this value, except 1/6 of the wave file is occupied, and of that 1/6th only 1/36 is available.
		 * In reality, the entire bit spacing is being used (the default spacing is 1 hidden bit per 4 bytes), but to ensure that file corruption does not take 
		 * place, the additional bytes are excluded in this calculation.*/
		if((((waveFile.length - 500)) / BIT_SPACE) / BIT_SPACE < (hiddenFile.length * 8) || BIT_SPACE < 4) {
			if (BIT_SPACE < 4) {
				System.out.println("Bit space is too small which will cause heap overflow, increase the BIT_SPACE in source code to ensure the file is embedded properly.");
				System.exit(0);
			}
			System.err.println("Hidden file size too large.");
			System.exit(0);
		}
		
		//The largest wave file that was embedded during testing was 107342728 bytes.  If the .wav file is too large java runs out of heap space.
		if (waveFile.length > 107342728) {
			System.err.println("Please use a smaller wave file (no large than 107342728 bytes) to eliminate heap overflows in the JVM.");
			System.exit(0);
		}
	}

	/**
	 * Uploads files for manipulation and stores them in hiddenFile and waveFile.  Both are byte arrays, the hiddenFile array holds the file
	 * you are trying to hide within the wave file, the waveFile holds the .wav file you are embedding the hidden file within.
	 * @param waveFilePath
	 * @param hiddenFilePath
	 */
	public static void uploadFiles(String waveFilePath, String hiddenFilePath) {
		try {
			waveFile = Files.readAllBytes(Paths.get(waveFilePath));
			hiddenFile = Files.readAllBytes(Paths.get(hiddenFilePath));
		} catch (IOException e1) {
			//System.out.println(e1);
			System.out.println("Ensure that you are entering the correct file/s and the appropriate path.\n"
					+ "If the problem persists, try putting your files in the project folder to eliminate entering the file path.");
			System.out.println("Exiting program.");
			System.exit(0);
		}
	}

	/**
	 * Encode manipulates only the least significant bit starting at byte 500 and skipping every byte between the BIT_SPACING which by default is 4. 
	 * Each of the bits from the hiddenFile are placed in these locations. For example:
	 * 				
	 * 							 ________The bits from the hidden file only affect this column. So one byte of the hidden file will fill 8
	 * 							 |		 of the bytes that are skipped to.  For every 8 chunks of the wave file (chunks are the bytes that are skipped to),
	 * 							 |		 1 byte of the hidden file can be stored.				
	 * 							 |		 
	 * 			waveFile[ 00000000
	 * 					  01010101
	 * 					  01100110
	 * 						 .
	 * 					 	 .
	 * 					 	 .
	 * 				      10100111
	 */
	public static void encode() {
		//This section iterates the wave file, but only the indices that we will be modifying. From the 500th byte on the original wave file,
		//we increment 75 bytes before making a modification. Caution should be taken when iterating the modifiedFile, as its index 0 is equivalent 
		//to the 500th index of the waveFile array which has the original bytes. The next index for the waveFileChunks[2] is actually the 500th + 4bytes in 
		//the original waveFile which is waveFile[504].
		//Later we will modify each least significant bit in the modified file array with every bit from a binary string containing all of the hidden files
		//data.
		waveFileChunks = new String[waveFile.length];
		int iterator = 0;
		StringBuilder waveBinary = new StringBuilder();
		for (int i = 500; i < waveFile.length; i += BIT_SPACE)
		{
			int index = waveFile[i];
			for (int j = 0; j < 8; j++)
			{
				waveBinary.append((index & 128) == 0 ? 0 : 1);
				index <<= 1;
			}
			waveFileChunks[iterator] = waveBinary.toString();
			waveBinary.replace(0, waveBinary.length(), "");
			iterator++;
		}

		//resize waveFileChunks to remove null values
		int counter = 0;
		for (int i = 0; i < waveFileChunks.length; i++) {
			if(waveFileChunks[i] == null) {
				break;
			}counter ++;
		}
		String[] temp = waveFileChunks.clone();
		waveFileChunks = new String[counter];
		for (int i = 0; i < waveFileChunks.length; i++) {
			waveFileChunks[i] = temp[i];
		}

		//Reads hidden file in as binary.  This will be a large string, each bit will be placed in the LSB of each index in the binary array of the wave file.
		StringBuilder hiddenBinary = new StringBuilder();
		for (byte b : hiddenFile)
		{
			int val = b;
			for (int i = 0; i < 8; i++)
			{
				hiddenBinary.append((val & 128) == 0 ? 0 : 1);
				val <<= 1;
			}
		}
		//Large string of bits, place each in the LSB of the wave file
		hiddenFileBinary = hiddenBinary.toString();

		//Now we modify the hiddenFileBinary so that the file type is added at the beginning of the hidden file
		//The format is as follows: 
		//fileExtension|delimiter(***)|length(which will be the # of bytes iterated before reaching the end, starting at the last delimiter)|delimiter|hiddenFileData
		//When decoding a file, the bytes will be read up until the end of the second delimiter before writing the actual file, as the file extension and data length
		//must be known before decoding begins.
		//Builds the binary for the file extension
		StringBuilder fileExtBuilder = new StringBuilder();
		for (byte b : fileExtension)
		{
			int val = b;
			for (int i = 0; i < 8; i++)
			{
				fileExtBuilder.append((val & 128) == 0 ? 0 : 1);
				val <<= 1;
			}
		}
		String fileExtensionString = fileExtBuilder.toString();

		//Builds the binary for the delimiter
		StringBuilder delimiterBuilder = new StringBuilder();
		for (byte b : delimiter)
		{
			int val = b;
			for (int i = 0; i < 8; i++)
			{
				delimiterBuilder.append((val & 128) == 0 ? 0 : 1);
				val <<= 1;
			}
		}
		String delimiterString = delimiterBuilder.toString();

		String length = hiddenFileBinary.length() + "";
		byte[] lengthBytes = length.getBytes();

		//Builds the binary for the length of the data.  This is the bit length of the hiddenFileBinary, which will be the number of bytes that will
		//be iterated when decoded from the last delimiter during decode.
		StringBuilder lengthBuilder = new StringBuilder();
		for (byte b : lengthBytes)
		{
			int val = b;
			for (int i = 0; i < 8; i++)
			{
				lengthBuilder.append((val & 128) == 0 ? 0 : 1);
				val <<= 1;
			}
		}
		String lengthString = lengthBuilder.toString();

		//Updates hiddenBinary to the modifed format: 
		//fileExtension|delimiter(***)|length(which will be the # of bytes iterated before reaching the end, starting at the last delimiter)|delimiter|hiddenFileData
		hiddenFileBinary = fileExtensionString + delimiterString + lengthString + delimiterString + hiddenFileBinary;
	}

	/**
	 * Gets the file extension for the encoding process and stores the bytes. These byes are used in the encoding process to store the hidden file extension.(see encode());
	 * @param filePath
	 */
	public static void getFileExt(String filePath) {
		//Adds error handling for files containing periods in their names. Ex: hidden.file.txt
		String[] extenstion = filePath.split("\\.(?=[^\\.]+$)");
		String fileExt = "." + extenstion[1];
		fileExtension = fileExt.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Makes a clone of the original wave file and integrates all of the modified bytes that have had the least significant bit manipulated.
	 */
	public static void mergeHiddenFile() {
		//Copies the original waveFile
		modifiedFile = waveFile.clone();

		//Least significant bit modification begins here.  The entire hiddenFileBinary string will be placed in each LSB of the waveFileChunks array.
		for (int i = 0; i < hiddenFileBinary.length(); i++) {
			waveFileChunks[i] = waveFileChunks[i].substring(0, 7) + hiddenFileBinary.charAt(i);
		}

		//now the modified file will be merged with hidden file chunks.  All thats left after the merge is writing the new file.
		for (int i = 500, j = 0; i < waveFileChunks.length; i+= BIT_SPACE, j++) {
			modifiedFile[i] = (byte)Integer.parseInt(waveFileChunks[j], 2);
		}
	}

	/**
	 * Writes the modified file to the projects folder.
	 * @param filePath
	 */
	public static void writeModified(String filePath) {
		//Writes file to the specified path while adding the file extension
		try {
			Files.write(Paths.get(filePath), modifiedFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Decode is similar to the encoding process.  All bytes are read in from a wave file containing a hidden file. The indices that have a manipulated least significant
	 * bit are placed within a byte array.  All of the bytes are then converted to binary and parsed through, using the file format below during the parsing process
	 *  to ultimately recover the hidden file.
	 *  fileExtension|delimiter(***)|length(which will be the # of bytes iterated before reaching the end, starting at the last delimiter)|delimiter|hiddenFileData
	 * @param filePath
	 */
	public static void decode(String filePath) {

		byte[] encodedFile = null;
		try {
			encodedFile = Files.readAllBytes(Paths.get(filePath));
		} catch (IOException e1) {
			System.out.println(e1);
			System.out.println();
			System.out.println("Ensure that you are entering the correct file/s and the appropriate path.\n"
					+ "If the problem persists, try putting your files in the project folder to eliminate entering the file path.");
			System.out.println("Exiting program.");
			System.exit(0);
		}

		//Gets all possible modified chunks from the read .wav file
		String[] hiddenChunks = new String[encodedFile.length];

		// Convert each byte value into a binary string
		int iterator = 0;
		StringBuilder waveBinary = new StringBuilder();
		
		for (int i = 500; i < encodedFile.length - 1; i += BIT_SPACE)
		{
			int index = encodedFile[i];
			for (int j = 0; j < 8; j++)
			{
				waveBinary.append((index & 128) == 0 ? 0 : 1);
				index <<= 1;
			}
			hiddenChunks[iterator] = waveBinary.toString();
			waveBinary.replace(0, waveBinary.length(), "");
			iterator++;
		}

		//resize hiddenChnks to remove null values
		int counter = 0;
		for (int i = 0; i < hiddenChunks.length; i++) {
			if(hiddenChunks[i] == null) {
				break;
			}counter ++;
		}
		String[] temp = hiddenChunks.clone();
		hiddenChunks = new String[counter];
		for (int i = 0; i < hiddenChunks.length; i++) {
			hiddenChunks[i] = temp[i];
		}

		//All thats left is to grab each least significant bit from the hiddenChunks array.
		StringBuilder hiddenBinary = new StringBuilder();
		for (int i = 0; i < hiddenChunks.length; i++) {
			hiddenBinary.append(hiddenChunks[i].charAt(7));
		}
		//Large string of bits, this is the hidden binary containing all LSB
		String hiddenBinaryString = hiddenBinary.toString();

		//Add delimiter to separate into 8 bit strings
		StringBuilder addSlashString = new StringBuilder();
		int count = 0;
		for (int i = 0; i < hiddenBinaryString.length(); i++) {
			if(count < 8) {
				addSlashString.append(hiddenBinaryString.charAt(i));
				count ++;
			}
			if(count == 8) {
				addSlashString.append("/");
				count = 0;
			}
		}
		String separated = addSlashString.toString();
		String[] separatedBinary = separated.split("/");

		
		//Now we split the array into segments separated by the delimeter: *** which is 00101010_00101010_00101010 in binary
		//These values keep track of the file type, data length, and data locations within the separated binary array
		//so we can pull that information and assemble it all in a file.
		int fileTypeEnd = 0;
		int dataLengthStart = 0;
		int dataLengthEnd = 0;
		int startIndex = 0;

		//Iterate separated binary to find the beginning of the data, the file type, and the length of the data
		int delCounter = 0;
		for (int i = 0; i < separatedBinary.length; i++) {
			if(separatedBinary[i].contains("00101010")) {
				delCounter ++;
			}
			if (delCounter == 1) {
				fileTypeEnd = i - 1;
				delCounter++;
			}
			if(delCounter == 3) {
				dataLengthStart = i + 2;
			}
			if(delCounter == 4) {
				dataLengthEnd = i;
			}
			if(delCounter == 6) {
				startIndex = i + 2;
				break;
			}
		}
		//holds the extension and length
		String hiddenFileExt = "";
		String hiddenFileL = "";
		for (int i = 0; i < separatedBinary.length; i++) {
			//Verified
			if(i <= fileTypeEnd) {
				hiddenFileExt = hiddenFileExt + (char)Integer.parseInt(separatedBinary[i], 2);
			}
			//verified
			if(i >= dataLengthStart && i <= dataLengthEnd) {
				hiddenFileL = hiddenFileL + (char)Integer.parseInt(separatedBinary[i], 2);
			}
		}
		int hiddenFileLength = Integer.parseInt(hiddenFileL);

		//Add all valid data to this array from separated binary using the start point of the hidden data we have established along
		//with the hiddenFileLength. Then it will be converted to a byte array.
		String[] hiddenData = new String[hiddenFileLength / 8];

		for (int i = 0, j = startIndex; i < hiddenData.length && j < separatedBinary.length; i++, j++) {
			hiddenData[i] = separatedBinary[j];
		}
		
		//All thats left is to write each binary string as its byte equivalent and then write to a file
		byte[] hiddenBytes = new byte[hiddenData.length];
		for (int i = 0; i < hiddenData.length; i++) {
			hiddenBytes[i] = (byte)Integer.parseInt(hiddenData[i], 2);
		}

		//Writes file to the specified path while adding the file extension
		try {
			Files.write(Paths.get("out" + hiddenFileExt), hiddenBytes);
		} catch (IOException e) {
			System.err.println("Error Writing File Method: writeFile()");
			e.printStackTrace();
		}
	}
}
