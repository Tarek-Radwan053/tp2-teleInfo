import java.io.*;
import java.net.*;
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
                Frame frame = identifyFrame(line, frameNbr);
                if (frame == null) {
                    System.err.println("Frame " + frameNbr + " is invalid. Skipping...");
                    continue;
                }
                // Check if the frame type is "F" (End of communication frame)
                if ("F".equals(frame.getType())) {
                    System.out.println("Received End of Communication (F) frame.");
                    // Send acknowledgment for the end frame
                    System.out.println("Communication ended, closing connection.");
                    endt=1;
                    return;  // Stop processing, end the communication
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
    public Frame identifyFrame(String line, int frameNbr) {
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

    private boolean checkErrors(Frame frame) {
        // Verify the CRC
        return CRC.validateCRC(frame);
    }

    private void sendAck(Socket clientSocket, int frameNum) {
        try {
            if (clientSocket.isClosed() || clientSocket.isOutputShutdown()) {
                System.err.println("Cannot send ACK. Socket is closed or output is shut down.");
                return;
            }

            //Frame ackFrame = new Frame("A", frameNum, null, "");
            OutputStream out = clientSocket.getOutputStream();
            //String outputFrame = ackFrame.toByteString();
            //outputFrame += "\n";  // Append newline
            //out.write(outputFrame.getBytes());

            int retries = 3;
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
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error sending ACK for frame " + frameNum + ": " + e.getMessage());
        }
    }

    private void sendRejection(Socket clientSocket, int frameNum) throws IOException {
        Frame rejFrame = new Frame("R", frameNum, null, "");
        OutputStream out = clientSocket.getOutputStream();
        String outputFrame = rejFrame.toByteString() + "\n";  // Append newline
        out.write(rejFrame.toByteString().getBytes());
        out.flush();
        System.out.println("Sent REJ for frame " + frameNum);
    }
}
