import java.util.*;

public class TestSuite {
    public static void simulateError(Frame frame) {
        // Invert a bit in the frame to simulate an error
        System.out.println("Simulating error in frame " + frame.toByteString());
    }

    public static void simulateFrameLoss(List<Frame> frames) {
        // Simulate the loss of a frame
        System.out.println("Simulating frame loss...");
    }

    public static void printTraffic(List<Frame> frames) {
        // Print the traffic between sender and receiver
        for (Frame frame : frames) {
            System.out.println(frame.toByteString());
        }
    }
}
