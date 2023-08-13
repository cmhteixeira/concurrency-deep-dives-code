package com.cmhteixeira.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cmhteixeira.MyConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class CmhConcurrentHashMapConcurrencyTest {

  @Test
  public void sizeAfterInsertRemoval() {
    MyConcurrentHashMap<String, Integer> map = new MyConcurrentHashMap<>();
    int numEntries = 1000;
    List<Map.Entry<String, Integer>> entries =
        IntStream.range(0, numEntries).mapToObj(i -> Map.entry(String.valueOf(i), i)).toList();

    IntStream.range(0, 10)
        .mapToObj(
            value ->
                new CmhConcurrentHashMapConcurrencyTest.Runner<>(
                    map, String.valueOf(value), entries))
        .map(
            thread -> {
              thread.start();
              return thread;
            })
        .forEach(
            l -> {
              try {
                l.join();
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            });

    assertEquals(map.size(), numEntries * 10);
  }

  static class Runner<V> extends Thread {
    MyConcurrentHashMap<String, V> map;
    List<Map.Entry<String, V>> entries;
    String prefix;

    Runner(MyConcurrentHashMap<String, V> map, String prefix, List<Map.Entry<String, V>> entries) {
      this.map = map;
      this.entries = entries;
      this.prefix = prefix;
    }

    @Override
    public void run() {
      for (Map.Entry<String, V> entry : entries) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        System.out.println(
            "Inserting: " + entry.getKey() + " from: " + Thread.currentThread().getName());
        map.put(entry.getKey() + prefix, entry.getValue());
      }
    }
  }
}
