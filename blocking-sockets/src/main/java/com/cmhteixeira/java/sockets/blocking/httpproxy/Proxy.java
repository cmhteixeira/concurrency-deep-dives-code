package com.cmhteixeira.java.sockets.blocking.httpproxy;

import java.net.InetSocketAddress;

public record Proxy(InetSocketAddress client, InetSocketAddress tunnel, String uuid) {}
