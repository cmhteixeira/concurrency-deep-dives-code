package com.cmhteixeira.java.sockets.nonblocking.crawler;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Crawler {

  private ConcurrentLinkedQueue<URL> webPagesToCrawl = new ConcurrentLinkedQueue<>();
  private Map<SelectionKey, SocketChannel> connections = new HashMap<>();
  private int maxConcurrency = 10;

  private void run() throws IOException {
    Selector selector = Selector.open();

    SocketChannel socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(false);
    socketChannel.connect(null);
    socketChannel.register(selector, SelectionKey.OP_CONNECT);

    while (true) {
      Set<SelectionKey> keys = selector.selectedKeys();
      SelectionKey selectionKey = keys.stream().findFirst().get();
      SocketChannel socketChannel2 = connections.get(selectionKey);
      if (selectionKey.isConnectable()) {
        if (socketChannel2.finishConnect()) {
          socketChannel2.register(selector, SelectionKey.OP_WRITE);
        } else {
        }
        break;
      }
      if (selectionKey.isWritable()) {
        break;
      }
      if (selectionKey.isReadable()) {
        break;
      }
    }
  }

  public static void main(String[] args)
      throws IOException,
          NoSuchAlgorithmException,
          KeyManagementException,
          KeyStoreException,
          CertificateException {
    TrustManagerFactory tmF = TrustManagerFactory.getInstance("PKIX");
    KeyStore keystore = KeyStore.getInstance("JKS");
    keystore.load(
        new FileInputStream(
            Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts").toFile()),
        "changeit".toCharArray());
    tmF.init(keystore);

    SecureRandom random = new SecureRandom();
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, tmF.getTrustManagers(), random);

    SSLEngine sslEngine = sslContext.createSSLEngine();
    sslEngine.setUseClientMode(true);

    SocketChannel socket = SocketChannel.open();
    socket.connect(new InetSocketAddress("ft.com", 443));

    Foo foo = new Foo(sslEngine, socket);
    GET getRequest = new GET("www.ft.com", "/");
    foo.write(getRequest.encodeHttpRequest().getBytes(StandardCharsets.UTF_8));
  }
}
