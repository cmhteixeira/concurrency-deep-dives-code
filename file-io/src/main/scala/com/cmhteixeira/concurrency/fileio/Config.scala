package com.cmhteixeira.concurrency.fileio
import cats.data.{NonEmptyList, ValidatedNel}
import cats.implicits.catsSyntaxValidatedId
import com.monovore.decline.Argument

import java.nio.file.Path

sealed trait Config

object Config {
  case class SyncConfig(
      dirs: NonEmptyList[Path],
      numFiles: Int,
      sizeFilesBytes: Int,
      userControlStart: Boolean,
      osMode: Option[OSMode]
  ) extends Config {
    final override def toString: String = {
      val dirsString = dirs.toList.mkString(", ")
      s"SyncConfig{dirs=[$dirsString], numFiles=$numFiles, sizeEachFile=$sizeFilesBytes, userControlled=$userControlStart, osMode=${osMode
          .getOrElse("NA")}"
    }
  }

  case class AsyncConfig(
      dirs: NonEmptyList[Path],
      numFiles: Int,
      sizeFilesBytes: Int,
      userControlStart: Boolean,
      numThreads: Int,
      maxElements: Option[Int],
      executorStatistics: Boolean,
      osMode: Option[OSMode]
  ) extends Config {
    final override def toString: String = {
      val maxElementsStr = maxElements.fold("Unbounded")(_.toString)
      val dirsString = dirs.toList.mkString(", ")
      s"AsyncConfig{dirs=[$dirsString], numFiles=$numFiles, sizeEachFile=$sizeFilesBytes, userControlled=$userControlStart, numThreads=$numThreads, maxElements=$maxElementsStr, executorStats=$executorStatistics, osMode=${osMode
          .getOrElse("NA")}"
    }
  }

  sealed trait OSMode
  object OSMode {
    case object DirectIO extends OSMode
    case object SyncIO extends OSMode

    implicit val ev: Argument[OSMode] = new Argument[OSMode] {
      override def read(
          string: String
      ): ValidatedNel[String, OSMode] =
        string.toLowerCase match {
          case "direct" => DirectIO.validNel
          case "sync" => SyncIO.validNel
          case other => s"Input '$other' is not valid.".invalidNel
        }
      override def defaultMetavar: String = "Whether IO operations should by O_DIRECT, or O_SYNC."
    }
  }
}
