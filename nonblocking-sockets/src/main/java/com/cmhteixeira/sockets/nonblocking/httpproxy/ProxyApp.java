package com.cmhteixeira.sockets.nonblocking.httpproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class ProxyApp {

  Selector selector;
  Map<SelectionKey, SocketWrapper> sockets = new HashMap<>();

  sealed interface SocketWrapper {
    record ListeningSocket(ServerSocketChannel ssC) implements SocketWrapper {}

    record AcceptedSocket(SocketChannel asC) implements SocketWrapper {}

    record ConnectingSocket(SocketChannel asC) implements SocketWrapper {}
  }

  private void connectKey(ServerSocketChannel serverChannel) {
    try {
      SocketChannel socketChannel = serverChannel.accept();
      socketChannel.configureBlocking(false);
      SelectionKey key =
          socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
      sockets.put(key, new SocketWrapper.AcceptedSocket(socketChannel));
    } catch (Exception e) {
      System.out.println("Hi there");
    }
  }

  ProxyApp() throws IOException {
    selector = Selector.open();
  }

  private void run() throws IOException {

    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress("localhost", 5545));
    serverSocketChannel.configureBlocking(false);
    SelectionKey acceptKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    sockets.put(acceptKey, new SocketWrapper.ListeningSocket(serverSocketChannel));

    while (true) {
      //            selector.selectedKeys()
      selector.select(
          selectorKey -> {
            SocketWrapper socketWrapper = sockets.get(selectorKey);
            if (socketWrapper == null) throw new RuntimeException("kabom!");
            if (socketWrapper instanceof SocketWrapper.ListeningSocket)
              connectKey(((SocketWrapper.ListeningSocket) socketWrapper).ssC);

            if (socketWrapper instanceof SocketWrapper.AcceptedSocket) {}
            if (socketWrapper instanceof SocketWrapper.ConnectingSocket) {}
          });
    }

    //    System.out.println("Hello world!");
  }

  public static void main(String[] args) throws IOException {
    ProxyApp proxyApp = new ProxyApp();
    proxyApp.run();
  }
}
