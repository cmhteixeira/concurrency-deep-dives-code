package com.cmhteixeira.concurrency.fileio

import java.util
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{AbstractExecutorService, ExecutorService, TimeUnit}
import scala.util.{Failure, Success, Try}

final class TrackingExecutorService private (delegatee: ExecutorService, submissions: AtomicReference[List[Try[Long]]])
    extends AbstractExecutorService {

  def getSubmissions: List[Try[Long]] = submissions.get()

  private def addSubmission(submission: Try[Long]): Unit = {
    val currentState = submissions.get()
    if (!submissions.compareAndSet(currentState, currentState :+ submission)) addSubmission(submission)
  }

  private class TrackingRunnable(delegate: Runnable) extends Runnable {
    override def run(): Unit = {
      val start = System.nanoTime()
      Try(delegate.run()) match {
        case Failure(exception) =>
          addSubmission(Failure(exception))
          throw exception
        case Success(()) =>
          val tookMillis = (System.nanoTime() - start) / 1_000_000L
          addSubmission(Success(tookMillis))
      }
    }
  }

  override def shutdown(): Unit = delegatee.shutdown()
  override def shutdownNow(): util.List[Runnable] = delegatee.shutdownNow()
  override def isShutdown: Boolean = delegatee.isShutdown
  override def isTerminated: Boolean = delegatee.isTerminated
  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = delegatee.awaitTermination(timeout, unit)
  override def execute(command: Runnable): Unit = delegatee.execute(new TrackingRunnable(command))
}

object TrackingExecutorService {
  def apply(delegatee: ExecutorService): TrackingExecutorService =
    new TrackingExecutorService(delegatee, new AtomicReference[List[Try[Long]]](List.empty))
}
