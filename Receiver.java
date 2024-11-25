import java.io.*;
import java.net.*;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Receiver {

    private int nbrFrames=0;
    private   int endt=0;//end of communication
    private int expectedFrameNum = 0;  // Keeps track of the expected frame number
    private int windowSize = 1;  // Window size for Go-Back-N ARQ

    private ServerSocket serverSocket;


    public static void main(String[] args) throws IOException {
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
        serverSocket.setSoTimeout(60000);
        System.out.println("Receiver listening on port " + port);
        while (true) {
            try (Socket clientSocket = serverSocket.accept()) {
                System.out.println("Connection accepted from " + clientSocket.getInetAddress());

                // Process the connection frame (type "C")
                if (!processConnectionFrame(clientSocket)) {
                    System.err.println("Invalid connection frame. Closing connection.");
                    clientSocket.close();
                    continue;
                }
                // Process the incoming data from the sender
                processIncomingData(clientSocket);
                if (endt==1) {
                    break;
                }
            } catch (IOException e) {
                System.err.println("Error handling client connection: " + e.getMessage());
            }
        }
    }

    private boolean processConnectionFrame(Socket clientSocket) throws IOException {
        InputStream in = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = reader.readLine();

        if (line != null) {
            Frame frame = Frame.identifyFrame(line, 1);
            if (frame != null && "C".equals(frame.getType()) && CRC.validateCRC(frame)) {
                System.out.println("Received valid connection frame.");
                sendAck(clientSocket, frame.getNum());
                return true;
            }
        }
        return false;
    }

    private void processIncomingData(Socket clientSocket) throws IOException {
        InputStream in = clientSocket.getInputStream();
        Scanner scanner = new Scanner(in);  // Using Scanner to read input stream
        System.out.println("Receiving data from sender...");

        // Loop through the incoming data
        while (scanner.hasNext()) {
            String line = scanner.nextLine();  // Read each line
            System.out.println("line: " + line);

            // Count the number of frames based on flags
            nbrFrames = countFlags(line, "01111110") / 2;
            System.out.println("Detected " + nbrFrames + " frames.");

            // Process each frame within the window size
            for (int frameNbr = 1; frameNbr <= nbrFrames; frameNbr++) {
                Frame frame = Frame.identifyFrame(line, frameNbr);
                if (frame == null) {
                    System.err.println("Frame " + frameNbr + " is invalid. Skipping...");
                    continue;
                }
                // Check if the frame type is "F" (End of communication frame)
                if ("F".equals(frame.getType())) {
                    System.out.println("Received End of Communication (F) frame.");
                    // Unstuff the frame data
                    String unstuffedData = BitStuffing.removeBitStuffing(frame.getData());
                    Frame unstuffedFrame = new Frame(frame.getType(), frame.getNum(), unstuffedData, frame.getCrc());
                    //Validate the CRC
                    if (CRC.validateCRC(unstuffedFrame)) {
                        System.out.println("End of Communication frame is valid. Closing connection.");
                        endt = 1;
                        return;
                    } else {
                        System.err.println("CRC mismatch for End of Communication frame. Closing connection.");
                    }
                }

                // Check if the frame is the expected one within the window
                if (frame.getNum() == expectedFrameNum) {
                    // If the frame is correct, acknowledge it and increment the expected number
                    if (checkErrors(frame)) {
                        System.out.println("Received valid frame: " + frame.getNum());
                        expectedFrameNum = (expectedFrameNum + 1) ;  // Slide the window
                    } else {
                        sendRejection(clientSocket, frame.getNum());
                    }
                } else if (frame.getNum() > expectedFrameNum && frame.getNum() < expectedFrameNum + windowSize) {
                    // If the frame is within the window, accept and acknowledge it
                    if (checkErrors(frame)) {
                        sendAck(clientSocket, frame.getNum());
                    } else {
                        sendRejection(clientSocket, frame.getNum());
                    }
                } else {
                    // Reject out-of-order frames
                    sendRejection(clientSocket, frame.getNum());
                }
            }
            sendAck(clientSocket, expectedFrameNum );  // Send cumulative ACK for the last frame
            if (endt==1) {
                break;
            }

        }

        System.out.println("All frames received and processed.");
    }

    public int countFlags(String line, String flag) {
        int count = 0;
        int index = 0;

        // Continue à chercher le flag jusqu'à ce qu'il n'y en ait plus
        while ((index = line.indexOf(flag, index)) != -1) {
            count++;  // Trouvé un flag, on incrémente le compteur
            index += flag.length();  // On déplace l'index après le flag trouvé
        }

        return count;
    }


    private boolean checkErrors(Frame frame) {
        // Verify the CRC
        boolean isValid = CRC.validateCRC(frame);
        if (!isValid) {
            System.err.println("CRC mismatch for frame: " + frame.getNum());
        }
        return isValid;
    }

    private void sendAck(Socket clientSocket, int frameNum) {
        try {
            if (clientSocket.isClosed() || clientSocket.isOutputShutdown()) {
                System.err.println("Cannot send ACK. Socket is closed or output is shut down.");
                return;
            }

            Frame ackFrame = new Frame("A", frameNum, "", "");
            OutputStream out = clientSocket.getOutputStream();
            String ackCrc = CRC.calculateFrameCRC(ackFrame);
            //System.out.println("ackCrc: " + ackCrc);
            ackFrame.setCrc(ackCrc);
            String outputFrame = ackFrame.toByteString() + "\n";  // Append newline;
            out.write(outputFrame.getBytes());
            out.flush();
            System.out.println("Sent ACK for frame " + frameNum);

            /*int retries = 3;
            int backoff = 100; // Milliseconds

            while (retries > 0) {
                try {
                    out.write(("ACK " + frameNum + "\n").getBytes());
                    out.flush();
                    System.out.println("ACK " + frameNum);
                    return;
                } catch (IOException e) {
                    retries--;
                    System.err.println("Retrying to send ACK... Retries left: " + retries);
                    Thread.sleep(backoff);
                    backoff *= 2; // Exponential backoff
                }
            }

            if (retries == 0) {
                System.err.println("Failed to send ACK for frame " + frameNum + " after multiple attempts.");
            }*/
        } catch (IOException e) {
            System.err.println("Error sending ACK for frame " + frameNum + ": " + e.getMessage());
        }
    }

    private void sendRejection(Socket clientSocket, int frameNum) throws IOException {
        Frame rejFrame = new Frame("R", frameNum, null, "");
        OutputStream out = clientSocket.getOutputStream();

        String rejCrc = CRC.calculateFrameCRC(rejFrame);
        rejFrame.setCrc(rejCrc);
        String outputFrame = rejFrame.toByteString() + "\n";  // Append newline;
        out.write(outputFrame.getBytes());
        out.flush();
        System.out.println("Sent REJ for frame " + frameNum);
    }
}
