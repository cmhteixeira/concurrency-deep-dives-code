package schedulers.executorservice;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ExecutorRunner {
  public static void main(String[] args)
      throws ExecutionException, InterruptedException, IOException {
    //    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

    Socket remoteConnection = new Socket("localhost", 8080);

    Runnable sendHeartBeat = new HeartbeatTask(remoteConnection);

    Runnable sleep = new MyTask(10, "sleepy-john");

    ScheduledFuture<?> heartBeatTask =
        scheduler.scheduleWithFixedDelay(sendHeartBeat, 0, 2, TimeUnit.SECONDS);

    ScheduledFuture<?> sleepTask = scheduler.schedule(sleep, 1, TimeUnit.SECONDS);

    //    ScheduledFuture<?> heartBeatTask2 =
    //        scheduler.scheduleWithFixedDelay(sendHeartBeat, 0, 60, TimeUnit.SECONDS);
  }
}

// Normal
// 0 s - Start db
// 10 s - End db
// 12 s - Start db
// 22 s - End db
// 24 s - Start db
// 34 s - End db
// 36 s - Start db

// Fixed delay
// 0 s - Start db
// 10 s - End db
// 11 s - Start one-off
// 16 s - End one-off
// 16 s - Start db
// 26 s - End db
// 28 s - Start db
