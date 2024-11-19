import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Receiver {

    private int nbrFrames=0;
    private ServerSocket serverSocket;
    private List<Frame> receivedFrames = new ArrayList<>();

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
        System.out.println("Receiver listening on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Connection accepted from " + clientSocket.getInetAddress());

            // Set a timeout for reading from the socket
            /*clientSocket.setSoTimeout(5000);  // 5 seconds timeout*/
                InputStream in = clientSocket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line="";
                while ((line = reader.readLine()) != null) {
                    nbrFrames=countFlags( line,"01111110");
                    nbrFrames=nbrFrames/2;
                    for (int frameNbr=1;frameNbr<nbrFrames+1;frameNbr++) {
                        Frame frame = identifyFRame(line, frameNbr);
                        if (checkErrors(frame)) {
                            sendAck(clientSocket, frame.getNum());
                        } else {
                            sendRejection(clientSocket, frame.getNum());
                        }
                    }

                }
            }
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
    public Frame identifyFRame(String line, int frameNbr) {
        String FLAG = "01111110";  // The flag that marks the start and end of each frame

        // Calculate the start and end positions of the frame
        int indexFlag = line.indexOf(FLAG, (frameNbr - 1) * FLAG.length());
        int indexFlag2 = line.indexOf(FLAG, frameNbr * FLAG.length());

        // Ensure valid flag positions were found
        if (indexFlag == -1 || indexFlag2 == -1) {
            System.out.println("Error: Invalid flag positions.");
            return null;  // If no valid flags found, return null (invalid frame)
        }

        // Extract the frame content (data and CRC part) between the flags
        String frameContent = line.substring(indexFlag + FLAG.length(), indexFlag2);

        // We assume the frame structure to be:
        // type: 1 char
        // num: 1 char (or int)
        // data: variable length string
        // crc: last 4 chars as CRC

        if (frameContent.length() < 12) {
            System.out.println("Error: Frame content too small.");
            return null;  // If frame content is too small, it's invalid
        }

        // Extract type, num, data, and crc from the frame content
        char type = frameContent.charAt(0);  // First byte: type
        int num = frameContent.charAt(1);    // Second byte: num
        String data = frameContent.substring(2, frameContent.length() - 4);  // Data section



        // not sure if i shoud unstef this or the data it self?
        //

        String crc=frameContent.substring(frameContent.length() - 4);

        // Return the frame object
        return new Frame(type, num, data, crc);
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
