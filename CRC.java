import java.util.Objects;

public class CRC {
    private static final int POLYNOMIAL = 0x1021;  // CRC-CCITT polynomial (x16 + x12 + x5 + 1)
    private static final int INITIAL_VALUE = 0xFFFF;  // Initial CRC value

    // Method to calculate the CRC-CCITT checksum
    public static String calculateCRC(String input) {
        int crc = INITIAL_VALUE;  // Initialize CRC register

        for (char c : input.toCharArray()) {
            crc ^= (c << 8);  // XOR character bits into CRC
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {  // If the most significant bit is 1
                    crc = (crc << 1) ^ POLYNOMIAL;  // Left shift and XOR with polynomial
                } else {
                    crc = (crc << 1);  // Left shift without XOR
                }
            }
        }

        crc &= 0xFFFF;  // Ensure CRC is 16 bits
        return String.format("%04X", crc);  // Return CRC as a 4-character hex string
    }

    // Validate the CRC
    //not sure if this is correct maybe the comparaisan shoud be between the calculated crc from unstufed data and the  crc unstefed?
    public static boolean validateCRC(String input, String crc) {
        String b=calculateCRC(input);
        boolean a= Objects.equals(b, crc);
        return a;
    }

    // Calculate CRC for the entire frame (type, num, and data)
    public static String calculateFrameCRC(Frame frame) {
        String combinedData = frame.getType() + frame.getNum() + frame.getData();
        return calculateCRC(combinedData);
    }
}
