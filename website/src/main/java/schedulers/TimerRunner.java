package schedulers;

import java.util.Timer;
import java.util.TimerTask;

public class TimerRunner {
  public static void main(String[] args) {
    Timer timer = new Timer();

    TimerTask task = new BackupDB2(10, "db");
    TimerTask task2 = new BackupDB2(5, "one-off");

    timer.scheduleAtFixedRate(task, 0, 12_000);
    timer.schedule(task2, 11_000L);
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