package com.cmhteixeira.concurrency.fileio
import java.util.concurrent.{ExecutorService, LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}

object ExecutorBuilder {
  def trackingExecutionTime(delegatee: ExecutorService): TrackingExecutorService = TrackingExecutorService(delegatee)

  private def threadPoolExecutor(numThreads: Int, maxElements: Option[Int]): ThreadPoolExecutor = {
    val queue = maxElements.fold(new LinkedBlockingQueue[Runnable]())(new LinkedBlockingQueue[Runnable](_))
    new ThreadPoolExecutor(numThreads, numThreads, 0L, TimeUnit.SECONDS, queue)
  }

  def createExecutor(numThreads: Int, maxElements: Option[Int]): ExecutorService =
    maxElements match {
      case Some(value) => threadPoolExecutor(value, maxElements)
      case None =>
        if (numThreads == 1) java.util.concurrent.Executors.newSingleThreadExecutor()
        else threadPoolExecutor(numThreads, maxElements)
    }
}
