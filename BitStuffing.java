public class BitStuffing {

    /**
     * Applique le bit stuffing à une chaîne binaire. Cela ajoute un '0' après cinq '1' consécutifs.
     *
     * @param data La chaîne binaire d'entrée
     * @return La chaîne avec le bit stuffing appliqué
     */
    public static String applyBitStuffing(String data) {
        StringBuilder stuffedData = new StringBuilder(); // Stocke les données après bit stuffing
        int consecutiveOnes = 0; // Compteur pour suivre les '1' consécutifs

        for (char bit : data.toCharArray()) { // Parcourt chaque bit dans la chaîne
            stuffedData.append(bit); // Ajoute le bit actuel
            if (bit == '1') {
                consecutiveOnes++;
                if (consecutiveOnes == 5) {
                    stuffedData.append('0'); // Insère un '0' après cinq '1'
                    consecutiveOnes = 0; // Réinitialise le compteur
                }
            } else {
                consecutiveOnes = 0; // Réinitialise si le bit est '0'
            }
        }

        return stuffedData.toString(); // Retourne la chaîne avec bit stuffing
    }

    /**
     * Retire le bit stuffing d'une chaîne binaire. Supprime tout '0' inséré après cinq '1'.
     *
     * @param stuffedData La chaîne binaire avec bit stuffing
     * @return La chaîne d'origine sans bit stuffing
     */
    public static String removeBitStuffing(String stuffedData) {
        StringBuilder originalData = new StringBuilder(); // Stocke les données après suppression du bit stuffing
        int consecutiveOnes = 0; // Compteur pour suivre les '1' consécutifs

        for (int i = 0; i < stuffedData.length(); i++) { // Parcourt chaque bit dans la chaîne
            char bit = stuffedData.charAt(i);
            originalData.append(bit); // Ajoute le bit actuel à la chaîne originale

            if (bit == '1') {
                consecutiveOnes++;
                if (consecutiveOnes == 5) {
                    // Si cinq '1' consécutifs sont détectés, vérifie le bit suivant
                    if (i + 1 < stuffedData.length() && stuffedData.charAt(i + 1) == '0') {
                        i++; // Ignore le '0' inséré par le bit stuffing
                    }
                    consecutiveOnes = 0; // Réinitialise le compteur
                }
            } else {
                consecutiveOnes = 0; // Réinitialise si le bit est '0'
            }
        }

        return originalData.toString(); // Retourne la chaîne sans bit stuffing
    }

    /**
     * Convertit une chaîne de caractères en sa représentation binaire.
     *
     * @param input La chaîne de caractères à convertir
     * @return La représentation binaire de la chaîne
     */
    public static String stringToBinary(String input) {
        StringBuilder binaryRepresentation = new StringBuilder(); // Stocke la chaîne binaire
        byte[] byteArray = input.getBytes(); // Convertit la chaîne en tableau d'octets

        for (byte b : byteArray) {
            // Convertit chaque octet en une chaîne binaire (8 bits), complétée par des zéros à gauche si nécessaire
            String binaryString = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            binaryRepresentation.append(binaryString); // Ajoute la chaîne binaire au résultat final
        }

        return binaryRepresentation.toString(); // Retourne la chaîne binaire
    }
}
