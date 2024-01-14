package com.cmhteixeira.java.sockets.blocking.tcpclient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import picocli.CommandLine;

@CommandLine.Command(name = "TcpServer")
public class EchoServerApp implements Callable<Integer> {

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

  static AtomicInteger read = new AtomicInteger(0);
  static volatile boolean halt = true;

  @Override
  public Integer call() throws Exception {

    ServerSocket serverSocket = new ServerSocket(localPort, backlog, localAddress);
    serverSocket.setReceiveBufferSize(receiveBufferSize);

    Scanner controlStream = new Scanner(System.in, StandardCharsets.UTF_8);

    if (waitBeforeAccepting) {
      System.out.println("Press any button to accept connection.");
      controlStream.nextLine();
    }

    Socket socket = serverSocket.accept();
    socket.setReceiveBufferSize(receiveBufferSize);
    System.out.printf(
        "Connection accepted.\n Receive buffer= %d\n  RemoteAddress= %s\n  RemotePort= %d",
        socket.getReceiveBufferSize(), socket.getRemoteSocketAddress(), socket.getPort());

    InputStream inputStream = socket.getInputStream();
    InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

    new Thread(
            () -> {
              while (true) {
                String control = controlStream.nextLine();
                if (halt) {
                  halt = false;
                  System.out.println("Continuing ...");
                } else {
                  halt = true;
                  System.out.println("Halting ...");
                }
              }
            })
        .start();

    while (true) {
      if (read.get() == 0) {
        Thread.sleep(100);
      } else {
        int mayChar = streamReader.read();
        if (mayChar == -1) {
          System.out.println("Stream ended. -1 received from input stream. Exiting.");
          System.exit(0);
        }
        read.accumulateAndGet(2, (current, provided) -> current - provided);
        System.out.print((char) mayChar);
      }
    }
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new EchoServerApp()).execute(args);
    System.exit(exitCode);
  }
}
