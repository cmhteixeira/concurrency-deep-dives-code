package futures;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CountWordOccurrence {

  private static Map<String, Integer> countOccurrence(Path filePath) {
    Map<String, Integer> occurrences = new HashMap<>();
    try (Scanner scanner = new Scanner(filePath, StandardCharsets.UTF_8)) {
      while (scanner.hasNext()) {
        String word = scanner.next();
        occurrences.compute(
            word,
            (ignored, currentCount) -> {
              if (currentCount == null) return 1;
              else return currentCount + 1;
            });
      }
    } catch (Exception e) {
      throw new RuntimeException(String.format("Counting word occurrence in '%s'", filePath), e);
    }
    return occurrences;
  }

  public static void main(String[] args) {
    Path inputFile = Paths.get(args[0]);
    CompletableFuture<Map<String, Integer>> cF1 =
        CompletableFuture.supplyAsync(() -> countOccurrence(inputFile));

    System.out.println(cF1.join());
  }
}
