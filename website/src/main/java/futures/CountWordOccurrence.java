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
    Set<String> wordsToFilter =
        Set.of(
            "you",
            "was",
            "in",
            "to",
            "and",
            "of",
            "I",
            "a",
            "for",
            "he",
            "had",
            "not",
            "with",
            "it",
            "at",
            "his",
            "she",
            "is",
            "on",
            "her",
            "that",
            "be",
            "have",
            "as",
            "were",
            "what",
            "from",
            "my",
            "are",
            "He",
            "they",
            "been",
            "me",
            "And",
            "your",
            "so",
            "him",
            "all",
            "but",
            "The",
            "am",
            "an",
            "this",
            "out",
            "But",
            "by",
            "would",
            "did",
            "no",
            "or",
            "could",
            "one",
            "about",
            "up",
            "will",
            "said",
            "into",
            "only",
            "like",
            "very",
            "there",
            "do",
            "if",
            "don't",
            "know",
            "has",
            "how",
            "though",
            "some",
            "\"I",
            "She",
            "when",
            "went",
            "who",
            "more",
            "You",
            "don’t,",
            "come",
            "“I",
            "even",
            "must",
            "we",
            "which",
            "him.",
            "them",
            "man",
            "it,",
            "just",
            "go",
            "see",
            "such",
            "It",
            "looked",
            "before",
            "still",
            "began",
            "can",
            "little",
            "him,",
            "it’s",
            "it.",
            "once",
            "last",
            "down",
            "time",
            "came",
            "made",
            "don’t",
            "thought",
            "something",
            "two",
            "should",
            "their",
            "without",
            "now",
            "What",
            "then",
            "you,",
            "tell",
            "almost",
            "the",
            "may",
            "me,");
    CompletableFuture<Map<String, Integer>> cF1 =
        CompletableFuture.supplyAsync(() -> countOccurrence(inputFile));

    CompletableFuture<List<Map.Entry<String, Integer>>> cF2 =
        cF1.thenApply(
            wordsToCount ->
                wordsToCount.entrySet().stream()
                    .filter(p -> !wordsToFilter.contains(p.getKey()))
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .toList());

    List<Map.Entry<String, Integer>> res = cF2.join();
    System.out.println(res.subList(0, 3));
  }
}
