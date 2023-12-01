package com.cmhteixeira.concurrency.fileio

import cats.data.ValidatedNel
import cats.implicits.{catsSyntaxTuple9Semigroupal, catsSyntaxValidatedId}
import com.cmhteixeira.concurrency.fileio.Config.OSMode
import com.monovore.decline.{Argument, Command, Opts}

import java.nio.file.Path

object MainCommand {

  sealed trait Mode
  case object Sync extends Mode
  case object Async extends Mode

  implicit val argumnetMode: Argument[Mode] = new Argument[Mode] {
    override def read(
        string: String
    ): ValidatedNel[String, Mode] =
      string.toLowerCase match {
        case "sync" => Sync.validNel
        case "async" => Async.validNel
        case other => s"Value $other not valid.".invalidNel
      }
    override def defaultMetavar: String = "Possible 'sync' or 'async' (case insensitive)"
  }

  private def parseSizeFiles(size: String): ValidatedNel[String, Int] = {
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

  private val oneMbInBytes: Int = 1024 * 1024 * 1
  private val nameLongOptionSizeFiles = "size-files"

  val mainCommand: Command[Config] =
    Command[Config](name = "cmhTeixeira - AsynchronousFileChannel tests.", header = "") {

      val numFiles =
        Opts.option[Int]("number-of-files", help = "Number of files to create").withDefault(10)

      val sizeFiles =
        Opts
          .option[String](nameLongOptionSizeFiles, help = "Size of each file")
          .mapValidated(parseSizeFiles)
          .withDefault(oneMbInBytes)

      val dirs = Opts.options[Path](long = "directories", help = "", short = "dirs")

      val javaMode =
        Opts
          .option[Mode](long = "java-mode", help = "Use AsynchronousFileChannel or synchronous FileChannel")
          .withDefault(Async)

      val numThreads =
        Opts
          .option[Int](long = "num-threads", help = "Number of threads on executor shared amongst file channels.")
          .withDefault(1)

      val maxElements = Opts
        .option[Int](
          long = "max-elems",
          help = "Maximum number of elements possible to keep on the queue backing the executor."
        )
        .orNone

      val statsExecutor = Opts
        .flag(long = "stats-ec", help = "Whether to track number and time duration of tasks that the executor runs.")
        .orFalse

      val waitToStart =
        Opts
          .flag("start-user-control", "If waiting for the user to signal start of the program by pressing any key")
          .orFalse

      val osMode =
        Opts
          .option[OSMode](
            "io-mode",
            "If IO operations should be O_DIRECT or O_SYNC, or neither."
          )
          .orNone

      (numFiles, sizeFiles, dirs, waitToStart, numThreads, maxElements, statsExecutor, javaMode, osMode).mapN {
        case (numberFiles, sizeFiles, dirs, wait, numThreads, maxElems, statsEc, Async, osMode) =>
          Config.AsyncConfig(dirs, numberFiles, sizeFiles, wait, numThreads, maxElems, statsEc, osMode)
        case (numberFiles, sizeFiles, dirs, wait, _, _, _, Sync, osMode) =>
          Config.SyncConfig(dirs, numberFiles, sizeFiles, wait, osMode)
      }
    }
}
