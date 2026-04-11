import cn.hutool.crypto.digest.BCrypt;
public class TmpBC {
  public static void main(String[] args) {
    System.out.println(BCrypt.checkpw("123456", "nopassword"));
  }
}
