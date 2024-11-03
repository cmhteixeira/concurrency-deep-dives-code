package com.cmhteixeira.sockets.nonblocking.httpproxy;

import com.cmhteixeira.sockets.nonblocking.crawler.GET;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
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

  private static int SIZE_BUFFERS = 10_000;
  private URL webpage;
  private SSLEngine sslEngine;
  private SocketChannel socketChannel;

  private Selector selector;

  private ByteBuffer readPlainBuffer;
  private ByteBuffer readEncryptedBuffer;
  private ByteBuffer writePlainBuffer;
  private ByteBuffer writeEncryptedBuffer;

  public TlsNegotiation(SSLEngine sslEngine, URL webpage, SocketChannel socketChannel)
      throws IOException {
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
    this.webpage = webpage;
    this.selector = Selector.open();
  }

  public void run() throws IOException {
    //    this.socketChannel.register(this.selector, SelectionKey.OP_WRITE, SelectionKey.OP_READ);

    GET get = new GET(webpage.getHost(), "/");
    writePlainBuffer.put(get.encodeHttpRequest().getBytes(StandardCharsets.UTF_8));
    sslEngine.setUseClientMode(true);
    System.out.println("Is connected? " + socketChannel.isConnected());

    SSLEngineResult sslEngineResult = sslEngine.wrap(writePlainBuffer, writeEncryptedBuffer);
    writeEncryptedBuffer.flip();
    write();
    recursive(sslEngineResult.getHandshakeStatus(), sslEngineResult.getStatus(), 0);
  }

  private void recursive(HandshakeStatus handshakeStatus, Status status, int iter)
      throws IOException {
    if (iter > 15) return;
    System.out.println("############################");
    System.out.printf("Status=%s + HandshakeStatus=%s\n", status, handshakeStatus);
    if (status == OK && handshakeStatus == FINISHED) {
      System.out.println("Read: FINISHED");
    } else if (status == OK && handshakeStatus == NEED_TASK) {
      System.out.println("Read: NEED TASK");
      Runnable task = sslEngine.getDelegatedTask(); // should not block current thread
      task.run();
      recursive(sslEngine.getHandshakeStatus(), OK, iter + 1);
    } else if (status == OK && handshakeStatus == NOT_HANDSHAKING) {
      System.out.println("Read: NOT HANDSHAKING");
    } else if (status == OK && handshakeStatus == NEED_UNWRAP) {
      System.out.println("Read: NEED UNWRAP");
      if (!(readEncryptedBuffer.hasRemaining() && readEncryptedBuffer.position() != 0)) {
        System.out.println("Reading ...");
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
}
