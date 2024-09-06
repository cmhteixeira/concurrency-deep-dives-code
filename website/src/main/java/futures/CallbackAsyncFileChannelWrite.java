package futures;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

public class CallbackAsyncFileChannelWrite {

  static CompletableFuture<Integer> write(AsynchronousFileChannel fC, String contents) {
    CompletableFuture<Integer> cF1 = new CompletableFuture<>();
    fC.write(
        ByteBuffer.wrap(contents.getBytes()),
        0,
        null,
        new CompletionHandler<>() {
          public void completed(Integer result, Object attachment) {
            cF1.complete(result);
          }

          public void failed(Throwable exc, Object attachment) {
            cF1.completeExceptionally(exc);
          }
        });

    return cF1;
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    AsynchronousFileChannel fC =
        AsynchronousFileChannel.open(Paths.get(args[0]), StandardOpenOption.WRITE);
    CompletableFuture<Integer> cF1 = write(fC, "Concurrency Deep Dives");
    cF1.whenComplete(
        (numBytes, ex) -> {
          if (ex == null) System.out.printf("Finished writing %d bytes into the file\n", numBytes);
          else ex.printStackTrace();
        });
    Thread.sleep(1_000L);
  }
}
