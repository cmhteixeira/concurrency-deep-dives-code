package com.cmhteixeira.sockets.nonblocking.httpproxy;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;
import static javax.net.ssl.SSLEngineResult.Status;
import static javax.net.ssl.SSLEngineResult.Status.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;

public class TlsNegotiation {

  private OverallState overallState;

  private SSLEngine sslEngine;
  private SocketChannel socketChannel;

  private ByteBuffer readPlainBuffer;
  private ByteBuffer readEncryptedBuffer;
  private ByteBuffer writePlainBuffer;
  private ByteBuffer writeEncryptedBuffer;

  public TlsNegotiation(SSLEngine sslEngine, SocketChannel socketChannel) {
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
    this.overallState = OverallState.NEGOTIATING;
  }

  public void write(ByteBuffer src) throws IOException {
    if (src.capacity() > writeEncryptedBuffer.capacity())
      throw new IllegalArgumentException("Too much... feed me less");

    if (overallState == OverallState.NEGOTIATING) {
      SSLEngineResult sslEngineResult = sslEngine.unwrap(readEncryptedBuffer, readPlainBuffer);
      recursiveNegotiaton(sslEngineResult.getHandshakeStatus(), sslEngineResult.getStatus(), 0);
      overallState = new OverallState.Normal(true, readEncryptedBuffer.hasRemaining());
      System.out.println("Finished negotiation ...");
    }
    printWriteBuffers();

    writeEncryptedBuffer.clear(); // yes
    writePlainBuffer.clear(); // yes
    writePlainBuffer.put(src); // TODO: This is expensive, right? Is there a better way?
    writePlainBuffer.flip();
    SSLEngineResult res = sslEngine.wrap(writePlainBuffer, writeEncryptedBuffer);
    recursiveSend(res.getHandshakeStatus(), res.getStatus(), 0);
    printWriteBuffers();
    System.out.println("Finished writing ...");
  }

  public int read(ByteBuffer src) throws IOException {
    int bytesRead = recursiveRead(NOT_HANDSHAKING, OK, 0);
    printReadBuffers();
    src.put(readPlainBuffer.flip());
    return bytesRead;
  }

  private int recursiveRead(HandshakeStatus handshakeStatus, Status status, int iter)
      throws IOException {

    System.out.println("Recursive read. Iter: " + iter);
    if (handshakeStatus != FINISHED && handshakeStatus != NOT_HANDSHAKING) {
      throw new RuntimeException("New Handshake required??");
    }

    printReadBuffers();

    return switch (status) {
      case BUFFER_UNDERFLOW -> {
        readEncryptedBuffer.compact();
        read();
        printReadBuffers();
        SSLEngineResult res = sslEngine.unwrap(readEncryptedBuffer.flip(), readPlainBuffer);
        System.out.println(res);
        printReadBuffers();
        yield recursiveRead(res.getHandshakeStatus(), res.getStatus(), iter + 1);
      }
      case BUFFER_OVERFLOW ->
          throw new IllegalStateException("Don't know what to do... BUFFER_OVERFLOW");
      case OK -> {
        SSLEngineResult res = sslEngine.unwrap(readEncryptedBuffer, readPlainBuffer);
        System.out.println(res);
        printReadBuffers();
        if (res.bytesProduced() != 0) {
          if (!readEncryptedBuffer.hasRemaining()) readEncryptedBuffer.clear();
          yield res.bytesProduced();
        } else yield recursiveRead(res.getHandshakeStatus(), res.getStatus(), iter + 1);
      }
      case CLOSED -> throw new RuntimeException("CLOSEED???");
    };
  }

  private void printAllBuffers() {
    System.out.println("###############");
    System.out.println("readPlainBuffer: " + readPlainBuffer);
    System.out.println("readEncryptedBuffer: " + readEncryptedBuffer);
    System.out.println("writePlainBuffer: " + writePlainBuffer);
    System.out.println("writeEncryptedBuffer: " + writeEncryptedBuffer);
    System.out.println("###############");
  }

  private void printWriteBuffers() {
    System.out.println("###############");
    System.out.println("writePlainBuffer: " + writePlainBuffer);
    System.out.println("writeEncryptedBuffer: " + writeEncryptedBuffer);
    System.out.println("###############");
  }

