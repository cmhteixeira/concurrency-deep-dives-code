package schedulers.executorservice;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

public class ReadFileTask implements Callable<String> {

  Path fileToRead;

  public ReadFileTask(Path fileToRead) {
    this.fileToRead = fileToRead;
  }

  @Override
  public String call() throws Exception {
    return Files.readString(this.fileToRead);
  }
}
