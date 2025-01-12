package com.cmhteixeira.sockets.nonblocking.httpclient;

import com.cmhteixeira.sockets.nonblocking.httpproxy.TlsNegotiation;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import rawhttp.core.*;

public class HttpClientImpl implements HttpClient {

  private final SSLContext sslContext;
  private final SSLEngine sslEngine;

  public HttpClientImpl(SSLContext sslContext) {
    this.sslContext = sslContext;
    this.sslEngine = this.sslContext.createSSLEngine();
    this.sslEngine.setUseClientMode(true);
  }

  @Override
  public RawHttpResponse<Void> send(RawHttpRequest req) throws IOException {
    SocketChannel socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(true);
    socketChannel.connect(new InetSocketAddress(req.getUri().getHost(), req.getUri().getPort()));
    TlsNegotiation tlsNegotiation = new TlsNegotiation(this.sslEngine, socketChannel);
    String res = tlsNegotiation.send(req.toString());
    RawHttp rwaHttp = new RawHttp();
    return rwaHttp.parseResponse(res);
  }

  @Override
  public CompletionStage<RawHttpResponse<Void>> sendAsync(RawHttpRequest req) {
    return CompletableFuture.failedFuture(new UnsupportedOperationException("Not yet implemented"));
  }
}
