package com.cmhteixeira.scala

import java.net.Socket
import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object RunnerScala extends App {
  val echoClient = new EchoClient
  echoClient.send("hello", "localhost", 8080)
}

class EchoClient {
  def send(msg: String, host: String, port: Int): Future[String] = Future {
    val socket = new Socket(host, port)
    val payload = msg.getBytes
    socket.getOutputStream.write(payload)
    new String(socket.getInputStream.readNBytes(payload.length))
  }
}
