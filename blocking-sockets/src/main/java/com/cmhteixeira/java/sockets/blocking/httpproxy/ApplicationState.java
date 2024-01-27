package com.cmhteixeira.java.sockets.blocking.httpproxy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationState {

  private Logger LOG = LoggerFactory.getLogger(getClass());

  private ConcurrentHashMap<Proxy, ProxyState> proxies = new ConcurrentHashMap<>();

  public ApplicationState() {}

  public void clientConnected(Proxy proxy, long timestamp) {
    proxies.put(proxy, new ProxyState.TunnelUnEstablished(timestamp));
  }

  public void tunnelConnected(Proxy proxy) {
    proxies.compute(
        proxy,
        (actualProxy, state) -> {
          if (actualProxy == null || state == null) {
            LOG.warn("Attempting to connect {} with no mapping.", actualProxy);
            return state;
          } else {
            return switch (state) {
              case ProxyState.TunnelUnEstablished unEstablished -> unEstablished.establish();
              case ProxyState.Open open -> {
                LOG.warn("Attempting to connect {} already in state {}.", actualProxy, state);
                yield state;
              }
              case ProxyState.Closed closed -> {
                LOG.warn("Attempting to connect {} already in state {}.", actualProxy, state);
                yield state;
              }
            };
          }
        });
  }

  public Map<String, TunnelStats> statsPerTunnelDomain() {
    Map<String, TunnelStats> res = new HashMap<>();

    proxies.forEach(
        (proxy, state) ->
            res.compute(
                proxy.tunnel().getHostName(),
                (hostName, tunnelStats) -> {
                  if (tunnelStats == null) {
                    return new TunnelStats(1, state.bytesIntoClient(), state.bytesIntoTunnel());
                  } else {
                    return new TunnelStats(
                        tunnelStats.numberTunnels + 1,
                        tunnelStats.totalBytesIn + state.bytesIntoClient(),
                        tunnelStats.totalBytesOut + state.bytesIntoTunnel());
                  }
                }));
    return res;
  }

  public String statsPerTunnelDomainShow() {
    StringBuilder strBuilder = new StringBuilder();
    for (Map.Entry<String, TunnelStats> entry : statsPerTunnelDomain().entrySet()) {
      String tunnelDes = entry.getKey();
      TunnelStats stats = entry.getValue();
      strBuilder.append(tunnelDes);
      strBuilder.append(String.format("  Number = %d\n", stats.numberTunnels));
      strBuilder.append(String.format("  bytesOut = %d\n", stats.totalBytesOut));
      strBuilder.append(String.format("  bytesIn = %d\n", stats.totalBytesIn));
      strBuilder.append("");
    }
    return strBuilder.toString();
  }

  public void bytesIntoTunnel(Proxy proxy, long numBytes) {
    proxies.compute(
        proxy,
        (actualProxy, state) -> {
          if (actualProxy == null || state == null) {
            LOG.warn("Registering bytes into tunnel {} but state is null", actualProxy);
            return state;
          } else {
            return switch (state) {
              case ProxyState.TunnelUnEstablished unEstablished -> {
                LOG.warn("Registering bytes into tunnel {} when in state {}.", actualProxy, state);
                yield state;
              }
              case ProxyState.Open open -> {
                yield open.bytesIntoTunnel(numBytes);
              }
              case ProxyState.Closed closed -> {
                LOG.warn("Registering bytes into tunnel {} when in state {}.", actualProxy, state);
                yield state;
              }
            };
          }
        });
  }

  public void bytesIntoClient(Proxy proxy, long numBytes) {
    proxies.compute(
        proxy,
        (actualProxy, state) -> {
          if (actualProxy == null || state == null) {
            LOG.warn("Registering bytes into client {} but state is null", actualProxy);
            return state;
          } else {
            return switch (state) {
              case ProxyState.TunnelUnEstablished unEstablished -> {
                LOG.warn("Registering bytes into client {} but state is {}", actualProxy, state);
                yield state;
              }
              case ProxyState.Open open -> open.bytesIntoClient(numBytes);
              case ProxyState.Closed closed -> {
                LOG.warn("Registering bytes into client {} but state is {}", actualProxy, state);
                yield state;
              }
            };
          }
        });
  }

  public void closeProxy(Proxy proxy, long timestamp, ProxyState.Closed.Reason reason) {
    proxies.computeIfPresent(
        proxy,
        (i, state) ->
            switch (state) {
              case ProxyState.TunnelUnEstablished unestablished -> unestablished.close(timestamp);
              case ProxyState.Open open -> open.close(timestamp, reason);
              case ProxyState.Closed closed -> {
                LOG.warn("Closing {}, when already {}", proxy, closed);
                yield closed;
              }
            });
  }

  private record TunnelStats(int numberTunnels, long totalBytesIn, long totalBytesOut) {}
}
