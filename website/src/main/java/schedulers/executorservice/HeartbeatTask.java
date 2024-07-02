package schedulers.executorservice;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HeartbeatTask implements Runnable {

  private final Socket remoteSocket;
  private static final String keepAliveMessage = "stay-alive\n";

  public HeartbeatTask(Socket remoteSocket) {
    this.remoteSocket = remoteSocket;
  }

  @Override
  public void run() {
    try {
      remoteSocket.getOutputStream().write(keepAliveMessage.getBytes(StandardCharsets.UTF_8));
      System.out.printf("Sent " + keepAliveMessage);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
