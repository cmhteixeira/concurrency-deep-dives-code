package com.cmhteixeira.concurrency.fileio

import cats.implicits.catsSyntaxTuple7Semigroupal
import com.monovore.decline.{CommandApp, Opts}

import java.nio.file.Path

object WriteFilesApp
    extends CommandApp(
      name = "cmhTeixeira - AsynchronousFileChannel tests.",
      header = "",
      main = {

        val numFiles =
          Opts.option[Int]("number-of-files", help = "Number of files to create").withDefault(10)

        val sizeFiles =
          Opts
            .option[String](nameLongOptionSizeFiles, help = "Size of each file")
            .mapValidated(parseSizeFiles)
            .withDefault(oneMbInBytes)

        val dirs = Opts.options[Path](long = "directories", help = "", short = "dirs")

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

        (numFiles, sizeFiles, dirs, waitToStart, numThreads, maxElements, statsExecutor).mapN {
          case (numberFiles, sizeFiles, dirs, wait, numThreads, maxElems, statsEc) =>
            WriteFiles.run(Config(dirs, numberFiles, sizeFiles, wait, numThreads, maxElems, statsEc))
        }
      }
    ) {}
