import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Receiver {

    private int nbrFrames = 0; // Nombre de trames détectées dans une communication
    private int endt = 0; // Indicateur de fin de communication
    private int expectedFrameNum = 0; // Numéro de trame attendu (Go-Back-N ARQ)
    private int windowSize = 1; // Taille de la fenêtre (Go-Back-N ARQ)

    private ServerSocket serverSocket; // Socket serveur pour accepter les connexions des expéditeurs

    public static void main(String[] args) throws IOException {
        // Vérifie que le port est passé en argument
        if (args.length != 1) {
            System.out.println("Usage: java Receiver <port>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]); // Récupère le port à utiliser
        Receiver receiver = new Receiver();
        receiver.start(port); // Démarre le serveur sur le port donné
    }

    /**
     * Démarre le serveur et gère les connexions entrantes.
     *
     * @param port Le port sur lequel le serveur écoute
     * @throws IOException En cas d'erreur de communication
     */
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(60000); // Timeout après 60 secondes sans connexion
        System.out.println("Receiver listening on port " + port);

        while (true) {
            try (Socket clientSocket = serverSocket.accept()) { // Accepte une connexion client
                System.out.println("Connection accepted from " + clientSocket.getInetAddress());

                // Traitement de la trame de connexion (type "C")
                if (!processConnectionFrame(clientSocket)) {
                    System.err.println("Invalid connection frame. Closing connection.");
                    clientSocket.close();
                    continue; // Passe à la prochaine connexion
                }

                // Traite les données envoyées par l'expéditeur
                processIncomingData(clientSocket);

                // Si fin de communication, arrête le serveur
                if (endt == 1) {
                    break;
                }
            } catch (IOException e) {
                System.err.println("Error handling client connection: " + e.getMessage());
            }
        }
    }

    /**
     * Traite la trame de connexion initiale envoyée par l'expéditeur.
     *
     * @param clientSocket Le socket client
     * @return true si la trame est valide, false sinon
     */
    private boolean processConnectionFrame(Socket clientSocket) throws IOException {
        InputStream in = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = reader.readLine();

        if (line != null) {
            Frame frame = Frame.identifyFrame(line, 1); // Identifie la trame reçue
            if (frame != null && "C".equals(frame.getType()) && CRC.validateCRC(frame)) {
                System.out.println("Received valid connection frame.");
                sendAck(clientSocket, frame.getNum()); // Envoie un ACK pour la trame
                return true;
            }
        }
        return false;
    }

    /**
     * Traite les données envoyées par l'expéditeur.
     *
     * @param clientSocket Le socket client
     */
    private void processIncomingData(Socket clientSocket) throws IOException {
        InputStream in = clientSocket.getInputStream();
        Scanner scanner = new Scanner(in); // Scanner pour lire les données
        System.out.println("Receiving data from sender...");

        // Traite chaque ligne reçue
        while (scanner.hasNext()) {
            String line = scanner.nextLine(); // Lit une ligne de données
            System.out.println("line: " + line);

            // Compte les trames détectées dans la ligne
            nbrFrames = countFlags(line, "01111110") / 2;
            System.out.println("Detected " + nbrFrames + " frames.");

            // Traite chaque trame détectée
            for (int frameNbr = 1; frameNbr <= nbrFrames; frameNbr++) {
                Frame frame = Frame.identifyFrame(line, frameNbr); // Identifie la trame
                if (frame == null) {
                    System.err.println("Frame " + frameNbr + " is invalid. Skipping...");
                    continue;
                }

                // Vérifie si c'est une trame de fin de communication (type "F")
                if ("F".equals(frame.getType())) {
                    System.out.println("Received End of Communication (F) frame.");
                    String unstuffedData = BitStuffing.removeBitStuffing(frame.getData());
                    Frame unstuffedFrame = new Frame(frame.getType(), frame.getNum(), unstuffedData, frame.getCrc());

                    if (CRC.validateCRC(unstuffedFrame)) {
                        System.out.println("End of Communication frame is valid. Closing connection.");
                        endt = 1;
                        return;
                    } else {
                        System.err.println("CRC mismatch for End of Communication frame. Closing connection.");
                    }
                }

                // Vérifie si c'est une trame de P-bit
                if ("P".equals(frame.getType())) {
                    System.out.println("Received P-bit frame.");
                    String unstuffedData = BitStuffing.removeBitStuffing(frame.getData());
                    Frame unstuffedFrame = new Frame(frame.getType(), frame.getNum(), unstuffedData, frame.getCrc());

                    if (CRC.validateCRC(unstuffedFrame)) {
                        System.out.println("P-bit frame is valid.");
                        // Handle the P-bit frame as needed
                    } else {
                        System.err.println("CRC mismatch for P-bit frame.");
                    }
                }

                // Vérifie si la trame est la bonne
                if (frame.getNum() == expectedFrameNum) {
                    if (checkErrors(frame)) {
                        System.out.println("Received valid frame: " + frame.getNum());
                        expectedFrameNum = (expectedFrameNum + 1); // Avance la fenêtre
                    } else {
                        sendRejection(clientSocket, frame.getNum());
                    }
                } else {
                    sendRejection(clientSocket, frame.getNum()); // Rejette les trames hors de la fenêtre
                }
            }

            sendAck(clientSocket, expectedFrameNum); // Envoie un ACK cumulatif
            if (endt == 1) {
                break;
            }
        }

        System.out.println("All frames received and processed.");
    }

    /**
     * Compte les occurrences d'un flag dans une ligne.
     *
     * @param line La ligne à analyser
     * @param flag Le flag à rechercher
     * @return Le nombre de flags trouvés
     */
    public int countFlags(String line, String flag) {
        int count = 0;
        int index = 0;

        // Cherche le flag jusqu'à la fin de la ligne
        while ((index = line.indexOf(flag, index)) != -1) {
            count++;
            index += flag.length();
        }

        return count;
    }

    /**
     * Vérifie les erreurs d'une trame via le CRC.
     *
     * @param frame La trame à vérifier
     * @return true si la trame est valide, false sinon
     */
    private boolean checkErrors(Frame frame) {
        boolean isValid = CRC.validateCRC(frame);
        if (!isValid) {
            System.err.println("CRC mismatch for frame: " + frame.getNum());
        }
        return isValid;
    }

    /**
     * Envoie un ACK (accusé de réception) pour une trame donnée.
     *
     * @param clientSocket Le socket client
     * @param frameNum Le numéro de trame à confirmer
     */
    private void sendAck(Socket clientSocket, int frameNum) {
        try {
            if (clientSocket.isClosed() || clientSocket.isOutputShutdown()) {
                System.err.println("Cannot send ACK. Socket is closed or output is shut down.");
                return;
            }

            Frame ackFrame = new Frame("A", frameNum, "", "");
            OutputStream out = clientSocket.getOutputStream();
            ackFrame.setCrc(CRC.calculateFrameCRC(ackFrame));
            String outputFrame = ackFrame.toByteString() + "\n"; // Prépare la trame
            out.write(outputFrame.getBytes());
            out.flush();
            frameNum--; // Décrémente le numéro de trame pour l'affichage
            System.out.println("Sent ACK for frame " + frameNum);
        } catch (IOException e) {
            System.err.println("Error sending ACK for frame " + frameNum + ": " + e.getMessage());
        }
    }

    /**
     * Envoie un rejet (REJ) pour une trame donnée.
     *
     * @param clientSocket Le socket client
     * @param frameNum Le numéro de trame rejetée
     * @throws IOException En cas d'erreur d'écriture
     */
    private void sendRejection(Socket clientSocket, int frameNum) throws IOException {
        Frame rejFrame = new Frame("R", frameNum, null, "");
        OutputStream out = clientSocket.getOutputStream();

        rejFrame.setCrc(CRC.calculateFrameCRC(rejFrame));
        String outputFrame = rejFrame.toByteString() + "\n";
        out.write(outputFrame.getBytes());
        out.flush();
        System.out.println("Sent REJ for frame " + frameNum);
    }
}
