
import java.io.UnsupportedEncodingException;

public class Encoding {
    public String encode(String msg, boolean flag) {//When flag  is true there is a limit in 75 characters
        byte[] bytes = null;
        if (msg.isEmpty()) {
            return "";
        }
        try {
            bytes = msg.getBytes("ISO-8859-15");//Identify charset
        } catch (UnsupportedEncodingException ex) {
            return "";
        }

        //encoded message
        String encodedMessage = "";
        int total = 0;
        for (int i = 0; i < bytes.length; i++) {
            //If the character is a CR
            if (bytes[i] == 13) {
                encodedMessage = encodedMessage.concat("\n");
                total = 0;
                continue;
            //If next character is LF
            } else if (bytes[i] == 10) {
                continue;
            }
            //Convert message in Hexadecimal format
            encodedMessage = encodedMessage.concat("=" + Integer.toHexString(bytes[i] & 255).toUpperCase());

            if (total >= 72 && flag) {//limit reached for body concerning the 75 characters create new line
                encodedMessage = encodedMessage.concat("=\n");
                total = 0;
            }
            total += 3;//+3 for the encoded char
        }
        return encodedMessage;
    }
}