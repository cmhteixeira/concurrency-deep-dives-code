package com.cmhteixeira.java.sockets.blocking.echoapp;

import com.cmhteixeira.java.sockets.blocking.tcpclient.BufferSizeConverter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "EchoServer")
public class SingleThreadWithAvailableServer implements Callable<Integer> {
  @CommandLine.Option(
      names = "--address",
      defaultValue = "0",
      description = "Local internet address to use. Default: ${DEFAULT-VALUE}")
  private InetAddress localAddress;

  @CommandLine.Option(
      names = "--port",
      defaultValue = "3081",
      description = "Local port to use. Default=${DEFAULT-VALUE}")
  private int localPort;

  @CommandLine.Option(
      names = "--backlog",
      defaultValue = "1",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
  private int backlog;

  @CommandLine.Option(
      names = {"-I", "--buffer-size-receive"},
      defaultValue = "10",
      converter = BufferSizeConverter.class,
      description = "Receive buffer size in bytes. RecvQ")
  private Integer receiveBufferSize;

  @CommandLine.Option(
      names = {"-O", "--buffer-size-send"},
      defaultValue = "10",
      converter = BufferSizeConverter.class,
      description = "Send buffer size in bytes. SendQ")
  private Integer sendBufferSize;

  @CommandLine.Option(
      names = "--accept-timeout",
      defaultValue = "1000",
      description = "Time in millis to accept a connection")
  private int acceptTimeout;

  @CommandLine.Option(
      names = "--read-timeout",
      defaultValue = "1000",
      description = "Time in millis to read from a socket")
  private int readTimeout;

  @Override
  public Integer call() throws Exception {

    ServerSocket serverSocket = new ServerSocket();
    serverSocket.setReceiveBufferSize(receiveBufferSize);
    serverSocket.bind(new InetSocketAddress(localAddress, localPort), backlog);

    List<Socket> connections = new ArrayList();
    byte[] buffer = new byte[1024];
    int connectionIndex = 0;
    while (true) {
      Socket socket;
      try {
        if (connections.isEmpty()) serverSocket.setSoTimeout(0);
        socket = serverSocket.accept();
        socket.setReceiveBufferSize(receiveBufferSize);
        socket.setSendBufferSize(sendBufferSize);
        socket.setSoTimeout(readTimeout);
        serverSocket.setSoTimeout(acceptTimeout);
      } catch (Exception e) {
        socket = null;
      }

      if (socket != null) {
        connections.add(socket);
        System.out.println("Socket added ...");
      }

      if (connections.isEmpty()) break;

      Socket s = connections.get(connectionIndex);

      if (s.getInputStream().available() <= 0) {
        connectionIndex = nextIndex(connectionIndex, connections.size());
        continue;
      }

      int numBytesRead;
      try {
        numBytesRead = s.getInputStream().read(buffer);
      } catch (SocketTimeoutException e) {
        connectionIndex = nextIndex(connectionIndex, connections.size());
        continue;
      }

      if (numBytesRead == -1) {
        System.out.printf("Removing %s from socket list ...\n", s.getRemoteSocketAddress());
        connections.remove(s);
        connectionIndex = Math.min(connectionIndex, connections.size() - 1);
        continue;
      }
      s.getOutputStream().write(buffer, 0, numBytesRead);

      connectionIndex = nextIndex(connectionIndex, connections.size());
    }

    return -1;
  }

  private int nextIndex(int current, int size) {
    if (current == size - 1) return 0;
    else return current + 1;
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new SingleThreadWithAvailableServer()).execute(args);
    System.exit(exitCode);
  }
}
