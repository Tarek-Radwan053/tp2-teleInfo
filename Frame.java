public class Frame {
    private static final String FLAG = "01111110"; // Flag to mark the beginning and end of a frame
    private char type;  // Frame type ('I' for information, 'A' for ACK, etc.)
    private int num;    // Frame number (sequence number)
    private String data;  // Data carried in the frame
    private String crc;   // CRC checksum for the frame

    public Frame(char type, int num, String data, String crc) {
        this.type = type;
        this.num = num;
        this.data = data;
        this.crc = crc;
    }

    // Convert the frame to a byte string representation
    public String toByteString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FLAG);
        sb.append(type);
        sb.append((char) num);

        // Apply bit stuffing to the data if it's not null
        String stuffedData = (data != null) ? BitStuffing.applyBitStuffing(data) : "";
        sb.append(stuffedData);

        // CRC should be calculated on stuffed data
        String crc = CRC.calculateFrameCRC(this);
        sb.append(crc);

        sb.append(FLAG);  // Frame ends with the flag
        return sb.toString();
    }

    // Getters
    public char getType() { return type; }
    public int getNum() { return num; }
    public String getData() { return data; }
    public String getCrc() { return crc; }
}
