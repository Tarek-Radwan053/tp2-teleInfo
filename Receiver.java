import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Receiver {

    private ServerSocket serverSocket;
    private List<Frame> receivedFrames = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        // Start the receiver by accepting port from command-line args
        if (args.length != 1) {
            System.out.println("Usage: java Receiver <port>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);  // Port number passed as command-line argument
        Receiver receiver = new Receiver();
        receiver.start(port);
    }

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Receiver listening on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            InputStream in = clientSocket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                Frame frame = parseFrame(line);
                if (checkErrors(frame)) {
                    sendAck(clientSocket, frame.getNum());
                } else {
                    sendRejection(clientSocket, frame.getNum());
                }
            }
        }
    }

    private Frame parseFrame(String line) {
        // Parse the line to create a Frame object
        // This is a simplified example
        return new Frame('I', 0, line, "");  // Dummy values
    }

    private boolean checkErrors(Frame frame) {
        // Verify the CRC
        return CRC.validateCRC(frame.getData(), frame.getCrc());
    }

    private void sendAck(Socket clientSocket, int frameNum) throws IOException {
        Frame ackFrame = new Frame('A', frameNum, null, "");
        OutputStream out = clientSocket.getOutputStream();
        out.write(ackFrame.toByteString().getBytes());
        out.flush();
        System.out.println("Sent ACK for frame " + frameNum);
    }

    private void sendRejection(Socket clientSocket, int frameNum) throws IOException {
        Frame rejFrame = new Frame('R', frameNum, null, "");
        OutputStream out = clientSocket.getOutputStream();
        out.write(rejFrame.toByteString().getBytes());
        out.flush();
        System.out.println("Sent REJ for frame " + frameNum);
    }
}
