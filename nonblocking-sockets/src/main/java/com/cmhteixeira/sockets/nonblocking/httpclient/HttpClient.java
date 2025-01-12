package com.cmhteixeira.sockets.nonblocking.httpclient;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import rawhttp.core.RawHttpRequest;
import rawhttp.core.RawHttpResponse;

public interface HttpClient {
  RawHttpResponse<Void> send(RawHttpRequest req) throws IOException, InterruptedException;

  CompletionStage<RawHttpResponse<Void>> sendAsync(RawHttpRequest req);
}
