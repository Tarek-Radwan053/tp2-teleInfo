import java.io.*;
import java.net.*;
import java.util.*;
import java.net.Socket;


public class Sender {
    private Socket socket;
    private BufferedReader fileReader;
    private List<Frame> sentFrames = new ArrayList<>();
    private final int windowSize = 4;  // Window size for Go-Back-N
    private int base = 0;  // Base of the window (first unacknowledged frame)
    private int nextSeqNum = 0;  // Next frame to send
    private Timer timer;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        System.out.println("Connected to receiver at " + host + ":" + port);
    }

    // Send frames from the file
    public void sendFrames(String fileName) throws IOException {
        fileReader = new BufferedReader(new FileReader(fileName));
        String line=fileReader.readLine();

        while ((line) != null || base < nextSeqNum) {
            // Send frames while window is not full
            while (nextSeqNum < base + windowSize && line != null) {
                String data = line;
                Frame frame = new Frame("I", nextSeqNum, data, "");
                String crc = CRC.calculateFrameCRC(frame);
                frame.setCrc(crc);
                boolean isLastFrameInWindow = (nextSeqNum == base + windowSize - 1);
                boolean isLastFrameInBatch = (line == null);
                sendFrame(frame,isLastFrameInWindow || isLastFrameInBatch);
                nextSeqNum++;  // Increment nextSeqNum
                line = fileReader.readLine();  // Read next line for data
            }
            int biggestFrame=-1;
            for (int i = nextSeqNum-1; i > base; i--) {
                if (waitForAck(i)) {
                    biggestFrame=i;
                }
            }
            // Wait for ACK for the frame at 'base'
            if (biggestFrame==-1) {
                System.out.println("Timeout! Resending frames starting from " + base);
                resendFrames(base);  // Resend frames from base onwards
                while (!waitForAck(nextSeqNum-1)) {
                    resendFrames(base);
                }

            } else if (biggestFrame==nextSeqNum-1) {
                base=biggestFrame+1;
            }
            else {
                while (!waitForAck(nextSeqNum-1)) {
                    resendFrames(biggestFrame+1);
                }
            }
        }
        // Send the final frame (End of transmission)
        sendFrame(new Frame("F", nextSeqNum, null, ""),true);  // End of transmission frame
        System.out.println("Sent End of Communication (F) frame");
    }

    private void sendFrame(Frame frame, boolean isLastFrame) throws IOException {
        OutputStream out = socket.getOutputStream();
        String outputFrame = frame.toByteString();

        // Add newline for the last frame in a batch or "F" frame
        if (isLastFrame) {
            outputFrame += "\n";
        }

        out.write(outputFrame.getBytes());
        out.flush();

        System.out.println("Sent: " + outputFrame);  // Log the frame
        sentFrames.add(frame);  // Track the sent frame
    }


    // Wait for ACK with a timeout
    private boolean waitForAck(int frameNum) {
        try {
            //socket.setSoTimeout(3000);  // 3 seconds timeout
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String ack;

            while ((ack = reader.readLine()) != null) {  // Continuously read incoming messages
                if (ack.startsWith("ACK ")) {
                    int ackNum = Integer.parseInt(ack.split(" ")[1]);  // Extract the frame number from the ACK


                    // Check if the received ACK matches the expected frameNum
                    if (ackNum == frameNum) {
                        System.out.println("Received ACK for frame " + frameNum);
                        return true;  // ACK received for the requested frame
                    } else {
                        System.err.println("Received duplicate or outdated ACK for frame " + ackNum);
                    }
                } else {
                    System.err.println("Received unexpected response: " + ack);
                }
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout waiting for ACK for frame " + frameNum);
        } catch (IOException e) {
            System.err.println("Error receiving ACK: " + e.getMessage());
        }

        return false;  // Return false if no valid ACK is received
    }


    // Resend frames starting from the 'base' frame
    private void resendFrames(int frameNbr) throws IOException {
        for (int i = frameNbr; i < nextSeqNum; i++) {
            Frame frame = sentFrames.get(i);
            boolean isLastFrame = (i == nextSeqNum - 1);
            sendFrame(frame, isLastFrame);  // Append newline if necessary
        }
    }


        // Logic to handle ACKs, resend frames, etc.

        public static void main (String[]args){
            if (args.length != 4) {
                System.out.println("Usage: java Sender <Host> <Port> <Filename> <GoBackN>");
                return;
            }

            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String fileName = args[2];
            int goBackN = Integer.parseInt(args[3]);

            try {
                Sender sender = new Sender();
                sender.connect(host, port);
                sender.sendFrames(fileName);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
