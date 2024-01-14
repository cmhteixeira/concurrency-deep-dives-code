package com.cmhteixeira.java.sockets.blocking.httpproxy;

import com.cmhteixeira.java.http.ConnectResponse;
import com.cmhteixeira.java.http.HttpParsing;
import com.cmhteixeira.java.http.HttpVerb;
import com.cmhteixeira.java.http.RequestLine;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connection implements Runnable {

  private Logger LOG = LoggerFactory.getLogger(getClass());
  private Socket clientSocket;

  public Connection(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  private InetSocketAddress tunnelDestination(String authorityRequestTarget) {
    Pattern p = Pattern.compile("^\\s*(.*?):(\\d+)\\s*$");
    Matcher m = p.matcher(authorityRequestTarget);
    if (m.matches()) {
      String host = m.group(1);
      int port = Integer.parseInt(m.group(2));
      return new InetSocketAddress(host, port);
    }
    throw new IllegalArgumentException(
        String.format("Authority target '%s' not valid.", authorityRequestTarget));
  }

  private void runInternal() throws IOException {
    InputStream in = clientSocket.getInputStream();
    OutputStream out = clientSocket.getOutputStream();
    InputStreamReader inputStreamReader = new InputStreamReader(in);
    RequestLine requestLine = HttpParsing.parseRequestLine(inputStreamReader);
    Map<String, String> headers = HttpParsing.parseHeaders(inputStreamReader);
    String headerString =
        headers.entrySet().stream()
            .map(entry -> entry.getKey() + ":" + entry.getValue())
            .collect(Collectors.joining(" # "));
    switch (requestLine.verb()) {
      case HttpVerb.CONNECT c -> {
        LOG.info("Received a CONNECT HTTP message: '{}'. Headers: {}", requestLine, headerString);
        foo(tunnelDestination(requestLine.path()));
      }
      default -> {
        LOG.info("Not a CONNECT HTTP message: '{}'. Headers: {}", requestLine, headerString);
        clientSocket.close();
      }
    }
  }

  private void foo(InetSocketAddress tunnelDestination) throws IOException {
    //    if (!tunnelDestination.getHostName().contains("twitter.com")) {
    //      clientSocket.close();
    //      LOG.info("Not a request to twitter.com: {}", tunnelDestination.getHostName());
    //      return;
    //    }
    Socket tunnelSocket = new Socket();
    tunnelSocket.connect(tunnelDestination);

    clientSocket
        .getOutputStream()
        .write(new ConnectResponse(200, "Connection Established").toBytes());
    new Outbound(tunnelSocket).start();

    byte[] buffer = new byte[1024];
    while (true) {
      int bytesRead = tunnelSocket.getInputStream().read(buffer);
      if (bytesRead == -1) {
        LOG.info(
            "End of stream reading from tunnel destination {}",
            tunnelSocket.getRemoteSocketAddress());
        clientSocket.close();
        tunnelSocket.close();
        break;
      }
      LOG.info("Read {} bytes from tunnel {}", bytesRead, tunnelSocket.getInetAddress());
      clientSocket.getOutputStream().write(buffer, 0, bytesRead);
      LOG.info("Sent bytes to client destination {}", clientSocket.getInetAddress());
    }
  }

  private class Outbound extends Thread {

    Socket tunnelSocket;

    Outbound(Socket tunnelSocket) {
      this.tunnelSocket = tunnelSocket;
    }

    public void runInternal() throws IOException {
      byte[] buffer = new byte[1024];
      while (true) {
        int bytesRead = clientSocket.getInputStream().read(buffer);
        if (bytesRead == -1) {
          LOG.info("End of stream reading from client {}", clientSocket.getRemoteSocketAddress());
          clientSocket.close();
          tunnelSocket.close();
          break;
        }
        LOG.info("Read {} bytes from client {}", bytesRead, clientSocket.getInetAddress());
        tunnelSocket.getOutputStream().write(buffer, 0, bytesRead);
        LOG.info("Sent bytes to tunnel destination {}", tunnelSocket.getInetAddress());
      }
    }

    @Override
    public void run() {
      try {
        runInternal();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void run() {
    try {
      runInternal();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
