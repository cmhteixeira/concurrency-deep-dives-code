package com.cmhteixeira.concurrency.fileio
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object TestPasswordReader extends App {
  val foo = PasswordReader(System.in).password

  foo.onComplete {
    case Failure(exception) => println("Error")
    case Success(value) => println(s"Password is $value")
  }

  Thread.sleep(100000)
}
