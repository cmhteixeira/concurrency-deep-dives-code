package com.cmhteixeira.concurrency.sockets.blocking.staticfileserver
import org.slf4j.LoggerFactory

import java.io.DataInputStream
import java.net.{InetSocketAddress, NetworkInterface, ServerSocket}
import java.nio.file.Path
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

object FileServer extends App {

  def parseRequestList(dIS: DataInputStream): Try[Path] = ???

  private val log = LoggerFactory.getLogger("BlockingSockets")
  val interfaces = NetworkInterface.networkInterfaces().toList.asScala
  interfaces.foreach(interface => {
    println(s"#####")
    println(s"DisplayName=${interface.getDisplayName}")
    println(s"HardwareAddress=${Option(interface.getHardwareAddress).map(new String(_)).getOrElse("NA")}")
    println(s"isLoopBack=${interface.isLoopback}")
    interface.getInterfaceAddresses.asScala.foreach(ip => {
      println(s"  IP=${ip.getAddress}")
    })
  })

  val serverSocket = new ServerSocket()
  serverSocket.bind(new InetSocketAddress("127.0.0.1", 8080))
  val clientSocket = serverSocket.accept()
  log.info("Connection accepted ...")

  val inputStream = clientSocket.getInputStream
  val dataInputStream = new DataInputStream(inputStream)


  var exit = false
  while (!exit) {
    println("Entering ...")
    println(new String(inputStream.readNBytes(15)))
    exit = true
  }
}
