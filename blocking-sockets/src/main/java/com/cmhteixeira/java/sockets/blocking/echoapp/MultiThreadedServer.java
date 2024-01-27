package com.cmhteixeira.java.sockets.blocking.echoapp;

import com.cmhteixeira.java.sockets.blocking.tcpclient.BufferSizeConverter;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "EchoServer")
public class MultiThreadedServer implements Callable<Integer> {
  @CommandLine.Option(
      names = "--address",
      defaultValue = "localhost",
      description = "Local internet address to use. Default: ${DEFAULT-VALUE}")
  private InetAddress localAddress;

  @CommandLine.Option(
      names = "--port",
      defaultValue = "8080",
      description = "Local port to use. Default=${DEFAULT-VALUE}")
  private int localPort;

  @CommandLine.Option(
      names = "--backlog",
      defaultValue = "1",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
  private int backlog;

  @CommandLine.Option(
      names = {"-I", "--buffer-size"},
      defaultValue = "10",
      converter = BufferSizeConverter.class,
      description = "Send buffer size in bytes. RecvQ")
  private Integer receiveBufferSize;

  private static class Connection extends Thread {

    private final Socket socket;
    private final byte[] buffer = new byte[1024];

    private Connection(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      try {
        while (true) {
          int bytesRead = socket.getInputStream().read(this.buffer);
          if (bytesRead == -1) return;
          socket.getOutputStream().write(this.buffer, 0, bytesRead);
        }
      } catch (Exception e) {
        System.out.printf("Error for socket %s finished \n.", socket.getRemoteSocketAddress());
      } finally {
        try {
          socket.close();
        } catch (IOException e) {
        }
      }
    }
  }

  @Override
  public Integer call() throws Exception {
    ServerSocket serverSocket = new ServerSocket(localPort);

    while (true) {
      Socket socket = serverSocket.accept();
      new Connection(socket).start();
    }
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new MultiThreadedServer()).execute(args);
    System.exit(exitCode);
  }
}
