import java.io.*;
import java.net.*;
import java.util.*;
import java.net.Socket;


public class Sender {
    private Socket socket;
    private BufferedReader fileReader;
    private List<Frame> sentFrames = new ArrayList<>();
    private final int windowSize = 4;  // Example window size
    private Timer timer;

    public void connect(String host, int port) throws IOException {

        socket = new Socket(host, port);
        System.out.println("Connected to receiver at " + host + ":" + port);
    }

    public void sendFrames(String fileName) throws IOException {
        fileReader = new BufferedReader(new FileReader(fileName));
        String line;
        int frameNum = 0;
        while ((line = fileReader.readLine()) != null) {
            String data = line;
            Frame frame = new Frame("I", frameNum, data, "");
            String crc = CRC.calculateFrameCRC(frame);
            frame.setCrc(crc);
            sendFrame(frame);
            frameNum = (frameNum + 1);  // Frame number on 3 bits (0-7)
        }
        sendFrame(new Frame("F", 0, null, ""));  // End of transmission frame
    }

    private void sendFrame(Frame frame) throws IOException {
        OutputStream out = socket.getOutputStream();
        String outputFrame=frame.toByteString();
        out.write(outputFrame.getBytes());
        /*out.flush();*/
        sentFrames.add(frame);
        System.out.println("Sent: " + outputFrame);
        /*startTimer();*/
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
