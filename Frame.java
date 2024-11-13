public class Frame {
        private static final String FLAG = "01111110";
        private char type;
        private int num;
        private String data;
        private String crc;

        public Frame(char type, int num, String data, String crc) {
            this.type = type;
            this.num = num;
            this.data = data;
            this.crc = crc;
        }

        public String toByteString() {
            // Convert the frame to a bit string representation
            StringBuilder sb = new StringBuilder();
            sb.append(FLAG);
            sb.append(type);
            sb.append((char) num);
            if (data != null) sb.append(data);
            sb.append(crc);
            sb.append(FLAG);
            return sb.toString();
        }

        // Getters and setters for frame fields
        public char getType() { return type; }
        public int getNum() { return num; }
        public String getData() { return data; }
        public String getCrc() { return crc; }


}
