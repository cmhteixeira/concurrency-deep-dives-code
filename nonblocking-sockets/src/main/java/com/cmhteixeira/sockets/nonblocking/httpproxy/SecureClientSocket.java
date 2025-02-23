package com.cmhteixeira.sockets.nonblocking.httpproxy;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;
import static javax.net.ssl.SSLEngineResult.Status;
import static javax.net.ssl.SSLEngineResult.Status.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;

public class SecureClientSocket {

  private final SSLEngine sslEngine;
  private final SocketChannel socketChannel;

  private final ByteBuffer readPlainBuffer;
  private final ByteBuffer readEncryptedBuffer;
  private ByteBuffer writePlainBuffer;
  private final ByteBuffer writeEncryptedBuffer;
  private final TlsSocketInputStream inputStream;
  private final TlsSocketOutputStream outputStream;
  private final AtomicBoolean singleHandshake = new AtomicBoolean(false);

  public SecureClientSocket(
      SSLEngine sslEngine,
      InetSocketAddress ipAddress,
      Integer sizeAppRead,
      Integer sizePacketRead,
      Integer sizePacketWrite)
      throws IOException {
    this.socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(true);
    socketChannel.connect(ipAddress);
    this.sslEngine = sslEngine;
    var session = sslEngine.getSession();
    this.readPlainBuffer =
        ByteBuffer.allocateDirect(size(sizeAppRead, session.getApplicationBufferSize()));
    this.readEncryptedBuffer =
        ByteBuffer.allocateDirect(size(sizePacketRead, session.getPacketBufferSize()));
    this.writePlainBuffer = ByteBuffer.allocateDirect(session.getApplicationBufferSize());
    this.writeEncryptedBuffer =
        ByteBuffer.allocateDirect(size(sizePacketWrite, session.getPacketBufferSize()));
    this.inputStream = new TlsSocketInputStream();
    this.outputStream = new TlsSocketOutputStream();
  }

  private Integer size(Integer maybeNull, Integer defaultValue) {
    return Optional.ofNullable(maybeNull).filter(o -> o >= defaultValue).orElse(defaultValue);
  }

  private void handshakeIfRequired() throws IOException {
    if (singleHandshake.get()) return;
    if (!singleHandshake.compareAndSet(false, true)) handshakeIfRequired();
    else {
      readEncryptedBuffer.position(readEncryptedBuffer.limit());
      recursiveNegotiation(NEED_WRAP, OK, 0);
      System.out.println("Finished negotiation ...");
    }
  }

  public InputStream getInputStream() {
    readPlainBuffer.position(readPlainBuffer.limit()); // temporary hack.
    return inputStream;
  }

  public OutputStream getOutputStream() {
    return outputStream;
  }

