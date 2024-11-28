import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Frame {
    private static final String FLAG = "01111110"; // Indique le début et la fin d'une trame
    private String type;  // Type de trame ('I' pour information, 'A' pour ACK, etc.)
    private int num;    // Numéro de séquence de la trame
    private String data;  // Données contenues dans la trame
    private String crc;   // Somme de contrôle CRC pour vérifier l'intégrité

    public Frame(String type, int num, String data, String crc) {
        this.type = type;
        this.num = num;
        this.data = data;
        this.crc = crc;
    }

    /**
     * Identifie et extrait une trame spécifique dans une chaîne binaire en fonction de son numéro.
     *
     * @param line      La chaîne binaire contenant plusieurs trames.
     * @param frameNbr  Le numéro de la trame à extraire.
     * @return          Une instance de Frame si la trame est valide, sinon null.
     */
    public static Frame identifyFrame(String line, int frameNbr) {
        String FLAG = "01111110";  // Marqueur de début et de fin des trames

        // Recherche les positions des flags dans la chaîne
        List<Integer> flagPositions = new ArrayList<>();
        int index = 0;
        while ((index = line.indexOf(FLAG, index)) != -1) {
            flagPositions.add(index);
            index += FLAG.length();
        }

        // Vérifie s'il y a suffisamment de flags pour former une trame
        if (flagPositions.size() < 2) {
            System.out.println("Erreur : Pas assez de flags pour former une trame.");
            return null;
        }

        // Vérifie si le numéro de la trame demandé est valide
        if (frameNbr < 1 || frameNbr >= flagPositions.size()) {
            System.out.println("Erreur : Numéro de trame invalide.");
            return null;
        }

        // Détermine les indices de début et de fin de la trame dans la chaîne
        int startIdx;
        int endIdx;
        if (frameNbr == 1) {
            endIdx = flagPositions.get(frameNbr);
            startIdx = 0 + FLAG.length();
        } else {
            endIdx = flagPositions.get(frameNbr * 2 - 1);
            if (frameNbr < 3) {
                startIdx = flagPositions.get(frameNbr) + FLAG.length();
            } else {
                startIdx = flagPositions.get(frameNbr + frameNbr - 2) + FLAG.length();
            }
        }

        // Extrait le contenu de la trame
        String frameContent = line.substring(startIdx, endIdx);

        // Étape 1 : Retire le bourrage de bits (bit stuffing) du contenu
        String unstuffedContent = BitStuffing.removeBitStuffing(frameContent);

        // Vérifie que le contenu désencapsulé est suffisant
        if (unstuffedContent.length() < 32) { // Minimum requis : type, numéro et CRC
            System.out.println("Erreur : Contenu de la trame trop court après désencapsulation.");
            return null;
        }

        // Étape 3 : Décoder le type de la trame (8 bits)
        String typeBinary = unstuffedContent.substring(0, 8);
        String type = String.valueOf((char) Integer.parseInt(typeBinary, 2));

        // Étape 4 : Décoder le numéro de la trame (8 bits)
        String numBinary = unstuffedContent.substring(8, 16);
        int num = Integer.parseInt(numBinary, 2);

        // Étape 5 : Extraire les données (avant les derniers 16 bits pour le CRC)
        String dataBinary = unstuffedContent.substring(16, unstuffedContent.length() - 32);
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < dataBinary.length(); i += 8) {
            String byteSegment = dataBinary.substring(i, Math.min(i + 8, dataBinary.length()));
            data.append((char) Integer.parseInt(byteSegment, 2));
        }

        // Étape 6 : Calculer et valider le CRC
        String crc = unstuffedContent.substring(0, unstuffedContent.length() - 32);
        crc = CRC.calculateCRC(crc);
        crc = BitStuffing.stringToBinary(crc);

        // Retourne une nouvelle instance de Frame avec les données extraites
        return new Frame(type, num, data.toString(), crc);
    }

    /**
     * Convertit la trame en une représentation binaire avec les flags et le bourrage de bits.
     *
     * @return La représentation binaire de la trame, prête à être envoyée.
     */
    public String toByteString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type);

        // Transforme le numéro en binaire sur 8 bits
        String numBinary = "00000000";
        if (type == "I" || type == "A" || type == "F") {
            numBinary = String.format("%8s", Integer.toBinaryString(num)).replace(' ', '0');
        }

        // Ajoute les données (si présentes)
        if (data != null) {
            sb.append(data);
        }

        // Convertit tout en binaire
        String allBinary = BitStuffing.stringToBinary(sb.toString());
        allBinary = allBinary.substring(0, 8) + numBinary + allBinary.substring(8);

        // Calcule et ajoute le CRC
        String crc = CRC.calculateCRC(allBinary);
        crc = BitStuffing.stringToBinary(crc);
        allBinary = allBinary + crc;

        // Applique le bourrage de bits
        allBinary = BitStuffing.applyBitStuffing(allBinary);

        // Ajoute les flags
        return FLAG + allBinary + FLAG;
    }

    /**
     * Takes a binary string and extracts the frame's type, sequence number, data, and CRC to create a new Frame object.
     */
    public static Frame fromByteString(String byteString) {
        // Assuming the byteString format is: type + num + data + crc
        String type = byteString.substring(0, 1);
        int num = Integer.parseInt(byteString.substring(1, 2));
        String data = byteString.substring(2, byteString.length() - 4);
        String crc = byteString.substring(byteString.length() - 4);
        return new Frame(type, num, data, crc);
    }

    // Getters
    public String getType() { return type; }
    public int getNum() { return num; }
    public String getData() { return data; }
    public String getCrc() { return crc; }

    // Setters
    public void setCrc(String crc) {
        this.crc = crc;
    }

    public void setData(String data) {
        this.data = data;
    }
}