  private void printReadBuffers() {
    System.out.println("###############");
    System.out.println("readPlainBuffer: " + readPlainBuffer);
    System.out.println("readEncryptedBuffer: " + readEncryptedBuffer);
    System.out.println("###############");
  }

  private void recursiveSend(HandshakeStatus handshakeStatus, Status status, int iter)
      throws IOException {
    if (handshakeStatus != FINISHED && handshakeStatus != NOT_HANDSHAKING) {
      throw new RuntimeException("New Handshake required??");
    }

    System.out.println("Iter: " + iter);

    if (writePlainBuffer.remaining() == 0 && iter != 0) {
      System.out.println("Everything sent... Number iterations: " + iter);
      return;
    }

    if (iter == 0) {
      writeEncryptedBuffer.flip();
    }

    switch (status) {
      case BUFFER_UNDERFLOW ->
          throw new IllegalStateException("Don't know what to do ...BUFFER_UNDERFLOW");
      case BUFFER_OVERFLOW ->
          throw new IllegalStateException("Don't know what to do ...BUFFER_OVERFLOW");
      case OK -> {
        printWriteBuffers();
        write();
        printWriteBuffers();
        if (writeEncryptedBuffer.hasRemaining()) {
          System.out.println("WARNING...");
          recursiveSend(handshakeStatus, status, iter + 1);
        } else if (writePlainBuffer.hasRemaining()) {
          writeEncryptedBuffer.clear();
          SSLEngineResult res = sslEngine.unwrap(writePlainBuffer, writeEncryptedBuffer);
          recursiveSend(res.getHandshakeStatus(), res.getStatus(), iter + 1);
        } else {
        }
      }
      case CLOSED -> throw new RuntimeException("CLOSEED???");
    }
  }

  private void recursiveNegotiaton(HandshakeStatus handshakeStatus, Status status, int iter)
      throws IOException {
    if (iter > 20) throw new RuntimeException("Something is up... couldn't complete handshake..");

    System.out.println("############################");
    System.out.printf("Status=%s + HandshakeStatus=%s\n", status, handshakeStatus);
    if (status == OK && handshakeStatus == FINISHED) {
      System.out.println("Read: FINISHED");
      overallState = OverallState.NORMAL;
    } else if (status == OK && handshakeStatus == NEED_TASK) {
      System.out.println("Read: NEED TASK");
      Runnable task = sslEngine.getDelegatedTask(); // should not block current thread
      task.run();
      recursiveNegotiaton(sslEngine.getHandshakeStatus(), OK, iter + 1);
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
      recursiveNegotiaton(
          sslEngineResult.getHandshakeStatus(), sslEngineResult.getStatus(), iter + 1);
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
      recursiveNegotiaton(
          sslEngineResult.getHandshakeStatus(), sslEngineResult.getStatus(), iter + 1);
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
      recursiveNegotiaton(
          sslEngineResult.getHandshakeStatus(), sslEngineResult.getStatus(), iter + 1);
    } else if (status == CLOSED) {
      System.out.println("Read: CLOSED");
    } else if (status == BUFFER_UNDERFLOW) {
      System.out.println("Read: BUFFER UNDERFLOW");
      read();
      SSLEngineResult sslEngineResult =
          sslEngine.unwrap(readEncryptedBuffer.flip(), readPlainBuffer);
      System.out.println(sslEngineResult);
      recursiveNegotiaton(
          sslEngineResult.getHandshakeStatus(), sslEngineResult.getStatus(), iter + 1);
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

  private sealed interface OverallState {
    Normal NORMAL = normal(false, false);
    Negotiating NEGOTIATING = new Negotiating();

    Normal hasLeftToEncode(boolean has);

    static Normal normal(boolean leftToWrap, boolean leftToUnwrap) {
      return new Normal(leftToWrap, leftToUnwrap);
    }

    record Normal(boolean leftToEncode, boolean leftToDecode) implements OverallState {

      @Override
      public Normal hasLeftToEncode(boolean has) {
        return new Normal(true, this.leftToDecode);
      }
    }

    record Negotiating() implements OverallState {
      @Override
      public Normal hasLeftToEncode(boolean has) {
        throw new IllegalStateException("kaBoom!");
      }
    }
  }
}
