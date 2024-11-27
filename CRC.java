import java.util.Objects;

public class CRC {
    // Constantes pour le calcul du CRC-CCITT
    private static final int POLYNOMIAL = 0x1021;  // Polynôme CRC-CCITT (x^16 + x^12 + x^5 + 1)
    private static final int INITIAL_VALUE = 0xFFFF;  // Valeur initiale du registre CRC

    /**
     * Méthode pour calculer le checksum CRC-CCITT pour une chaîne donnée.
     *
     * @param input La chaîne d'entrée pour laquelle calculer le CRC
     * @return Le CRC calculé sous forme de chaîne hexadécimale (4 caractères)
     */
    public static String calculateCRC(String input) {
        int crc = INITIAL_VALUE;  // Initialise le registre CRC avec la valeur initiale

        for (char c : input.toCharArray()) {  // Parcourt chaque caractère de la chaîne
            crc ^= (c << 8);  // XOR les bits du caractère dans le registre CRC
            for (int i = 0; i < 8; i++) {  // Effectue 8 itérations (1 pour chaque bit du caractère)
                if ((crc & 0x8000) != 0) {  // Si le bit le plus significatif est 1
                    crc = (crc << 1) ^ POLYNOMIAL;  // Décale à gauche et XOR avec le polynôme
                } else {
                    crc = (crc << 1);  // Décale simplement à gauche
                }
            }
        }

        crc &= 0xFFFF;  // Garde les 16 bits de poids faible (CRC-16)
        return String.format("%04X", crc);  // Retourne le CRC sous forme de chaîne hexadécimale
    }

    /**
     * Valide le CRC d'une trame en comparant le CRC calculé avec le CRC fourni dans la trame.
     *
     * @param frame La trame à valider
     * @return true si le CRC est valide, false sinon
     */
    public static boolean validateCRC(Frame frame) {
        // Convertit le numéro de la trame en binaire (8 bits avec padding)
        String numBinary = String.format("%8s", Integer.toBinaryString(frame.getNum())).replace(' ', '0');

        // Construit la chaîne binaire combinée pour le calcul du CRC
        String binaryData = BitStuffing.stringToBinary(frame.getType()) + numBinary + BitStuffing.stringToBinary(frame.getData());

        // Calcule le CRC à partir des données combinées
        String calculatedCRC = calculateCRC(binaryData);

        // Convertit le CRC calculé en binaire pour la comparaison
        String calculatedCRCBinary = BitStuffing.stringToBinary(calculatedCRC);


        // Compare le CRC calculé avec celui de la trame
        return Objects.equals(calculatedCRCBinary, frame.getCrc());
    }

    /**
     * Calcule le CRC pour une trame entière (type, numéro et données).
     *
     * @param frame La trame pour laquelle calculer le CRC
     * @return Le CRC calculé sous forme de chaîne hexadécimale
     */
    public static String calculateFrameCRC(Frame frame) {
        // Concatène les parties de la trame : type, numéro et données
        String combinedData = frame.getType() + frame.getNum() + frame.getData();
        // Calcule le CRC sur les données combinées
        return calculateCRC(combinedData);
    }
}
