package futures;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class FutureBridge {

  private static CompletableFuture<Void> passwordCreated(File dir) {
    CompletableFuture<Void> passwordFileExists = new CompletableFuture<>();

    FileAlterationMonitor monitor = new FileAlterationMonitor(1000L);
    FileAlterationObserver observer = new FileAlterationObserver(dir);
    observer.addListener(
        new FileAlterationListenerAdaptor() {
          @Override
          public void onFileCreate(final File file) {
            if (file.getName().equals("password.txt")) passwordFileExists.complete(null);
          }
        });
    monitor.addObserver(observer);
    try {
      monitor.start();
    } catch (Exception e) {
      return CompletableFuture.failedFuture(
          new RuntimeException("Failed to start file monitor", e));
    }

    return passwordFileExists;
  }

  public static void main(String[] args) {
    File dir = new File(args[0]);
    if (!dir.isDirectory()) System.exit(-1);

    CompletableFuture<Void> passwordCreated = passwordCreated(dir);

    passwordCreated.whenCompleteAsync(
        (ignored, ex) -> {
          if (ex != null) ex.printStackTrace();
          else System.out.println("Password file created!");
        });
  }
}
