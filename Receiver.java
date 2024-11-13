import java.io.*;
import java.net.*;

public class Receiver {
    private ServerSocket serverSocket;
    private List<Frame> receivedFrames = new ArrayList<>();

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

    private void sendAck(Socket clientSocket, int num) throws IOException {
        OutputStream out = clientSocket.getOutputStream();
        Frame ackFrame = new Frame('A', num, null, "");
        out.write(ackFrame.toByteString().getBytes());
        out.flush();
        System.out.println("Sent ACK for frame " + num);
    }

    private void sendRejection(Socket clientSocket, int num) throws IOException {
        OutputStream out = clientSocket.getOutputStream();
        Frame rejFrame = new Frame('R', num, null, "");
        out.write(rejFrame.toByteString().getBytes());
        out.flush();
        System.out.println("Sent REJ for frame " + num);
    }
}
