package futures;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ReadWriteFile {
  private static void textFilter(Path in, Path out) {
    try (Scanner input = new Scanner(Files.newInputStream(in), StandardCharsets.UTF_8);
        OutputStream output = Files.newOutputStream(out)) {

      while (input.hasNextLine()) {
        String newLine = input.nextLine().replace("cat", "dog");
        output.write((newLine + "\n").getBytes(StandardCharsets.UTF_8));
      }
    } catch (Exception e) {
      throw new RuntimeException(String.format("Copying '%s' to '%s'.", in, out), e);
    }
  }

  public static void main(String[] args) {
    Path fileIn = Path.of(args[0]);
    Path fileOut = Path.of(args[1]);

    System.out.println("Started ...");
    CompletableFuture<Void> cF1 = CompletableFuture.runAsync(() -> textFilter(fileIn, fileOut));
    System.out.println("Finished ..." + cF1.join());
  }
}
