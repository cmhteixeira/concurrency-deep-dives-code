package com.cmhteixeira.sockets.nonblocking.httpclient;

import com.cmhteixeira.sockets.nonblocking.httpproxy.TlsNegotiation;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import rawhttp.core.*;

public class HttpClientImpl implements HttpClient {

  private final SSLContext sslContext;

  public HttpClientImpl(SSLContext sslContext) {
    this.sslContext = sslContext;
  }

  @Override
  public RawHttpResponse<Void> send(RawHttpRequest req) throws IOException {
    SSLEngine sslEngine = this.sslContext.createSSLEngine();
    sslEngine.setUseClientMode(true);
    SocketChannel socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(true);
    socketChannel.connect(new InetSocketAddress(req.getUri().getHost(), req.getUri().getPort()));
    TlsNegotiation tlsNegotiation = new TlsNegotiation(sslEngine, socketChannel);
    tlsNegotiation.write(ByteBuffer.wrap(req.toString().getBytes(StandardCharsets.UTF_8)));
    RawHttp rwaHttp = new RawHttp();
    return rwaHttp.parseResponse(tlsNegotiation.getInputStream());
  }
}
