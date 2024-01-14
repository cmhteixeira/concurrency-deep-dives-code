package com.cmhteixeira.java.sockets.blocking.staticfileserver;

import com.cmhteixeira.java.http.HttpParsing;
import com.cmhteixeira.java.http.RequestLine;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import org.slf4j.LoggerFactory;

public class FileServer {
  public static void main(String[] args) throws IOException {
    var log = LoggerFactory.getLogger("BlockingSockets");
    List<NetworkInterface> interfaces = List.of();
    try {
      interfaces = NetworkInterface.networkInterfaces().toList();
    } catch (Exception e) {
      System.err.println("There was an error:");
      e.printStackTrace();
      System.exit(1);
    }

    for (NetworkInterface nInterface : interfaces) {
      System.out.println("#####");
      System.out.println("DisplayName = " + nInterface.getDisplayName());
      System.out.println("isLoopBack = " + nInterface.isLoopback());
      for (Iterator<InetAddress> it = nInterface.getInetAddresses().asIterator(); it.hasNext(); ) {
        InetAddress address = it.next();
        System.out.println("  IP = " + address.getHostAddress());
      }
    }

    Path currentDir = Paths.get("").toAbsolutePath();
    var serverSocket = new ServerSocket();
    serverSocket.bind(new InetSocketAddress("127.0.0.1", 8080));
    var clientSocket = serverSocket.accept();
    log.info("Connection accepted ...");

    var inputStream = clientSocket.getInputStream();
    var dataInputStream = new InputStreamReader(inputStream, StandardCharsets.US_ASCII);
    RequestLine requestLine = HttpParsing.parseRequestLine(dataInputStream);
    System.out.println(requestLine);
    Path requested = Paths.get("/").relativize(Paths.get(requestLine.path()));
    boolean isDir = Files.isDirectory(requested);
  }
}
