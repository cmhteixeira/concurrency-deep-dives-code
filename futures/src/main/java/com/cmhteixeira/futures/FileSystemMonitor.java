package com.cmhteixeira.futures;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.monitor.*;

public class FileSystemMonitor {

  private FileSystemMonitor() {}

  public static CompletableFuture<Path> monitorFolder(String dir, String pattern) {
    try {
      return internal(dir, pattern);
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  private static CompletableFuture<Path> internal(String dir, String pattern) throws Exception {
    CompletableFuture<Path> cF1 = new CompletableFuture<>();

    var monitor = new FileAlterationMonitor();
    var observer = new FileAlterationObserver(dir, new WildcardFileFilter(pattern));

    observer.addListener(
        new FileAlterationListenerAdaptor() {
          @Override
          public void onFileCreate(final File file) {
            cF1.complete(file.toPath()); // the important part
          }
        });
    monitor.addObserver(observer);
    monitor.start();

    return cF1;
  }
}
