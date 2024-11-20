public class BitStuffing {
    // Method to apply bit stuffing to a string of bits
    public static String applyBitStuffing(String data) {
        StringBuilder stuffedData = new StringBuilder();
        int consecutiveOnes = 0;


        for (char bit : data.toCharArray()) {
            stuffedData.append(bit);
            if (bit == '1') {
                consecutiveOnes++;
                if (consecutiveOnes == 5) {
                    stuffedData.append('0');  // Add a '0' after five '1's
                    consecutiveOnes = 0;
                }
            } else {
                consecutiveOnes = 0;  // Reset if the bit is '0'
            }
        }

        return stuffedData.toString();
    }

    // Method to remove bit stuffing from a string
    public static String removeBitStuffing(String stuffedData) {
        StringBuilder originalData = new StringBuilder();
        int consecutiveOnes = 0;


        for (int i = 0; i < stuffedData.length(); i++) {
            char bit = stuffedData.charAt(i);
            originalData.append(bit);  // Add the current bit to the original data

            if (bit == '1') {
                consecutiveOnes++;
                if (consecutiveOnes == 5) {
                    // If five consecutive ones are detected, check the next bit
                    if (i + 1 < stuffedData.length() && stuffedData.charAt(i + 1) == '0') {
                        // Skip the next '0' (bit stuffing)
                        i++;  // Increment i to skip the stuffed '0'
                    }
                    consecutiveOnes = 0;  // Reset the counter after skipping the '0'
                }
            } else {
                consecutiveOnes = 0;  // Reset the counter if the bit is '0'
            }
        }
        return originalData.toString();
    }
    // Method to convert a string to binary representation
    public static String stringToBinary(String input) {
        StringBuilder binaryRepresentation = new StringBuilder();
        byte[] byteArray = input.getBytes();  // Convert string to byte array

        for (byte b : byteArray) {
            // Convert each byte to a binary string (8 bits) and pad with leading zeros if necessary
            String binaryString = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            binaryRepresentation.append(binaryString);  // Append to the final binary string
        }

        return binaryRepresentation.toString();
    }
}
