package com.cmhteixeira.sockets.nonblocking.httpproxy;

import com.cmhteixeira.sockets.nonblocking.crawler.GET;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;
import static javax.net.ssl.SSLEngineResult.Status;
import static javax.net.ssl.SSLEngineResult.Status.*;

public class TlsNegotiation {

  private OverallStatus overallStatus;

  private static int SIZE_BUFFERS = 10_000;
  //  private URL webpage;
  private SSLEngine sslEngine;
  private SocketChannel socketChannel;

  private Selector selector;

  private ByteBuffer readPlainBuffer;
  private ByteBuffer readEncryptedBuffer;
  private ByteBuffer writePlainBuffer;
  private ByteBuffer writeEncryptedBuffer;

  public TlsNegotiation(SSLEngine sslEngine, SocketChannel socketChannel) throws IOException {
    this.sslEngine = sslEngine;
    this.socketChannel = socketChannel;
    this.readPlainBuffer =
        ByteBuffer.allocateDirect(sslEngine.getSession().getApplicationBufferSize());
    this.readEncryptedBuffer =
        ByteBuffer.allocateDirect(sslEngine.getSession().getPacketBufferSize());
    this.writePlainBuffer =
        ByteBuffer.allocateDirect(sslEngine.getSession().getApplicationBufferSize());
    this.writeEncryptedBuffer =
        ByteBuffer.allocateDirect(sslEngine.getSession().getPacketBufferSize());
    //    this.webpage = webpage;
    this.selector = Selector.open();
    this.overallStatus = OverallStatus.NEGOTIATING;
  }

  public String send(GET req) throws IOException {
    if (overallStatus == OverallStatus.NEGOTIATING) start();

    System.out.println("##################");
    System.out.println("Finished negotiation ...");
    System.out.println("Plain read: " + readPlainBuffer);
    System.out.println("Encrypted read: " + readEncryptedBuffer);
    System.out.println("Plain write: " + writePlainBuffer);
    System.out.println("Encrypted write: " + writeEncryptedBuffer);
    System.out.println("Sending ...");
    writeEncryptedBuffer.clear(); // yes
    writePlainBuffer.clear(); // yes
    writePlainBuffer.put(req.encodeHttpRequest().getBytes(StandardCharsets.UTF_8));
    writePlainBuffer.flip();
    SSLEngineResult res = sslEngine.wrap(writePlainBuffer, writeEncryptedBuffer);
    System.out.println(res);
    writeEncryptedBuffer.flip();
    write();
    readEncryptedBuffer.compact();
    readPlainBuffer.clear();
    read();
    readEncryptedBuffer.flip();
    System.out.println(readEncryptedBuffer);
    System.out.println("Plain read before unwrapping: " + readPlainBuffer);
    SSLEngineResult res2 = sslEngine.unwrap(readEncryptedBuffer, readPlainBuffer);
    System.out.println("Plain read after unwrapping: " + readPlainBuffer);
    System.out.println(readEncryptedBuffer);
    System.out.println(res2);
    SSLEngineResult res3 = sslEngine.unwrap(readEncryptedBuffer, readPlainBuffer);
    System.out.println("Plain read after unwrapping: " + readPlainBuffer);
    System.out.println(readEncryptedBuffer);
    System.out.println(res3);
    readPlainBuffer.flip();
    byte[] foo = new byte[readPlainBuffer.limit()];
    readPlainBuffer.get(foo);
    var t = new String(foo, StandardCharsets.UTF_8);
    System.out.printf("Result: %s\n", t);
    return t;
  }

  public void start() throws IOException {
    sslEngine.setUseClientMode(true);

    SSLEngineResult sslEngineResult = sslEngine.unwrap(readEncryptedBuffer, readPlainBuffer);
    recursive(sslEngineResult.getHandshakeStatus(), sslEngineResult.getStatus(), 0);
  }

