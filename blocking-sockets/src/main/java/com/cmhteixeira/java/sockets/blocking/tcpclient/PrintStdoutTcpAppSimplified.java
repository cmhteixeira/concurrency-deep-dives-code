package com.cmhteixeira.java.sockets.blocking.tcpclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "Print data onto stdout TCP server - Simplified")
public class PrintStdoutTcpAppSimplified implements Callable<Integer> {

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

  @CommandLine.Option(
      names = "--interval",
      defaultValue = "1000",
      description = "Interval to sleep for in millis.")
  private int sleepInterval;

  @CommandLine.Option(
      names = "--wait-before-accepting",
      defaultValue = "false",
      description = "Whether to wait for user to allow accepting the connection")
  private boolean waitBeforeAccepting;

  static volatile boolean halt = true;

  @Override
  public Integer call() throws Exception {
    ServerSocket serverSocket = new ServerSocket(localPort, backlog, localAddress);
    serverSocket.setReceiveBufferSize(receiveBufferSize);

    if (waitBeforeAccepting) {
      System.out.println("Press any button to accept connection.");
      System.in.read();
    }

    Socket socket = serverSocket.accept();
    socket.setReceiveBufferSize(receiveBufferSize);
    System.out.printf(
        "Connection accepted.\n Receive buffer= %d\n  RemoteAddress= %s\n  RemotePort= %d\n",
        socket.getReceiveBufferSize(), socket.getRemoteSocketAddress(), socket.getPort());

    InputStream inputStream = socket.getInputStream();

    new Thread(
            () -> {
              while (true) {
                try {
                  System.in.read();
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
                if (halt) {
                  halt = false;
                  System.out.println("Continuing ...");
                } else {
                  halt = true;
                  System.out.println("Stopping ...");
                }
              }
            })
        .start();

    while (true) {
      if (halt) {
        Thread.sleep(100);
      } else {
        int mayChar = inputStream.read();
        if (mayChar == -1) {
          System.out.println("Stream ended. -1 received from input stream. Exiting.");
          System.exit(0);
        }
        System.out.print((char) mayChar);
      }
    }
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new PrintStdoutTcpAppSimplified()).execute(args);
    System.exit(exitCode);
  }
}
