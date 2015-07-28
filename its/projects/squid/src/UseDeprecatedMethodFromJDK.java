import java.util.Date;

public class UseDeprecatedMethodFromJDK {
  public void use() {
    // was deprecated as of JDK version 1.1
    new Date().getDate(); // violation
  }
}
