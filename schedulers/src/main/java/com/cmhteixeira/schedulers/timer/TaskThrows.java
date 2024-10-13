package com.cmhteixeira.schedulers.timer;

import java.util.TimerTask;

public class TaskThrows extends TimerTask {

  private final long startReference;
  private final int throwAfter;

  private final String taskName;

  public TaskThrows(int throwAfter, String taskName) {
    this.startReference = System.currentTimeMillis();
    this.throwAfter = throwAfter;
    this.taskName = taskName;
  }

  private int lapsedS() {
    return (int) ((System.currentTimeMillis() - startReference) / 1000L);
  }

  @Override
  public void run() {
    System.out.printf("%d s - Start %s\n", lapsedS(), taskName);
    try {
      Thread.sleep(throwAfter * 1000L);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    throw new RuntimeException(
        String.format(
            "Throwing for task %s on thread %s after %d seconds",
            taskName, Thread.currentThread().getName(), throwAfter));
  }
}
