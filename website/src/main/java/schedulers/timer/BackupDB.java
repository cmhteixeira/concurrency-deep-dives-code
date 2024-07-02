package schedulers.timer;

import java.util.TimerTask;
public class BackupDB extends TimerTask {

  @Override
  public void run() {
    System.out.println("Starting backup");
    try {
      Thread.sleep(10_000);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.println("End backup");
  }
}
