package com.cmhteixeira.concurrency
import cats.data.{NonEmptyList, ValidatedNel}
import cats.implicits.catsSyntaxValidatedId

import java.nio.file.Path

package object fileio {
  private[fileio] val oneMbInBytes: Int = 1024 * 1024 * 1
  private[fileio] val nameLongOptionSizeFiles = "size-files"
  private[fileio] def parseSizeFiles(size: String): ValidatedNel[String, Int] = {
    size.toIntOption match {
      case Some(value) => value.validNel[String]
      case None =>
        val error = s"Input '$size' for option '$nameLongOptionSizeFiles' invalid.".invalidNel
        size.takeRight(1).toLowerCase match {
          case "k" =>
            size
              .dropRight(1)
              .toIntOption
              .fold[ValidatedNel[String, Int]](error)(sizeInKb => (sizeInKb * 1024).validNel)
          case "m" =>
            size
              .dropRight(1)
              .toIntOption
              .fold[ValidatedNel[String, Int]](error)(sizeInMb => (sizeInMb * 1024 * 1024).validNel)
          case "g" =>
            size
              .dropRight(1)
              .toIntOption
              .fold[ValidatedNel[String, Int]](error)(sizeInGb => (sizeInGb * 1024 * 1024 * sizeInGb).validNel)
          case _ => error
        }
    }
  }

  case class Config(
      dirs: NonEmptyList[Path],
      numFiles: Int,
      sizeFilesBytes: Int,
      userControlStart: Boolean,
      numThreads: Int,
      maxElements: Option[Int],
      executorStatistics: Boolean
  ) {
    final override def toString: String = {
      val maxElementsStr = maxElements.fold("Unbounded")(_.toString)
      val dirsString = dirs.toList.mkString(", ")
      s"Config{dirs=[$dirsString], numFiles=$numFiles, sizeEachFile=$sizeFilesBytes, userControlled=$userControlStart, numThreads=$numThreads, maxElements=$maxElementsStr, executorStats=$executorStatistics"
    }
  }
}
