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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientConnection implements Runnable {

  private Logger LOG = LoggerFactory.getLogger(getClass());
  private Socket clientSocket;
  private ApplicationState state;

  private final Configuration conf;

  public ClientConnection(Socket clientSocket, ApplicationState state, Configuration conf) {
    this.clientSocket = clientSocket;
    this.state = state;
    this.conf = conf;
  }

  public ClientConnection(Socket clientSocket, ApplicationState state) {
    this.clientSocket = clientSocket;
    this.state = state;
    this.conf = new Configuration(1024);
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
        process(tunnelDestination(requestLine.path()));
      }
      case HttpVerb.GET get -> {
        if (requestLine.path().equals("stats")) {
          LOG.info("Giving stats ...");
          out.write(state.statsPerTunnelDomainShow().getBytes(StandardCharsets.UTF_8));
        } else {
          LOG.info("Not a CONNECT HTTP message: '{}'. Headers: {}", requestLine, headerString);
          clientSocket.close();
        }
      }

      default -> {
        LOG.info("Not a CONNECT HTTP message: '{}'. Headers: {}", requestLine, headerString);
        clientSocket.close();
      }
    }
  }

  private void process(InetSocketAddress tunnelDestination) throws IOException {
    com.cmhteixeira.java.sockets.blocking.httpproxy.Proxy proxy =
        new com.cmhteixeira.java.sockets.blocking.httpproxy.Proxy(
            (InetSocketAddress) clientSocket.getRemoteSocketAddress(),
            tunnelDestination,
            UUID.randomUUID().toString());

    state.clientConnected(proxy, System.currentTimeMillis());

    Socket tunnelSocket = new Socket();
    try {
      tunnelSocket.connect(tunnelDestination);
    } catch (Exception e) {
      LOG.info("Could not connect to tunnel.", e);
      state.closeProxy(
          proxy, System.currentTimeMillis(), new ProxyState.Closed.Reason.CouldNotConnectTunnel());
    }

    state.tunnelConnected(proxy);

    clientSocket
        .getOutputStream()
        .write(new ConnectResponse(200, "Connection Established").toBytes());
    new Proxy(tunnelSocket, proxy).start();

    byte[] buffer = new byte[conf.bufferSize];
    while (true) {
      int bytesRead = tunnelSocket.getInputStream().read(buffer);
      if (bytesRead == -1) {
        LOG.info("End of stream reading from tunnel destination for {}", proxy);
        clientSocket.close();
        tunnelSocket.close();
        state.closeProxy(proxy, System.currentTimeMillis(), new ProxyState.Closed.Reason.Tunnel());
        return;
      }
      LOG.debug("Read {} bytes from tunnel {}", bytesRead, tunnelSocket.getInetAddress());
      clientSocket.getOutputStream().write(buffer, 0, bytesRead);
      state.bytesIntoClient(proxy, bytesRead);
      LOG.debug("Sent bytes to client destination {}", clientSocket.getInetAddress());
    }
  }

  private class Proxy extends Thread {

    Socket tunnelSocket;
    com.cmhteixeira.java.sockets.blocking.httpproxy.Proxy proxy;

    Proxy(Socket tunnelSocket, com.cmhteixeira.java.sockets.blocking.httpproxy.Proxy proxy) {
      this.tunnelSocket = tunnelSocket;
      this.proxy = proxy;
    }

    public void runInternal() throws IOException {
      byte[] buffer = new byte[conf.bufferSize];
      while (true) {
        int bytesRead = clientSocket.getInputStream().read(buffer);
        if (bytesRead == -1) {
          LOG.info("End of stream from client {}", proxy);
          clientSocket.close();
          tunnelSocket.close();
          state.closeProxy(
              proxy, System.currentTimeMillis(), new ProxyState.Closed.Reason.Client());
          return;
        }
        LOG.debug("Read {} bytes from client {}", bytesRead, clientSocket.getInetAddress());
        tunnelSocket.getOutputStream().write(buffer, 0, bytesRead);
        state.bytesIntoTunnel(proxy, bytesRead);
        LOG.debug("Sent bytes to tunnel destination {}", tunnelSocket.getInetAddress());
      }
    }

    @Override
    public void run() {
      try {
        runInternal();
      } catch (IOException e) {
        LOG.info("Finished {}.", proxy);
      }
    }
  }

  public record Configuration(int bufferSize) {}

  @Override
  public void run() {
    try {
      runInternal();
    } catch (IOException e) {
      LOG.info("Finished {}.", clientSocket.getRemoteSocketAddress());
    }
  }
}
