package com.cmhteixeira.sockets.nonblocking.httpclient;

import java.io.IOException;
import rawhttp.core.RawHttpRequest;
import rawhttp.core.RawHttpResponse;

public interface HttpClient {
  RawHttpResponse<Void> send(RawHttpRequest req) throws IOException, InterruptedException;
}
