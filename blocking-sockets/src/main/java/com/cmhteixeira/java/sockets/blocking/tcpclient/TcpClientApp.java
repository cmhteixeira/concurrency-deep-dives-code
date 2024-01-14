package com.cmhteixeira.java.sockets.blocking.tcpclient;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import picocli.CommandLine;

@CommandLine.Command(name = "TcpClient")
class TcpClientApp implements Callable<Integer> {

  @CommandLine.Option(names = "--local-address", description = "Local internet address to use.")
  private InetAddress localAddress;

  @CommandLine.Option(
      names = "--local-port",
      defaultValue = "0",
      description = "Local port to use. If not provided, underlying system picks ephemeral port.")
  private int localPort;

  @CommandLine.Option(names = "--address", description = "Remote address to send data to.")
  private InetAddress remoteAddress;

  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "Remote port to use.")
  private int remotePort;

  @CommandLine.Option(
      names = {"-O", "--buffer-size"},
      defaultValue = "10",
      converter = BufferSizeConverter.class,
      description = "Send buffer size in bytes. SendQ")
  private int sendBufferSize;

  @CommandLine.Option(
      names = "--interval",
      defaultValue = "1000",
      description = "Interval to sleep for in millis.")
  private int sleepInterval;

  static volatile boolean halt = true;

  private void printInputs() {
    System.out.println("Input");
    System.out.println(" SendBufferSize= " + sendBufferSize);
    System.out.println(" LocalAddress= " + localAddress);
    System.out.println(" LocalPort= " + localPort);
    System.out.println(" RemoteAddress= " + remoteAddress);
    System.out.println(" RemotePort= " + remotePort);
    System.out.println();
  }

  @Override
  public Integer call() throws Exception {
    printInputs();
    Socket socket = new Socket();
    socket.bind(new InetSocketAddress(localAddress, localPort));
    socket.setSendBufferSize(sendBufferSize);
    socket.connect(new InetSocketAddress(remoteAddress, remotePort));
    System.out.println("Connected. SendBuffer=" + socket.getSendBufferSize());

    OutputStreamWriter out =
        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);

    new Thread(
            () -> {
              Scanner controlStream = new Scanner(System.in);
              while (true) {
                controlStream.nextLine();
                if (halt) {
                  System.out.println("Continuing ...");
                  halt = false;
                } else {
                  System.out.println("Stopping ...");
                  halt = true;
                }
              }
            })
        .start();

    char[] f =
        IntStream.rangeClosed('A', 'Z')
            .mapToObj(c -> "" + (char) c)
            .collect(Collectors.joining())
            .toCharArray();

    for (int i = 0; ; ++i) {
      if (!halt) {
        char charIteration = f[i % f.length];
        String toSend = String.valueOf(charIteration).repeat(70);
        System.out.print("Sending: " + toSend);
        out.write(toSend + "\n");
        out.flush();
        System.out.println("   ✅");
      }
      Thread.sleep(sleepInterval);
    }
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new TcpClientApp()).execute(args);
    System.exit(exitCode);
  }
}
