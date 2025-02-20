package com.cmhteixeira.sockets.nonblocking.httpclient;

import com.cmhteixeira.sockets.nonblocking.httpproxy.TlsNegotiation;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import rawhttp.core.*;

public class HttpClientImpl implements HttpClient {

  private final SSLContext sslContext;

  public HttpClientImpl(SSLContext sslContext) {
    this.sslContext = sslContext;
  }

  @Override
  public RawHttpResponse<Void> send(RawHttpRequest req) throws IOException {
    SSLEngine sslEngine = configureSSLEngine(req.getUri().getHost());
    SocketChannel socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(true);
    socketChannel.connect(new InetSocketAddress(req.getUri().getHost(), req.getUri().getPort()));
    TlsNegotiation tlsNegotiation = new TlsNegotiation(sslEngine, socketChannel);
    tlsNegotiation.write(ByteBuffer.wrap(req.toString().getBytes(StandardCharsets.UTF_8)));
    RawHttp rwaHttp = new RawHttp();
    return rwaHttp.parseResponse(tlsNegotiation.getInputStream());
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
