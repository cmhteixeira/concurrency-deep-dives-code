package futures;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class FileSystemMonitor {
  private static final FileAlterationMonitor fileMonitor = new FileAlterationMonitor();

  static {
    try {
      fileMonitor.start();
    } catch (Exception e) {
      throw new RuntimeException("Something went wrong starting the file monitor", e);
    }
  }

  public static CompletableFuture<Path> monitorFolder(String pattern) {
    CompletableFuture<Path> cF1 = new CompletableFuture<>();
    //    FileAlterationObserver fileObserver =
    //        new FileAlterationObserver(
    //            "/home/cmhteixeira/", WildcardFileFilter.builder().setWildcards(pattern).get());
    FileAlterationObserver fileObserver =
        new FileAlterationObserver("/home/cmhteixeira/", (new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return false;
            }

            @Override
            public boolean accept(File dir, String name) {
                return false;
            }
        });
    fileMonitor.addObserver(fileObserver);
    fileObserver.addListener(
        new FileAlterationListenerAdaptor() {
          @Override
          public void onFileCreate(final File file) {
            cF1.complete(file.toPath());
          }
        });
    return cF1;
  }
}
