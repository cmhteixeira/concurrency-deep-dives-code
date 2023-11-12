package com.cmhteixeira.concurrency.fileio
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousChannelGroup, AsynchronousFileChannel, CompletionHandler}
import java.nio.file.{Paths, StandardOpenOption}
import java.util.UUID
import java.util.concurrent.{Executors, ThreadFactory, ThreadPoolExecutor}
import scala.concurrent.{ExecutionContext, Promise}

object Runner extends App {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool(new ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r)
      t.setDaemon(false)
      t
    }
  }))

  val newFile =
    Option(System.getProperty("user.home")).map(i => Paths.get(i, s"temp-${UUID.randomUUID().toString}")).get
  val fileChannel = AsynchronousFileChannel.open(newFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
  val group = AsynchronousChannelGroup.withThreadPool(???)


  val myPromise = Promise[Int]()

  fileChannel.write(
    ByteBuffer.wrap(new Array[Byte](1000)),
    0L,
    (),
    new CompletionHandler[Integer, Unit] {
      override def completed(result: Integer, attachment: Unit): Unit = myPromise.success(result.toInt)
      override def failed(exc: Throwable, attachment: Unit): Unit = myPromise.failure(exc)
    }
  )
  myPromise.future.andThen(_ => println("Completed"))
}
