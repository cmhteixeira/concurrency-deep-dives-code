package com.cmhteixeira.java.sockets.blocking.echoapp;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "EchoServer")
public class SingleThreadDummyAttempt implements Callable<Integer> {
  @CommandLine.Option(
      names = "--port",
      defaultValue = "8080",
      description = "Local port to use. Default=${DEFAULT-VALUE}")
  private int localPort;

  @Override
  public Integer call() throws Exception {
    byte[] buffer = new byte[1024];

    ServerSocket serverSocket = new ServerSocket(localPort);

    List<Socket> connections = new ArrayList<>();
    int connectionIndex = 0;
    while (true) {
      Socket newSocket = serverSocket.accept();
      connections.add(newSocket);
      Socket socket = connections.get(connectionIndex);
      int bytesRead = socket.getInputStream().read(buffer);
      if (bytesRead == -1) System.exit(0);
      socket.getOutputStream().write(buffer, 0, bytesRead);
      if (connectionIndex == connections.size() - 1) connectionIndex = 0;
      else connectionIndex = connectionIndex + 1;
    }
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new SingleThreadDummyAttempt()).execute(args);
    System.exit(exitCode);
  }
}
