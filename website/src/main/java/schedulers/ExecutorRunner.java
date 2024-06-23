package schedulers;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorRunner {
  public static void main(String[] args) {
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    Runnable taskA = new BackupDB2(10, "db");
    Runnable taskB = new BackupDB2(5, "one-off");

    scheduler.scheduleWithFixedDelay(taskA, 0, 12_000, TimeUnit.MILLISECONDS);
    scheduler.schedule(taskB, 11_000L, TimeUnit.MILLISECONDS);
  }
}

// Normal
//0 s - Start db
//10 s - End db
//12 s - Start db
//22 s - End db
//24 s - Start db
//34 s - End db
//36 s - Start db

// Fixed delay
//0 s - Start db
//10 s - End db
//11 s - Start one-off
//16 s - End one-off
//16 s - Start db
//26 s - End db
//28 s - Start db