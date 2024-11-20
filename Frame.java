import java.util.Arrays;

public class Frame {
    private static final String FLAG = "01111110"; // Flag to mark the beginning and end of a frame
    private String type;  // Frame type ('I' for information, 'A' for ACK, etc.)
    private int num;    // Frame number (sequence number)
    private String data;  // Data carried in the frame
    private String crc;   // CRC checksum for the frame

    public Frame(String type, int num, String data, String crc) {
        this.type = type;
        this.num = num;
        this.data = data;
        this.crc = crc;
    }

    // Convert the frame to a byte string representation
    //not sure if i should add bit stuffing individually or all together
    public String toByteString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        sb.append(num);

        // Apply bit stuffing to the data if it's not null binary
        if (data!=null) {
            sb.append(data);
        }


        // CRC should be calculated on stuffed data

        String crc = CRC.calculateCRC(sb.toString());
        sb.append(crc);
        System.out.println("crc: "+crc);



        return (FLAG+sb+FLAG);
    }


    // Getters
    public String getType() { return type; }
    public int getNum() { return num; }
    public String getData() { return data; }
    public String getCrc() { return crc; }

    public void setCrc(String crc) {
        this.crc = crc;
    }
}
