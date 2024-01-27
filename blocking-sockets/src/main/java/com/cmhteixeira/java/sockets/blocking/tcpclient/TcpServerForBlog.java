package com.cmhteixeira.java.sockets.blocking.tcpclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

public class TcpServerForBlog {

  public static int receiveBufferSize = 4;
  public static String localAddress = "0";
  public static int localPort = 3081;
  public static int backlog = 1;

  public static void main(String[] args) throws IOException {
    ServerSocket serverSocket = new ServerSocket();
    serverSocket.setReceiveBufferSize(receiveBufferSize);
    serverSocket.bind(new InetSocketAddress(localAddress, localPort), backlog);

    Socket socket = serverSocket.accept();
    InputStream in = socket.getInputStream();
    while (true) {
      int mayChar = in.read();
      if (mayChar == -1) {
        System.out.println("Stream ended. Exiting.");
        System.exit(0);
      }
      System.out.print((char) mayChar);
    }
  }
}
