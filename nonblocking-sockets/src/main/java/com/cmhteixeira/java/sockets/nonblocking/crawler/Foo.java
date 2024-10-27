package com.cmhteixeira.java.sockets.nonblocking.crawler;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

public class Foo {
  private int bufferSizes = 100_000;

  SocketChannel socketChannel;
  SSLEngine sslEngine;

  private ByteBuffer readPlainTextBuffer = ByteBuffer.allocate(bufferSizes);
  private ByteBuffer readEncryptedBuffer = ByteBuffer.allocate(bufferSizes);

  private ByteBuffer writePlainTextBuffer = ByteBuffer.allocate(bufferSizes);
  private ByteBuffer writeEncryptedBuffer = ByteBuffer.allocate(bufferSizes);

  Foo(SSLEngine sslEngine, SocketChannel socketChannel) throws SSLException {
    this.socketChannel = socketChannel;
    this.sslEngine = sslEngine;

    sslEngine.beginHandshake();
  }

  //  void readHandshake() throws IOException {
  //    int bytesRead = socketChannel.read(readEncryptedBuffer.clear());
  //    System.out.printf("HandShake. Read encrypted Buffer: %s.\n", readEncryptedBuffer);
  //    System.out.printf("HandShake. Read %d bytes\n", bytesRead);
  //
  //    SSLEngineResult engineResult =
  //        sslEngine.unwrap(readEncryptedBuffer.flip(), readPlainTextBuffer);
  //    SSLEngineResult.Status status = engineResult.getStatus();
  //    SSLEngineResult.HandshakeStatus handshakeStatus = engineResult.getHandshakeStatus();
  //
  //    if (status == Status.OK && handshakeStatus == HandshakeStatus.FINISHED) {
  //      System.out.println("HandShake Read: FINISHED");
  //    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NEED_TASK) {
  //      System.out.println("HandShake Read: NEED TASK");
  //      Runnable task = sslEngine.getDelegatedTask(); // should not block current thread
  //      task.run();
  //      readHandshake();
  //    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NOT_HANDSHAKING) {
  //      System.out.println("HandShake Read: NOT HANDSHAKING");
  //    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NEED_UNWRAP) {
  //      System.out.println("HandShake Read: NEED UNWRAP");
  //      readHandshake();
  //    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NEED_UNWRAP_AGAIN) {
  //      System.out.println("HandShake Read: NEED UNWRAP AGAIN");
  //    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NEED_WRAP) {
  //      System.out.println("HandShake Read: NEED WRAP");
  //    } else if (status == Status.CLOSED) {
  //      System.out.println("HandShake Read: CLOSED");
  //    } else if (status == Status.BUFFER_UNDERFLOW) {
  //      System.out.println("HandShake Read: BUFFER UNDERFLOW");
  //    } else if (status == Status.BUFFER_OVERFLOW) {
  //      System.out.println("HandShake Read: BUFFER OVERFLOW");
  //    }
  //  }

  void writeHandshake() {}

  void read() throws IOException {
    int bytesRead = socketChannel.read(readEncryptedBuffer.clear());
    System.out.printf("Read encrypted Buffer: %s.\n", readEncryptedBuffer);
    System.out.printf("Read %d bytes\n", bytesRead);

    SSLEngineResult engineResult =
        sslEngine.unwrap(readEncryptedBuffer.flip(), readPlainTextBuffer);
    SSLEngineResult.Status status = engineResult.getStatus();
    SSLEngineResult.HandshakeStatus handshakeStatus = engineResult.getHandshakeStatus();

    if (status == Status.OK && handshakeStatus == HandshakeStatus.FINISHED) {
      System.out.println("Read: FINISHED");
    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NEED_TASK) {
      System.out.println("Read: NEED TASK");
      Runnable task = sslEngine.getDelegatedTask(); // should not block current thread
      task.run();
      read();
    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NOT_HANDSHAKING) {
      System.out.println("Read: NOT HANDSHAKING");
    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NEED_UNWRAP) {
      System.out.println("Read: NEED UNWRAP");
      read();
    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NEED_UNWRAP_AGAIN) {
      System.out.println("Read: NEED UNWRAP AGAIN");
    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NEED_WRAP) {
      System.out.println("Read: NEED WRAP");
      write(new byte[100]);
    } else if (status == Status.CLOSED) {
      System.out.println("Read: CLOSED");
    } else if (status == Status.BUFFER_UNDERFLOW) {
      System.out.println("Read: BUFFER UNDERFLOW");
    } else if (status == Status.BUFFER_OVERFLOW) {
      System.out.println("Read: BUFFER OVERFLOW");
    }
  }

  void write(byte[] src) throws IOException {
    System.out.printf("Sending %d bytes.\n", src.length);
    SSLEngineResult engineResult = sslEngine.wrap(ByteBuffer.wrap(src), writeEncryptedBuffer);
    System.out.printf("Write Encrypted buffer: %s\n", writeEncryptedBuffer);
    SSLEngineResult.Status status = engineResult.getStatus();
    SSLEngineResult.HandshakeStatus handshakeStatus = engineResult.getHandshakeStatus();
    writeEncryptedBuffer.compact()


    if (status == Status.OK && handshakeStatus == HandshakeStatus.FINISHED) {
      System.out.println("Write: FINISHED");
    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NEED_TASK) {
      System.out.println("Write: NEED TASK");
    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NOT_HANDSHAKING) {
      System.out.println("Write: NOT HANDSHAKING");
    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NEED_UNWRAP) {
      System.out.println("Write: NEED UNWRAP");
      int bytesWritten = socketChannel.write(writeEncryptedBuffer.flip());
      System.out.printf("Wrote %d bytes\n", bytesWritten);
      read();
    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NEED_UNWRAP_AGAIN) {
      System.out.println("Write: NEED UNWRAP AGAIN");
    } else if (status == Status.OK && handshakeStatus == HandshakeStatus.NEED_WRAP) {
      System.out.println("Write: NEED WRAP");
    } else if (status == Status.CLOSED) {
      System.out.println("Write: CLOSED");
    } else if (status == Status.BUFFER_UNDERFLOW) {
      System.out.println("Write: BUFFER UNDERFLOW");
    } else if (status == Status.BUFFER_OVERFLOW) {
      System.out.println("Write: BUFFER OVERFLOW");
      writeEncryptedBuffer = ByteBuffer.allocate(100_000);
      write(src);
    }
  }
}
