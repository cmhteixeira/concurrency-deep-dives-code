package com.cmhteixeira.sockets.nonblocking.httpclient;

import com.cmhteixeira.sockets.nonblocking.httpproxy.SecureClientSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import javax.net.ssl.*;

import rawhttp.core.*;

public class HttpClientImpl implements HttpClient {

  private final SSLContext sslContext;
  private final Integer bufferSizeAppRead;
  private final Integer bufferSizePacketRead;
  private final Integer bufferSizePacketWrite;

  public HttpClientImpl(SSLContext sslContext) {
    this.sslContext = sslContext;
    this.bufferSizeAppRead = null; // defaults will be used
    this.bufferSizePacketRead = null; // defaults will be used
    this.bufferSizePacketWrite = null; // defaults will be used
  }

  public HttpClientImpl(
      SSLContext sslContext,
      int bufferSizeAppRead,
      int bufferSizePacketRead,
      int bufferSizePacketWrite) {
    this.sslContext = sslContext;
    this.bufferSizeAppRead = bufferSizeAppRead;
    this.bufferSizePacketRead = bufferSizePacketRead;
    this.bufferSizePacketWrite = bufferSizePacketWrite;
  }

  @Override
  public RawHttpResponse<Void> send(RawHttpRequest req) throws IOException {
    SSLEngine sslEngine = configureSSLEngine(req.getUri().getHost());
    SecureClientSocket secureClientSocket =
        new SecureClientSocket(
            sslEngine,
            new InetSocketAddress(req.getUri().getHost(), req.getUri().getPort()),
            bufferSizeAppRead,
            bufferSizePacketRead,
            bufferSizePacketWrite);
    InputStream in = secureClientSocket.getInputStream();
    OutputStream out = secureClientSocket.getOutputStream();
    req.writeTo(out);
    RawHttp rwaHttp = new RawHttp();
    return rwaHttp.parseResponse(in);
  }

  private SSLEngine configureSSLEngine(String host) {
    SSLEngine sslEngine = this.sslContext.createSSLEngine();
    SSLParameters sslParams = sslEngine.getSSLParameters();
    sslParams.setServerNames(List.of(new SNIHostName(host)));
    sslEngine.setSSLParameters(sslParams);
    sslEngine.setUseClientMode(true);
    return sslEngine;
  }
}
