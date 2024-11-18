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
            originalData.append(bit);

            if (bit == '1') {
                consecutiveOnes++;
                if (consecutiveOnes == 5) {
                    // Check the next bit and skip '0' if bit stuffing is detected
                    if (i + 1 < stuffedData.length() && stuffedData.charAt(i + 1) == '0') {
                        i++;  // Skip the '0'
                    }
                    consecutiveOnes = 0;
                }
            } else {
                consecutiveOnes = 0;  // Reset if the bit is '0'
            }
        }

        return originalData.toString();
    }
}
