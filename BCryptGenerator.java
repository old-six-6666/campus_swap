import cn.hutool.crypto.digest.BCrypt;

public class BCryptGenerator {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java BCryptGenerator <password>");
            return;
        }
        String password = args[0];
        String hashedPassword = BCrypt.hashpw(password);
        System.out.println("Hashed password: " + hashedPassword);
    }
}