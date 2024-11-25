import java.util.*;
import java.io.IOException;

public class TestSuite {
    public static void simulateError(Frame frame) {
        // Invert a bit in the frame to simulate an error
        frame.setData(frame.getData().substring(0, frame.getData().length() - 1) + (frame.getData().charAt(frame.getData().length() - 1) == '0' ? '1' : '0'));
        System.out.println("Simulating error in frame " + frame.toByteString());
    }

    public static void simulateFrameLoss(List<Frame> frames) {
        // Simulate the loss of a frame
        if (!frames.isEmpty()) {
            frames.remove(0);
            System.out.println("Simulating frame loss...");
        }
    }

    public static void printTraffic(List<Frame> frames) {
        // Print the traffic between sender and receiver
        for (Frame frame : frames) {
            System.out.println(frame.toByteString());
        }
    }

    public static void main(String[] args) {
        // Test 1: Test connection to receiver
        testConnection();

        // Test 2: Test sending frames
        testSendFrames();

        // Test 3: Test frame loss simulation
        testFrameLoss();

        // Test 4: Test error simulation
        testErrorSimulation();

        // Test 5: Test end of communication frame
        testEndOfCommunication();
    }

    private static void testConnection() {
        try {
            Sender sender = new Sender();
            sender.connect("localhost", 1235);
            System.out.println("testConnection passed");
        } catch (IOException e) {
            System.err.println("testConnection failed: " + e.getMessage());
        }
    }

    private static void testSendFrames() {
        try {
            Sender sender = new Sender();
            sender.connect("localhost", 8080);
            sender.sendFrames("testfile.txt");
            System.out.println("testSendFrames passed");
        } catch (IOException e) {
            System.err.println("testSendFrames failed: " + e.getMessage());
        }
    }

    private static void testFrameLoss() {
        List<Frame> frames = new ArrayList<>();
        frames.add(new Frame("I", 1, "data", "crc"));
        simulateFrameLoss(frames);
        if (frames.isEmpty()) {
            System.out.println("testFrameLoss passed");
        } else {
            System.err.println("testFrameLoss failed");
        }
    }

    private static void testErrorSimulation() {
        Frame frame = new Frame("I", 1, "data", "crc");
        simulateError(frame);
        if (!"data".equals(frame.getData())) {
            System.out.println("testErrorSimulation passed");
        } else {
            System.err.println("testErrorSimulation failed");
        }
}

    private static void testEndOfCommunication() {
        try {
            Sender sender = new Sender();
            sender.connect("localhost", 8080);
            sender.sendFrames("testfile.txt");
            // Assuming the last frame is the end of communication frame
            Frame endFrame = new Frame("F", 0, null, "crc");
            sender.sendFrame(endFrame, true);
            System.out.println("testEndOfCommunication passed");
        } catch (IOException e) {
            System.err.println("testEndOfCommunication failed: " + e.getMessage());
        }
    }
}