  private int recursiveRead(HandshakeStatus handshakeStatus, Status status, int iter)
      throws IOException {

    System.out.println("Recursive read. Iter: " + iter);
    if (handshakeStatus != FINISHED && handshakeStatus != NOT_HANDSHAKING) {
      throw new UnsupportedOperationException("Not possible to RE-handshake.");
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
        if (res.bytesProduced() != 0) {
          yield res.bytesProduced();
        } else yield recursiveRead(res.getHandshakeStatus(), res.getStatus(), iter + 1);
      }
      case BUFFER_OVERFLOW ->
          throw new IllegalStateException("Don't know what to do... BUFFER_OVERFLOW");
      case OK -> {
        SSLEngineResult res = sslEngine.unwrap(readEncryptedBuffer, readPlainBuffer);
        System.out.println(res);
        printReadBuffers();
        if (res.bytesProduced() != 0) {
          yield res.bytesProduced();
        } else yield recursiveRead(res.getHandshakeStatus(), res.getStatus(), iter + 1);
      }
      case CLOSED -> throw new RuntimeException("CLOSEED???");
    };
  }

  private void printAllBuffers() {
    System.out.println("######");
    System.out.println("readPlainBuffer: " + readPlainBuffer);
    System.out.println("readEncryptedBuffer: " + readEncryptedBuffer);
    System.out.println("writePlainBuffer: " + writePlainBuffer);
    System.out.println("writeEncryptedBuffer: " + writeEncryptedBuffer);
    System.out.println("######");
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

  private void recursiveWrite(HandshakeStatus handshakeStatus, Status status, int iter)
      throws IOException {
    if (handshakeStatus != FINISHED && handshakeStatus != NOT_HANDSHAKING) {
      throw new UnsupportedOperationException("Not possible to RE-handshake.");
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
          recursiveWrite(handshakeStatus, status, iter + 1);
        } else if (writePlainBuffer.hasRemaining()) {
          writeEncryptedBuffer.clear();
          SSLEngineResult res = sslEngine.unwrap(writePlainBuffer, writeEncryptedBuffer);
          recursiveWrite(res.getHandshakeStatus(), res.getStatus(), iter + 1);
        } else {
        }
      }
      case CLOSED -> throw new RuntimeException("CLOSEED???");
    }
  }

  private void recursiveNegotiation(HandshakeStatus handshakeStatus, Status status, int iter)
      throws IOException {
    System.out.printf("############ Start new iteration %d ################\n", iter);
    System.out.printf("%s + %s\n", status, handshakeStatus);
    printAllBuffers();

    if (status == CLOSED) throw new RuntimeException("Got CLOSED when handshaking.");

    if (handshakeStatus == FINISHED) {
      printAllBuffers();
      System.out.println("Handshake finished.");
    }

    if (handshakeStatus == NOT_HANDSHAKING) {
      throw new IllegalStateException("Should be impossible to reach this state.");
    }

    if (handshakeStatus == NEED_UNWRAP_AGAIN) {
      throw new IllegalStateException("Got NEED_UNWRAP_AGAIN but that should only apply for UDP.");
    }

    if (status == BUFFER_OVERFLOW) {
      throw new UnsupportedOperationException("Changing buffer sizes and OVERFLOWs not supported.");
    }

    if (status == BUFFER_UNDERFLOW && handshakeStatus == NEED_WRAP) {
      throw new IllegalStateException("Not expecting to get BUFFER_UNDERFLOW and NEED_UNWRAP.");
    }

    if (handshakeStatus == NEED_TASK) {
      Runnable task = sslEngine.getDelegatedTask();
      task.run();
      recursiveNegotiation(sslEngine.getHandshakeStatus(), status, iter + 1);
    }

    if (status == OK && handshakeStatus == NEED_UNWRAP) {
      if (!readEncryptedBuffer.hasRemaining()) {
        readEncryptedBuffer.clear();
        read();
        readEncryptedBuffer.flip();
      }
      SSLEngineResult engineResult = sslEngine.unwrap(readEncryptedBuffer, readPlainBuffer);
      recursiveNegotiation(engineResult.getHandshakeStatus(), engineResult.getStatus(), iter + 1);
    }

    if (status == OK && handshakeStatus == NEED_WRAP) {
      SSLEngineResult engineRes = sslEngine.wrap(writePlainBuffer, writeEncryptedBuffer);
      if (engineRes.bytesProduced() == 0) throw new IllegalStateException("No data wrapped.");
      writeEncryptedBuffer.flip();
      write();
      if (writeEncryptedBuffer.hasRemaining()) throw new IllegalStateException("Data remaining");
      writeEncryptedBuffer.clear();
      System.out.println(engineRes);
      recursiveNegotiation(engineRes.getHandshakeStatus(), engineRes.getStatus(), iter + 1);
    }

    if (status == BUFFER_UNDERFLOW && handshakeStatus == NEED_UNWRAP) {
      System.out.println("Before compacting: " + readEncryptedBuffer);
      readEncryptedBuffer.compact();
      System.out.println("After compacting: " + readEncryptedBuffer);
      read();
      readEncryptedBuffer.flip();
      System.out.println("After flipping: " + readEncryptedBuffer);
      SSLEngineResult engineRes = sslEngine.unwrap(readEncryptedBuffer, readPlainBuffer);
      System.out.printf("sslEngineResult: %s\n", engineRes);
      System.out.println(readEncryptedBuffer);
      recursiveNegotiation(engineRes.getHandshakeStatus(), engineRes.getStatus(), iter + 1);
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
    if (bytesRead == -1) throw new SocketException("End of stream reached.");
    System.out.printf("After reading %d: %s\n", bytesRead, readEncryptedBuffer);
  }

  private class TlsSocketOutputStream extends OutputStream {

    private TlsSocketOutputStream() {}

    @Override
    public void write(int b) throws IOException {
      ByteBuffer byteBuffer = ByteBuffer.allocate(1);
      byte theByte = (byte) b;
      byteBuffer.put(theByte);
      write(byteBuffer.array());
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      if (off < 0 || len < 0 || off + len > b.length)
        throw new IndexOutOfBoundsException("Invalid offset or length.");
      if (len == 0) return;
      handshakeIfRequired();
      ByteBuffer previousPlainWrite = writePlainBuffer;
      writePlainBuffer = ByteBuffer.wrap(b, off, len);
      writeEncryptedBuffer.clear();
      SSLEngineResult res = sslEngine.wrap(writePlainBuffer, writeEncryptedBuffer);
      recursiveWrite(res.getHandshakeStatus(), res.getStatus(), 0);
      writePlainBuffer = previousPlainWrite;
      if (writeEncryptedBuffer.hasRemaining()) {
        throw new IllegalStateException("Write Packet Buffer should have no remaining bytes.");
      }
    }

    @Override
    public void close() throws IOException {
      // does nothing. Should we be shutting down the socket?
    }

    @Override
    public void flush() throws IOException {
      // does nothing. Should this be changed?
    }
  }

  private class TlsSocketInputStream extends InputStream {
    private void readInternal() throws IOException {
      handshakeIfRequired();
      recursiveRead(NOT_HANDSHAKING, OK, 0);
      printReadBuffers();
      readPlainBuffer.flip();
    }

    @Override
    public int read() throws IOException {
      if (!readPlainBuffer.hasRemaining()) {
        readPlainBuffer.clear();
        readInternal();
      }
      return readPlainBuffer.get() & 0xFF;
    }

    @Override
    public long skip(long n) {
      if (n < 0) return 0L;
      int toDiscard = (int) Math.min(readPlainBuffer.remaining(), n);
      readPlainBuffer.position(readPlainBuffer.position() + toDiscard);
      return toDiscard;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (off < 0 || len < 0 || off + len > b.length)
        throw new IndexOutOfBoundsException("Invalid offset or length.");
      if (len == 0) return 0;
      int toRead = Math.min(readPlainBuffer.remaining(), len);
      if (toRead == 0) {
        readPlainBuffer.clear();
        readInternal();
        return read(b, off, len);
      }
      readPlainBuffer.get(b, off, len);
      return toRead;
    }

    @Override
    public int available() {
      // todo: Check if closed?
      return readPlainBuffer.remaining();
    }

    @Override
    public void close() throws IOException {
      // Todo: close stream?
    }
  }
}
