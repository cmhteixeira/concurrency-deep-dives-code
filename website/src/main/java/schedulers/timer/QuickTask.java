package schedulers.timer;

import java.util.TimerTask;

public class QuickTask extends TimerTask {

  private final long startReference;
  private int iterations = 0;

  private final String taskName;

  public QuickTask(String taskName) {
    this.startReference = System.currentTimeMillis();
    this.taskName = taskName;
  }

  private int lapsedS() {
    return (int) ((System.currentTimeMillis() - startReference) / 1000L);
  }

  @Override
  public void run() {
    ++iterations;
    System.out.printf("%d s - Run %s#%d\n", lapsedS(), taskName, iterations);
  }
}
