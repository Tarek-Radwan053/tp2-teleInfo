import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public static Frame identifyFrame(String line, int frameNbr) {
        String FLAG = "01111110";  // The flag that marks the start and end of each frame

        // Find all the positions of the flags in the string
        List<Integer> flagPositions = new ArrayList<>();
        int index = 0;
        while ((index = line.indexOf(FLAG, index)) != -1) {
            flagPositions.add(index);
            index += FLAG.length();
        }

        // Ensure that there are at least two flags to form a frame
        if (flagPositions.size() < 2) {
            System.out.println("Error: Not enough flags to form a frame.");
            return null;
        }

        // Now, we extract the frame based on the requested frame number
        if (frameNbr < 1 || frameNbr >= flagPositions.size()) {
            System.out.println("Error: Invalid frame number.");
            return null;
        }

        // Start position of the frame (after the first flag)
        int startIdx ;
        // End position of the frame (before the next flag)
        int endIdx;
        if (frameNbr==1){
            endIdx=flagPositions.get(frameNbr);
            startIdx= 0+FLAG.length();
        }
        else {
            endIdx = flagPositions.get(frameNbr*2-1) ;
            if (frameNbr<3){
                startIdx = flagPositions.get(frameNbr) + FLAG.length();
            }
            else{
                startIdx = flagPositions.get(frameNbr+frameNbr-2) + FLAG.length();
            }

        }

        // Extract the frame content between the flags
        String frameContent = line.substring(startIdx, endIdx);

        // Step 1: Remove bit stuffing from the frame content
        String unstuffedContent = BitStuffing.removeBitStuffing(frameContent);

        // Step 2: Ensure the unstuffed content is valid for processing
        if (unstuffedContent.length() < 32) { // At least 1 char for type, 1 for num, and 4 for CRC
            System.out.println("Error: Frame content too small after unstuffing.");
            return null;
        }

        // Step 3: Decode type (1 character = 8 bits)
        String typeBinary = unstuffedContent.substring(0, 8);
        String type = String.valueOf((char) Integer.parseInt(typeBinary, 2));

        // Step 4: Decode num (1 character = 8 bits)
        String numBinary = unstuffedContent.substring(8, 16);
        int num = Integer.parseInt(numBinary, 2);

        // Step 5: Extract data (all bits before the last 16 for CRC)
        String dataBinary = unstuffedContent.substring(16, unstuffedContent.length() - 32);
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < dataBinary.length(); i += 8) {
            String byteSegment = dataBinary.substring(i, Math.min(i + 8, dataBinary.length()));
            data.append((char) Integer.parseInt(byteSegment, 2));
        }
        //System.out.println("data: "+data.toString());

        // Step 6: Extract and decode CRC (last 16 bits)
        String crc = unstuffedContent.substring(0,unstuffedContent.length() - 32);
        //System.out.println("unnstuffeddata: "+crc);
        crc = CRC.calculateCRC(crc); // assuming CRC is calculated based on the received content
        crc = BitStuffing.stringToBinary(crc);  // Convert CRC into binary if needed
        //System.out.println("crc: "+crc);

        // Return the reconstructed Frame object
        return new Frame(type, num, data.toString(), crc);
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

    public void setData(String data) {
        this.data = data;
    }
}
