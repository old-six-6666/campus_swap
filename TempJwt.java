import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
public class TempJwt {
  public static void main(String[] args) {
    String secret = "campus_swap_secret_key_must_be_at_least_32_characters";
    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    String token = Jwts.builder()
      .setSubject("1000000")
      .setIssuedAt(new Date())
      .setExpiration(new Date(System.currentTimeMillis() + 86400000L))
      .signWith(key, SignatureAlgorithm.HS256)
      .compact();
    System.out.println(token);
  }
}
