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

        while ((line ) != null || base < nextSeqNum) {
            // Send frames while window is not full
            while (nextSeqNum < base + windowSize && line != null) {
                String data = line;
                Frame frame = new Frame("I", nextSeqNum, data, "");
                String crc = CRC.calculateFrameCRC(frame);
                frame.setCrc(crc);
                sendFrame(frame);
                nextSeqNum++;  // Increment nextSeqNum
                line = fileReader.readLine();  // Read next line for data
            }


            // Wait for ACK for the frame at 'base'
           /* if (!waitForAck(base)) {
                System.out.println("Timeout! Resending frames starting from " + base);
                //resendFrames();  // Resend frames from base onwards
            }

            // Move base forward after receiving an ACK for the frame at 'base'
            if (isAckReceivedForFrame(base)) {
                base++;  // Move base to the next unacknowledged frame
            }
            // should implent if not received the ack

            */
            break;
        }

        // Send the final frame (End of transmission)
        sendFrame(new Frame("F", nextSeqNum, null, ""));  // End of transmission frame
        System.out.println("Sent End of Communication (F) frame");
    }

    private void sendFrame(Frame frame) throws IOException {
        OutputStream out = socket.getOutputStream();
        String outputFrame = frame.toByteString();

        // Add a newline character at the end of the frame
        if (nextSeqNum > base + windowSize || frame.getType().equals("F")) {
            outputFrame += "\n";  // Append newline
        }


        out.write(outputFrame.getBytes());
        out.flush();

        System.out.println("Sent: " + outputFrame);  // Log the sent frame with newline
        sentFrames.add(frame);  // Track the sent frame
    }


    // Wait for ACK with a timeout
    private boolean waitForAck(int frameNum) {
        try {
            // Set a timeout for waiting for the ACK (e.g., 5 seconds)
            socket.setSoTimeout(5000);  // 5 seconds timeout
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            for (int i = base; i < nextSeqNum; i++) {
                System.out.println("waiting for ack for frame " + i);
                String ack = reader.readLine();


                if (ack != null && ack.equals("ACK " + frameNum)) {
                    System.out.println("Received ACK for frame " + frameNum);
                    return true;
                } else {
                    System.err.println("Received unexpected response: " + ack);
                    return false;
                }
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout waiting for ACK for frame " + frameNum);
            return false;
        } catch (IOException e) {
            System.err.println("Error receiving ACK: " + e.getMessage());
            return false;
        }
        return false;
    }





    // Resend frames starting from the 'base' frame
    private void resendFrames() throws IOException {
        for (int i = base; i < nextSeqNum; i++) {
            Frame frame = sentFrames.get(i);
            sendFrame(frame);  // Resend the frame
        }
    }

    private boolean isAckReceivedForFrame(int frameNum) {
        // In reality, this would check the receiver's ACK and update the base
        // Here we simulate that it is always true for simplicity.
        return true;  // Placeholder to simulate ACK reception
    }

    private void startTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Timeout! Resending frames...");
                // Logic to resend frames
            }
        }, 3000);  // 3-second timer
    }

    public void stopTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    // Logic to handle ACKs, resend frames, etc.

    public static void main(String[] args) {
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
