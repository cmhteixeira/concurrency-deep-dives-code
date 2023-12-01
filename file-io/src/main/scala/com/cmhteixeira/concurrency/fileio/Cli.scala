package com.cmhteixeira.concurrency.fileio
import scala.util.{Failure, Success, Try}

object Cli {
  def main(args: Array[String]): Unit = {
    MainCommand.mainCommand.parse(args, sys.env) match {
      case Left(value) =>
        System.err.println(value)
        sys.exit(1)
      case Right(async: Config.AsyncConfig) =>
        Try(WriteFilesAsync.run(async)) match {
          case Failure(_) => sys.exit(1)
          case Success(_) => sys.exit(0)
        }

      case Right(sync: Config.SyncConfig) =>
        WriteFilesSync.run(sync)
        sys.exit(0)
    }
  }
}
