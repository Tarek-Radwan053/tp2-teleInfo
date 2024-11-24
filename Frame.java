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
        //transform from int to binairy code on 8 bits
        String numBinary="00000000";
        if (type=="I" || type=="A" || type=="F") {
             numBinary =String.format("%8s", Integer.toBinaryString(num)).replace(' ', '0');
        }
        //System.out.println("numBinary: "+numBinary);

        // Apply bit stuffing to the data if it's not null binary
        if (data!=null) {
            sb.append(data);
        }
        //System.out.println("data: "+sb.toString());



        // CRC should be calculated on unstuffed data
        //System.out.println("data: "+sb.toString());

        String allBinairy = BitStuffing.stringToBinary(sb.toString());
        //System.out.println("allBinairy: "+allBinairy);
        String allBinairy1 = allBinairy.substring(0,8);
        //System.out.println("allBinairy1: "+allBinairy1);
        String allBinairy2 = allBinairy.substring(8);
        //System.out.println("data: "+allBinairy2);
        allBinairy = allBinairy1 + numBinary + allBinairy2;
        //System.out.println("allBinairy: "+allBinairy);

        String crc = CRC.calculateCRC(allBinairy);
        //System.out.println("crcofsender: "+crc);

        crc = BitStuffing.stringToBinary(crc);
        //System.out.println("crcofsenderBinairy: "+crc);


        allBinairy = allBinairy+crc;
        allBinairy = BitStuffing.applyBitStuffing(allBinairy);


         return FLAG+allBinairy+FLAG;
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
