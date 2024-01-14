package com.cmhteixeira.java.sockets.blocking.httpproxy;

import com.cmhteixeira.java.sockets.blocking.tcpclient.BufferSizeConverter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(name = "HttpProxyApp")
public class HttpProxyApp implements Callable<Integer> {

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

  private Logger LOG = LoggerFactory.getLogger(getClass());

  @Override
  public Integer call() throws Exception {
    ServerSocket serverSocket = new ServerSocket();
    serverSocket.bind(new InetSocketAddress(localAddress, localPort), backlog);

    while (true) {
      Socket socket = serverSocket.accept();
      LOG.info(
          "Connection accepted. RemoteAddress={}, RemotePort={}",
          socket.getInetAddress(),
          socket.getPort());
      Thread thread =
          new Thread(
              new Connection(socket), String.format("socket-%s", socket.getLocalSocketAddress()));
      thread.start();
    }
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new HttpProxyApp()).execute(args);
    System.exit(exitCode);
  }
}