  private void recursive(HandshakeStatus handshakeStatus, Status status, int iter)
      throws IOException {
    if (iter > 20) {
      System.out.println("Max iterations: " + iter);
      return;
    }
    System.out.println("############################");
    System.out.printf("Status=%s + HandshakeStatus=%s\n", status, handshakeStatus);
    if (status == OK && handshakeStatus == FINISHED) {
      System.out.println("Read: FINISHED");
      overallStatus = OverallStatus.NORMAL;
    } else if (status == OK && handshakeStatus == NEED_TASK) {
      System.out.println("Read: NEED TASK");
      Runnable task = sslEngine.getDelegatedTask(); // should not block current thread
      task.run();
      recursive(sslEngine.getHandshakeStatus(), OK, iter + 1);
    } else if (status == OK && handshakeStatus == NOT_HANDSHAKING) {
      System.out.println("Read: NOT HANDSHAKING");
    } else if (status == BUFFER_UNDERFLOW && handshakeStatus == NEED_UNWRAP) {
      System.out.println("Read. UNDERFLOW. NEED UNWRAP");
      System.out.println("Before compacting: " + readEncryptedBuffer);
      readEncryptedBuffer.compact();
      System.out.println("After compacting: " + readEncryptedBuffer);
      read();
      readEncryptedBuffer.flip();
      System.out.println("After flipping: " + readEncryptedBuffer);
      SSLEngineResult sslEngineResult = sslEngine.unwrap(readEncryptedBuffer, readPlainBuffer);
      System.out.printf("sslEngineResult: %s\n", sslEngineResult);
      System.out.println(readEncryptedBuffer);
      recursive(sslEngineResult.getHandshakeStatus(), sslEngineResult.getStatus(), iter + 1);
    } else if (status == OK && handshakeStatus == NEED_UNWRAP) {
      System.out.println("Read: NEED UNWRAP");
      if (!(readEncryptedBuffer.hasRemaining() && readEncryptedBuffer.position() != 0)) {
        System.out.println("Reading ...");
        readEncryptedBuffer.clear();
        read();
        readEncryptedBuffer.flip();
      }
      SSLEngineResult sslEngineResult = sslEngine.unwrap(readEncryptedBuffer, readPlainBuffer);
      System.out.printf("sslEngineResult: %s\n", sslEngineResult);
      System.out.println(readEncryptedBuffer);
      recursive(sslEngineResult.getHandshakeStatus(), sslEngineResult.getStatus(), iter + 1);
    } else if (status == OK && handshakeStatus == NEED_UNWRAP_AGAIN) {
      System.out.println("Read: NEED UNWRAP AGAIN");
    } else if (status == OK && handshakeStatus == NEED_WRAP) {
      System.out.println("Read: NEED WRAP");
      writeEncryptedBuffer.clear();
      writePlainBuffer.clear();
      SSLEngineResult sslEngineResult = sslEngine.wrap(writePlainBuffer, writeEncryptedBuffer);
      writeEncryptedBuffer.flip();
      write();
      System.out.println(sslEngineResult);
      recursive(sslEngineResult.getHandshakeStatus(), sslEngineResult.getStatus(), iter + 1);
    } else if (status == CLOSED) {
      System.out.println("Read: CLOSED");
    } else if (status == BUFFER_UNDERFLOW) {
      System.out.println("Read: BUFFER UNDERFLOW");
      read();
      SSLEngineResult sslEngineResult =
          sslEngine.unwrap(readEncryptedBuffer.flip(), readPlainBuffer);
      System.out.println(sslEngineResult);
      recursive(sslEngineResult.getHandshakeStatus(), sslEngineResult.getStatus(), iter + 1);
    } else if (status == BUFFER_OVERFLOW) {
      System.out.println("Read: BUFFER OVERFLOW");
    }
  }

  private void write() throws IOException {
    System.out.printf("Before writing: %s\n", writeEncryptedBuffer);
    int bytesWritten = socketChannel.write(writeEncryptedBuffer);
    System.out.printf("After writing %d: %s\n", bytesWritten, writeEncryptedBuffer);
  }

  private void read() throws IOException {
    System.out.printf("Before reading: %s\n", readEncryptedBuffer);
    int bytesRead = socketChannel.read(readEncryptedBuffer);
    System.out.printf("After reading %d: %s\n", bytesRead, readEncryptedBuffer);
  }

  private enum OverallStatus {
    NORMAL,
    NEGOTIATING;
  }
}
