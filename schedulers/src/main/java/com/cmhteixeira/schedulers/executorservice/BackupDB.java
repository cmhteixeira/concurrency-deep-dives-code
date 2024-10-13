package com.cmhteixeira.schedulers.executorservice;

public class BackupDB implements Runnable {

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
