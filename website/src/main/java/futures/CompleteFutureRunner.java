package futures;

import java.util.concurrent.CompletableFuture;

public class CompleteFutureRunner {
  public static void main(String[] args) {
    StdInputMonitor stdInputMonitor = new StdInputMonitor();
    CompletableFuture<String> passwordF = stdInputMonitor.monitor("password=");

    passwordF.whenCompleteAsync(
        (password, ex) -> {
          if (ex != null) ex.printStackTrace();
          else System.out.println("Password is: " + password);
        });
  }
}
