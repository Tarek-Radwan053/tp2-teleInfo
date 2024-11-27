import java.io.*;
import java.net.*;
import java.util.*;
import java.net.Socket;

public class Sender {
    private Socket socket; // Socket pour établir la connexion avec le récepteur
    private BufferedReader fileReader; // Lecture des données du fichier à envoyer
    private List<Frame> sentFrames = new ArrayList<>(); // Liste des trames envoyées
    private final int windowSize = 4; // Taille de la fenêtre pour le protocole Go-Back-N
    private int base = 0; // Base de la fenêtre (première trame non acquittée)
    private int nextSeqNum = 0; // Numéro de la prochaine trame à envoyer
    private Timer timer; // Timer pour gérer les délais d'attente des ACKs (non utilisé ici mais prévu)

    /**
     * Connecte l'émetteur au récepteur via un socket.
     *
     * @param host Adresse IP ou nom d'hôte du récepteur
     * @param port Port du récepteur
     * @throws IOException En cas d'erreur de connexion
     */
    public void connect(String host, int port) throws IOException {
        int retries = 5; // Nombre de tentatives de connexion
        int backoff = 3000; // Temps d'attente en millisecondes entre les tentatives

        while (retries > 0) {
            try {
                socket = new Socket(host, port); // Tente d'établir une connexion
                Frame connectFrame = new Frame("C", 0, "", ""); // Trame de demande de connexion
                String crc = CRC.calculateFrameCRC(connectFrame); // Calcul du CRC pour la trame
                connectFrame.setCrc(crc);
                sendFrame(connectFrame, true); // Envoie la trame de connexion
                System.out.println("Connected to receiver at " + host + ":" + port);
                return; // Connexion réussie
            } catch (IOException e) {
                System.err.println("Failed to connect to receiver. Retrying in " + backoff / 1000 + " seconds...");
                retries--; // Décrémente le nombre de tentatives restantes
                try {
                    Thread.sleep(backoff); // Attente avant la prochaine tentative
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Connection attempt interrupted", ie);
                }
            }
        }
        throw new IOException("Failed to connect to receiver after multiple attempts."); // Échec après toutes les tentatives
    }

    /**
     * Envoie les trames à partir d'un fichier texte en utilisant le protocole Go-Back-N.
     *
     * @param fileName Nom du fichier à lire
     * @throws IOException En cas d'erreur d'entrée/sortie
     */
    public void sendFrames(String fileName) throws IOException {
        fileReader = new BufferedReader(new FileReader(fileName));
        String line = fileReader.readLine();

        while ((line) != null || base < nextSeqNum) { // Continue tant qu'il y a des données ou des trames non acquittées
            // Envoie les trames tant que la fenêtre n'est pas pleine
            while (nextSeqNum < base + windowSize && line != null) {
                String data = line;
                Frame frame = new Frame("I", nextSeqNum, data, ""); // Crée une trame avec les données
                String crc = CRC.calculateFrameCRC(frame); // Calcul du CRC
                frame.setCrc(crc);
                line = fileReader.readLine(); // Lit la ligne suivante
                if (line == null) {
                    sendFrame(frame, true); // Envoie la dernière trame
                } else {
                    sendFrame(frame, false); // Envoie une trame normale
                }
                nextSeqNum++; // Incrémente le numéro de trame
            }

            int biggestFrame = -1; // Indique la plus grande trame acquittée
            for (int i = nextSeqNum; i > base; i--) {
                if (waitForAck(i)) { // Attente d'un ACK
                    biggestFrame = i;
                    break;
                }
            }

            if (biggestFrame == -1) { // Timeout pour toutes les trames
                System.out.println("Timeout! Resending frames starting from " + base);
                resendFrames(base); // Réenvoie les trames à partir de la base
            } else if (biggestFrame == nextSeqNum) { // Toutes les trames dans la fenêtre sont acquittées
                base = biggestFrame;
            } else { // Réenvoie les trames partiellement acquittées
                while (!waitForAck(nextSeqNum)) {
                    resendFrames(biggestFrame + 1);
                }
            }
        }

        // Envoie la trame de fin de communication
        Frame endFrame = new Frame("F", nextSeqNum, null, "");
        String endCrc = CRC.calculateFrameCRC(endFrame);
        endFrame.setCrc(endCrc);
        sendFrame(endFrame, false);
        System.out.println("Sent End of Communication (F) frame");
    }

    /**
     * Envoie une trame au récepteur.
     *
     * @param frame  La trame à envoyer
     * @param isLast Indique si c'est la dernière trame d'un lot
     * @throws IOException En cas d'erreur d'envoi
     */
    protected void sendFrame(Frame frame, boolean isLast) throws IOException {
        OutputStream out = socket.getOutputStream();
        String outputFrame = frame.toByteString();

        if (frame.getType().equals("F") || isLast || nextSeqNum + 1 >= base + windowSize) {
            outputFrame += "\n"; // Ajoute une fin de ligne si nécessaire
        }
        out.write(outputFrame.getBytes());
        out.flush();

        System.out.println("Sent: " + frame.getData()); // Log de la trame envoyée
        sentFrames.add(frame); // Ajoute la trame à la liste des trames envoyées
    }

    /**
     * Attend un ACK pour une trame avec un délai d'attente (timeout).
     *
     * @param frameNum Numéro de la trame à attendre
     * @return true si l'ACK est reçu, false sinon
     */
    private boolean waitForAck(int frameNum) {
        try {
            socket.setSoTimeout(3000); // Timeout de 3 secondes
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String ack = reader.readLine();
            Frame ackFrame = Frame.identifyFrame(ack, 1); // Décodage de la trame ACK

            if (ackFrame != null) {
                String unstuffedData = BitStuffing.removeBitStuffing(ackFrame.getData()); // Retire le bit stuffing
                Frame unstuffedFrame = new Frame(ackFrame.getType(), ackFrame.getNum(), unstuffedData, ackFrame.getCrc());
                frameNum--;
                if (CRC.validateCRC(unstuffedFrame)) { // Vérifie le CRC
                    if ("A".equals(unstuffedFrame.getType())) {
                        System.out.println("Received ACK for frame " + frameNum);
                        return true;
                    } else if ("R".equals(unstuffedFrame.getType())) {
                        System.err.println("Received REJ for frame " + frameNum);
                    } else {
                        System.err.println("Received unexpected frame type: " + unstuffedFrame.getType());
                    }
                } else {
                    System.err.println("CRC mismatch for frame " + frameNum);
                }
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout waiting for ACK for frame " + frameNum);
        } catch (IOException e) {
            System.err.println("Error receiving ACK: " + e.getMessage());
        }
        return false; // Retourne false si aucun ACK valide n'est reçu
    }

    /**
     * Réenvoie les trames à partir d'un numéro de trame donné.
     *
     * @param frameNbr Numéro de la première trame à réenvoyer
     * @throws IOException En cas d'erreur d'envoi
     */
    private void resendFrames(int frameNbr) throws IOException {
        for (int i = frameNbr; i < nextSeqNum; i++) {
            Frame frame = sentFrames.get(i);
            sendFrame(frame, i == nextSeqNum - 1); // Réenvoie la trame
        }
    }

    /**
     * Point d'entrée du programme. Initialise le Sender et commence l'envoi des trames.
     */
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
