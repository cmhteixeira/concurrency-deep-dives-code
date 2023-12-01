package com.cmhteixeira.concurrency.fileio

import cats.implicits.toTraverseOps
import com.cmhteixeira.concurrency.fileio.Config.OSMode
import com.sun.nio.file.ExtendedOpenOption
import org.slf4j.LoggerFactory

import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.file.{OpenOption, StandardOpenOption}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.jdk.CollectionConverters.SetHasAsJava
import scala.util.{Failure, Success, Try}

object WriteFilesAsync {
  private val log = LoggerFactory.getLogger("WriteFiles")
  def run(config: Config.AsyncConfig): Try[Unit] = {
    val Config.AsyncConfig(
      dirs,
      numFiles,
      sizeFilesBytes,
      userControlStart,
      numThreads,
      queueMaxElem,
      captureExStats,
      osMode
    ) =
      config
    log.info(s"Starting: $config")
    if (userControlStart) {
      println("Waiting for input to proceed ...")
      log.info("Waiting for input to proceed ...")
      System.in.read()
    }

    val startTime = System.nanoTime()
    val ec = ExecutorBuilder.createExecutor(numThreads, queueMaxElem)
    val trackingEc = if (captureExStats) ExecutorBuilder.trackingExecutionTime(ec) else ec

    val openOptions: Set[OpenOption] =
      Set(StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)

    val fileChannels = (1 to numFiles)
      .map { idx =>
        dirs.toList(idx % dirs.size).resolve(s"$idx.TXT")
      }
      .map { o =>
        log.info(s"Opening $o")
        AsynchronousFileChannel
          .open(
            o,
            (osMode match {
              case Some(OSMode.DirectIO) => openOptions + ExtendedOpenOption.DIRECT
              case Some(OSMode.SyncIO) => openOptions + StandardOpenOption.SYNC
              case None => openOptions
            }).asJava,
            trackingEc
          )
      }
      .toList

    log.info(s"Finished opening all files:  ${(System.nanoTime() - startTime) * 1L / (1_000_000_000)} s")
    val afterOpening = System.nanoTime()
    val submitted = fileChannels.map { fC =>
      val promise = Promise[Unit]()
      fC.write(
        ByteBuffer.wrap(new Array[Byte](sizeFilesBytes)),
        0,
        (),
        new CompletionHandler[Integer, Unit] {
          override def completed(result: Integer, attachment: Unit): Unit = {
            log.info(s"Wrote successfully.")
            promise.success(())
          }
          override def failed(exc: Throwable, attachment: Unit): Unit = promise.failure(exc)
        }
      )
      promise.future
    }
    log.info(s"Finished submitting all files:  ${(System.nanoTime() - afterOpening) * 1L / (1_000_000_000)} s")

    val res = submitted.sequence
      .andThen {
        case Failure(exception) => log.info("TODO", exception)
        case Success(_) =>
          trackingEc match {
            case ec: TrackingExecutorService =>
              val submissions = ec.getSubmissions
              val failure = submissions.collect { case Failure(exception) => exception }
              val successes = submissions.collect { case Success(timeMillis) => timeMillis }
              if (failure.nonEmpty) log.warn("Not all succeeded")
              val max = successes.max
              val min = successes.min
              val av = (successes.sum / successes.size)
              log.info(s"Max: $max. Min: $min. Average: $av")
              log.info(s"All: [${successes.sorted.mkString(", ")}]")
              log.info(s"Finished writing all files:  ${(System.nanoTime() - afterOpening) * 1L / (1_000_000)} ms")
            case _ => log.info("Finished ...")
          }
      }
      .andThen { _ =>
        log.info("Shutting down ....")
        trackingEc.shutdownNow()
        fileChannels.zipWithIndex.foreach { case (fC, i) =>
          fC.close()
          log.info(s"Closed file channel - $i")
        }
      }
    Try(Await.result(res, Duration.Inf))
  }
}
