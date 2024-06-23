package schedulers;

import java.util.TimerTask;

public class BackupDB2 extends TimerTask {

  private final long startReference;
  private final int sleepFor;

  private final String taskName;

  public BackupDB2(int sleepFor, String taskName) {
    this.startReference = System.currentTimeMillis();
    this.sleepFor = sleepFor;
    this.taskName = taskName;
  }

  private int lapsedS() {
    return (int) ((System.currentTimeMillis() - startReference) / 1000L);
  }

  @Override
  public void run() {
    System.out.printf("%d s - Start %s\n", lapsedS(), taskName);
    try {
      Thread.sleep(sleepFor * 1000L);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.printf("%d s - End %s\n", lapsedS(), taskName);
  }
}
