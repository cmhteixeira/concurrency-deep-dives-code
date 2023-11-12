package com.cmhteixeira.concurrency.fileio
import java.io.InputStream
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}
import scala.util.{Failure, Success, Try}

class PasswordReader private (myStream: InputStream) {
  private val passwordPrefix = " password="

  private def read(): Try[Char] =
    Try(myStream.read()).flatMap { i =>
      if (i == -1) Failure(new IllegalStateException("End of stream."))
      else { Success(i) }
    } map (_.toChar)

  private def readPrefix(prefix: String): Try[Unit] = {
    @tailrec
    def internal(index: Int, newChar: Char): Try[Unit] = Try(prefix(index + 1)) match {
      case Failure(_: IndexOutOfBoundsException) => Success(())
      case Failure(e) => Failure(e)
      case Success(currentValue) =>
        read() match {
          case Failure(exception) => Failure(exception)
          case Success(nextValue) =>
            if (currentValue == newChar)
              if (prefix.length - 2 == index) Success(()) else internal(index + 1, nextValue)
            else internal(0, nextValue)
        }
    }
    read().flatMap(t => internal(0, t))
  }

  @tailrec
  private def readPassword(acc: String): Try[String] =
    read() match {
      case Failure(exception) => Failure(exception)
      case Success(value) if value == ' ' => Success(acc + value)
      case Success(value) => readPassword(acc + value)
    }

  def password: Future[String] = Future {
    blocking {
      (for {
        _ <- readPrefix(" " + passwordPrefix)
        password <- readPassword("")
      } yield password).get
    }
  }
}

object PasswordReader {
  def apply(myStream: InputStream) = new PasswordReader(myStream)
}
