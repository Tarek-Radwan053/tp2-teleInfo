public class TestSuite {
    public static void main(String[] args) {
        System.out.println("Testing BitStuffing...");
        testBitStuffing();
        System.out.println("Testing CRC...");
        testCRC();
        System.out.println("Testing Frame...");
        testFrame();
        System.out.println("Testing BitStuffing with empty string...");
        bitStuffingHandlesEmptyString();
        System.out.println("Testing Frame with empty data...");
        frameHandlesEmptyData();
        System.out.println("Testing BitStuffing with no stuffing needed...");
        bitStuffingHandlesNoStuffingNeeded();
        System.out.println("Testing CRC with invalid CRC...");
        crcHandlesInvalidCRC();
        System.out.println("Testing Frame with invalid byte string...");
        frameHandlesInvalidByteString();
        System.out.println("Testing BitStuffing with all ones...");
        testBitStuffingWithAllOnes();
        System.out.println("Testing CRC with all ones...");
        testCRCWithAllOnes();
        System.out.println("Testing Frame with all ones...");
        testFrameWithAllOnes();
        System.out.println("Testing BitStuffing with maximum length data...");
        testBitStuffingWithMaxLength();
        System.out.println("Testing Frame with maximum length data...");
        testFrameWithMaxLengthData();
        System.out.println("Testing Frame with corrupted CRC...");
        testFrameWithCorruptedCRC();
        System.out.println("Testing multiple frames with sequence numbers...");
        testFrameSequenceHandling();
        System.out.println("Testing Acknowledgement Frame...");
        testAcknowledgementFrame();
        System.out.println("Testing Send Frame and Wait...");
        testSendFrameAndWait();
        System.out.println("All tests passed!");

    }

    private static void testBitStuffing() {
        String data = "01111110";
        String stuffedData = BitStuffing.applyBitStuffing(data);
        String unstuffedData = BitStuffing.removeBitStuffing(stuffedData);
        assert data.equals(unstuffedData) : "Failed to stuff and unstuff data";
    }

    private static void testCRC() {
        String data = "01111110";
        String crc = CRC.calculateCRC(data);
        boolean isValid = CRC.validateCRC(Frame.fromByteString(data + crc));
        assert isValid : "Failed to validate CRC";
    }

    private static void testFrame() {
        Frame frame = new Frame("I", 0, "01111110", "10101010");
        String byteString = frame.toByteString();
        Frame newFrame = Frame.fromByteString(byteString);
        assert frame.getType().equals(newFrame.getType()) : "Failed to serialize/deserialize type";
        assert frame.getNum() == newFrame.getNum() : "Failed to serialize/deserialize num";
        assert frame.getData().equals(newFrame.getData()) : "Failed to serialize/deserialize data";
        assert frame.getCrc().equals(newFrame.getCrc()) : "Failed to serialize/deserialize crc";
    }

    private static void bitStuffingHandlesEmptyString() {
        String data = "";
        String stuffedData = BitStuffing.applyBitStuffing(data);
        String unstuffedData = BitStuffing.removeBitStuffing(stuffedData);
        assert data.equals(unstuffedData) : "Failed to handle empty string";
    }

    private static void frameHandlesEmptyData() {
        Frame frame = new Frame("I", 0, "", "10101010");
        String byteString = frame.toByteString();
        Frame newFrame = Frame.fromByteString(byteString);
        assert frame.getData().equals(newFrame.getData()) : "Failed to handle empty data in frame";
    }

    private static void bitStuffingHandlesNoStuffingNeeded() {
        String data = "00000000";
        String stuffedData = BitStuffing.applyBitStuffing(data);
        String unstuffedData = BitStuffing.removeBitStuffing(stuffedData);
        assert data.equals(unstuffedData) : "Failed to handle data with no stuffing needed";
    }

    private static void crcHandlesInvalidCRC() {
        String data = "01111110";
        String crc = "00000000"; // Invalid CRC
        boolean isValid = CRC.validateCRC(Frame.fromByteString(data + crc));
        assert !isValid : "Failed to handle invalid CRC";
    }

    private static void frameHandlesInvalidByteString() {
        try {
            Frame.fromByteString("invalid");
            assert false : "Failed to handle invalid byte string";
        } catch (IllegalArgumentException e) {
            // Expected exception
        }
    }

    private static void testBitStuffingWithAllOnes() {
        String data = "11111111";
        String stuffedData = BitStuffing.applyBitStuffing(data);
        String unstuffedData = BitStuffing.removeBitStuffing(stuffedData);
        assert data.equals(unstuffedData) : "Failed to handle data with all ones";
    }

    private static void testCRCWithAllOnes() {
        String data = "11111111";
        String crc = CRC.calculateCRC(data);
        boolean isValid = CRC.validateCRC(Frame.fromByteString(data + crc));
        assert isValid : "Failed to validate CRC with all ones";
    }

    private static void testFrameWithAllOnes() {
        Frame frame = new Frame("I", 0, "11111111", "10101010");
        String byteString = frame.toByteString();
        Frame newFrame = Frame.fromByteString(byteString);
        assert frame.getType().equals(newFrame.getType()) : "Failed to serialize/deserialize type with all ones";
        assert frame.getNum() == newFrame.getNum() : "Failed to serialize/deserialize num with all ones";
        assert frame.getData().equals(newFrame.getData()) : "Failed to serialize/deserialize data with all ones";
        assert frame.getCrc().equals(newFrame.getCrc()) : "Failed to serialize/deserialize crc with all ones";
    }

    private static void testBitStuffingWithMaxLength() {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            data.append("0"); // Generate a large string
        }
        String stuffedData = BitStuffing.applyBitStuffing(data.toString());
        String unstuffedData = BitStuffing.removeBitStuffing(stuffedData);
        assert data.toString().equals(unstuffedData) : "Failed to handle maximum length data";
    }

    private static void testFrameWithMaxLengthData() {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            data.append("1"); // Generate a large string
        }
        Frame frame = new Frame("I", 0, data.toString(), CRC.calculateCRC(data.toString()));
        String byteString = frame.toByteString();
        Frame newFrame = Frame.fromByteString(byteString);
        assert frame.getData().equals(newFrame.getData()) : "Failed to handle maximum length data in frame";
    }

    private static void testFrameWithCorruptedCRC() {
        String data = "Hello";
        String crc = CRC.calculateCRC(data);
        String corruptedFrame = "01111110I0" + data + "FFFF" + "01111110"; // Manually corrupt the CRC
        try {
            Frame frame = Frame.fromByteString(corruptedFrame);
            boolean isValid = CRC.validateCRC(frame);
            assert !isValid : "Failed to handle corrupted CRC";
        } catch (IllegalArgumentException e) {
            // Expected exception for invalid frame format
        }
    }

    private static void testFrameSequenceHandling() {
        for (int i = 0; i < 10; i++) {
            Frame frame = new Frame("I", i, "Data" + i, CRC.calculateCRC("Data" + i));
            String byteString = frame.toByteString();
            Frame newFrame = Frame.fromByteString(byteString);
            assert frame.getNum() == newFrame.getNum() : "Failed to handle sequence number: " + i;
            assert frame.getData().equals(newFrame.getData()) : "Failed to handle data in sequence number: " + i;
        }
    }

    private static void testAcknowledgementFrame() {
        Frame ackFrame = new Frame("A", 1, "", CRC.calculateCRC(""));
        String byteString = ackFrame.toByteString();
        Frame newFrame = Frame.fromByteString(byteString);
        assert ackFrame.getType().equals(newFrame.getType()) : "Failed to serialize/deserialize ACK frame type";
        assert ackFrame.getNum() == newFrame.getNum() : "Failed to serialize/deserialize ACK frame num";
        assert ackFrame.getData().equals(newFrame.getData()) : "Failed to serialize/deserialize ACK frame data";
        assert ackFrame.getCrc().equals(newFrame.getCrc()) : "Failed to serialize/deserialize ACK frame crc";
    }

    private static void testSendFrameAndWait() {
        Frame sendFrame = new Frame("I", 2, "TestData", CRC.calculateCRC("TestData"));
        String byteString = sendFrame.toByteString();
        Frame newFrame = Frame.fromByteString(byteString);
        assert sendFrame.getType().equals(newFrame.getType()) : "Failed to serialize/deserialize send frame type";
        assert sendFrame.getNum() == newFrame.getNum() : "Failed to serialize/deserialize send frame num";
        assert sendFrame.getData().equals(newFrame.getData()) : "Failed to serialize/deserialize send frame data";
        assert sendFrame.getCrc().equals(newFrame.getCrc()) : "Failed to serialize/deserialize send frame crc";
    }
}