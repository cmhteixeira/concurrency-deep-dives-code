package com.cmhteixeira.java.sockets.blocking.echoapp;

import java.net.*;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "EchoServer")
public class OneConnectionServer implements Callable<Integer> {
  @CommandLine.Option(
      names = "--port",
      defaultValue = "8080",
      description = "Local port to use. Default=${DEFAULT-VALUE}")
  private int localPort;

  @Override
  public Integer call() throws Exception {
    byte[] buffer = new byte[1024];

    ServerSocket serverSocket = new ServerSocket(localPort);
    Socket socket = serverSocket.accept();

    while (true) {
      int bytesRead = socket.getInputStream().read(buffer);
      if (bytesRead == -1) return 0;
      socket.getOutputStream().write(buffer, 0, bytesRead);
    }
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new OneConnectionServer()).execute(args);
    System.exit(exitCode);
  }
}
