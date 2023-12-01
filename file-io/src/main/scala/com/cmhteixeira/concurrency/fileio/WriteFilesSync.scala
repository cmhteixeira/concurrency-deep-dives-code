package com.cmhteixeira.concurrency.fileio

import com.cmhteixeira.concurrency.fileio.Config.OSMode
import com.sun.nio.file.ExtendedOpenOption
import org.slf4j.LoggerFactory

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{OpenOption, StandardOpenOption}
import scala.jdk.CollectionConverters.SetHasAsJava

object WriteFilesSync {
  private val log = LoggerFactory.getLogger("WriteFilesSync")
  def run(config: Config.SyncConfig): Unit = {
    val Config.SyncConfig(dirs, numFiles, sizeFilesBytes, userControlStart, osMode) = config
    log.info(s"Starting: $config")
    if (userControlStart) {
      println("Waiting for input to proceed ...")
      log.info("Waiting for input to proceed ...")
      System.in.read()
    }

    val openOptions: Set[OpenOption] =
      Set(StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
    val startTime = System.nanoTime()

    val fileChannels = (1 to numFiles)
      .map { idx =>
        dirs.toList(idx % dirs.size).resolve(s"$idx.TXT")
      }
      .map { o =>
        log.info(s"Opening $o")
        FileChannel
          .open(
            o,
            (osMode match {
              case Some(OSMode.DirectIO) => openOptions + ExtendedOpenOption.DIRECT
              case Some(OSMode.SyncIO) => openOptions + StandardOpenOption.SYNC
              case None => openOptions
            }).asJava
          )
      }
      .toList

    log.info(s"Finished opening all files:  ${(System.nanoTime() - startTime) * 1L / (1_000_000_000)} s")
    val afterOpening = System.nanoTime()
    val timeTaken = fileChannels.map { fC =>
      val start = System.nanoTime()
      fC.write(ByteBuffer.wrap(new Array[Byte](sizeFilesBytes)))
      ((System.nanoTime() - start) * 1L) / 1_000_000L
    }
    log.info(s"Finished submitting all files:  ${(System.nanoTime() - afterOpening) * 1L / (1_000_000_000)} s")

    val max = timeTaken.max
    val min = timeTaken.min
    val av = timeTaken.sum / timeTaken.size
    log.info(s"(millis) Max: $max. Min: $min. Average: $av")
    log.info(s"All: [${timeTaken.sorted.mkString(", ")}]")
    log.info(s"Finished writing all files:  ${(System.nanoTime() - afterOpening) * 1L / (1_000_000)} ms")

    fileChannels.zipWithIndex.foreach { case (fc, i) =>
      fc.close()
      log.info(s"Closed - $i")
    }
  }
}
