package com.cmhteixeira.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cmhteixeira.MyConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class CmhConcurrentHashMapConcurrencyTest {

  @Test
  public void sizeAfterInsertRemoval() throws InterruptedException {
    MyConcurrentHashMap<String, String> map = new MyConcurrentHashMap<>();
    int numElems = 1000;
    int numThreads = 24;
    var elems =
        IntStream.range(0, numElems).mapToObj(i -> "" + i).map(key -> Map.entry(key, key)).toList();
    List<Thread> threadHandles = new ArrayList<>();
    for (int i = 1; i <= numThreads; ++i) {
      var threadHandle = new Thread(new Runner<>(map, "" + i, elems), "Thread-" + i);
      threadHandle.setDaemon(false);
      threadHandles.add(threadHandle);
      threadHandle.start();
    }

    for (Thread tH : threadHandles) {
      tH.join();
    }

    assertEquals(map.size(), numThreads * numElems);
  }

  static class Runner<V> implements Runnable {
    Map<String, V> map;
    List<Map.Entry<String, V>> entries;
    String prefix;

    Runner(Map<String, V> map, String prefix, List<Map.Entry<String, V>> entries) {
      this.map = map;
      this.entries = entries;
      this.prefix = prefix;
    }

    @Override
    public void run() {
      for (Map.Entry<String, V> entry : entries) {
        map.put(entry.getKey() + "#" + prefix, entry.getValue());
      }
    }
  }
}
