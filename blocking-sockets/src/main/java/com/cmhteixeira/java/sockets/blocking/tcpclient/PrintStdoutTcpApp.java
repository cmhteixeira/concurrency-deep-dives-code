package com.cmhteixeira.java.sockets.blocking.tcpclient;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import picocli.CommandLine;

@CommandLine.Command(name = "Print data onto stdout TCP server.")
public class PrintStdoutTcpApp implements Callable<Integer> {

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

  static AtomicReference<Status> statusRef = new AtomicReference<>(new Status.Continue());

  sealed interface Status permits Status.Continue, Status.Halt {
    record Continue() implements Status {}

    record Halt(int bytesToRead) implements Status {}
  }

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
        "Connection accepted.\n  Receive buffer= %d\n  RemoteAddress= %s\n  RemotePort= %d\n\n\n\n",
        socket.getReceiveBufferSize(), socket.getRemoteSocketAddress(), socket.getPort());

    InputStream inputStream = socket.getInputStream();

    new Thread(
            () -> {
              while (true) {
                String control = controlStream.nextLine();
                Status status = statusRef.get();
                switch (status) {
                  case PrintStdoutTcpApp.Status.Continue cont -> {
                    statusRef.compareAndSet(status, new Status.Halt(0));
                    System.out.println("Stopping ...");
                  }

                  case PrintStdoutTcpApp.Status.Halt halt -> {
                    if (halt.bytesToRead == 0) {
                      if (control.isBlank()) {
                        statusRef.compareAndSet(status, new Status.Continue());
                        System.out.println("Continuing ...");
                      } else {
                        int bytesToRead = Helper.bytesToRead2(control);
                        statusRef.compareAndSet(status, new Status.Halt(bytesToRead));
                      }
                    } else {
                      System.out.println("Not recognized.");
                    }
                  }
                }
              }
            })
        .start();

    while (true) {
      while (true) {
        Status status = statusRef.get();
        switch (status) {
          case PrintStdoutTcpApp.Status.Continue cont -> {
            int mayChar = inputStream.read();
            if (mayChar == -1) {
              System.out.println("Stream ended. -1 received from input stream. Exiting.");
              System.exit(0);
            }
            System.out.print((char) mayChar);
          }

          case PrintStdoutTcpApp.Status.Halt halt -> {
            if (halt.bytesToRead > 0) {
              int mayChar = inputStream.read();
              if (mayChar == -1) {
                System.out.println("Stream ended. -1 received from input stream. Exiting.");
                System.exit(0);
              }
              statusRef.compareAndSet(status, new Status.Halt(halt.bytesToRead - 1));
              System.out.print((char) mayChar);
            } else Thread.sleep(100);
          }
        }
      }
    }
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new PrintStdoutTcpApp()).execute(args);
    System.exit(exitCode);
  }
}
