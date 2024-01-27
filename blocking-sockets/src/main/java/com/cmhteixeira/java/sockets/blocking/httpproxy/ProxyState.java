package com.cmhteixeira.java.sockets.blocking.httpproxy;

public sealed interface ProxyState
    permits ProxyState.Closed, ProxyState.Open, ProxyState.TunnelUnEstablished {

  long bytesIntoTunnel();

  long bytesIntoClient();

  record TunnelUnEstablished(long startConnection) implements ProxyState {
    Closed close(long timeStamp) {
      return new Closed(
          startConnection, timeStamp, 0, 0, new Closed.Reason.CouldNotConnectTunnel());
    }

    Open establish() {
      return new Open(startConnection, 0, 0);
    }

    @Override
    public long bytesIntoTunnel() {
      return 0;
    }

    @Override
    public long bytesIntoClient() {
      return 0;
    }
  }

  record Open(long startConnection, long bytesIntoTunnel, long bytesIntoClient)
      implements ProxyState {

    Open bytesIntoTunnel(long numBytes) {
      return new Open(startConnection, bytesIntoTunnel + numBytes, bytesIntoClient);
    }

    Open bytesIntoClient(long numBytes) {
      return new Open(startConnection, bytesIntoTunnel, bytesIntoClient + numBytes);
    }

    Closed close(long timeStamp, Closed.Reason reason) {
      return new Closed(startConnection, timeStamp, bytesIntoTunnel, bytesIntoClient, reason);
    }
  }

  record Closed(
      long startConnection,
      long endConnection,
      long bytesIntoTunnel,
      long bytesIntoClient,
      Reason reason)
      implements ProxyState {
    public sealed interface Reason {
      record Tunnel() implements Reason {}

      record Client() implements Reason {}

      record CouldNotConnectTunnel() implements Reason {}
    }
  }
}
