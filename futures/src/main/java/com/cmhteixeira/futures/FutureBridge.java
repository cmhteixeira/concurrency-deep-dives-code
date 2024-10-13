package com.cmhteixeira.futures;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.io.monitor.*;

public class FutureBridge {

  /**
   * Notification when file `password.txt` is created inside {@code dir}.
   *
   * @param dir File system directory we want to monitor.
   * @return Future that completes when (and if) a file is created within {@code dir}.
   */
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
