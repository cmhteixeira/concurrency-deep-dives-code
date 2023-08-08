package com.cmhteixeira.concurrency;

import static org.junit.jupiter.api.Assertions.*;

import com.cmhteixeira.MyConcurrentHashMap;
import java.util.*;
import org.junit.jupiter.api.Test;

public class MyConcurrentHashMapTest {

  @Test
  public void emptyDoesNotContain() {
    Map<String, String> map = new MyConcurrentHashMap<>();
    assertNull(map.get("foo"));
  }

  @Test
  public void contains() {
    Map<String, String> map = new MyConcurrentHashMap<>();
    map.put("foo", "bar");
    assertEquals(map.get("foo"), "bar");
  }

  @Test
  public void containsAfterRemove() {
    Map<String, String> map = new MyConcurrentHashMap<>();
    map.put("foo", "bar");
    map.remove("foo");
    assertNull(map.get("foo"));
  }

  @Test
  public void size1() {
    Map<String, String> map = new MyConcurrentHashMap<>();
    map.put("foo", "bar");
    assertEquals(map.size(), 1);
  }

  @Test
  public void sizeAfterInsertRemoval() {
    Map<String, String> map = new MyConcurrentHashMap<>();
    map.put("foo", "bar");
    map.put("foo-Bar", "baz");
    map.remove("foo");
    assertEquals(map.size(), 1);
  }

  @Test
  public void containsValue() {
    Map<String, String> map = new MyConcurrentHashMap<>();
    map.put("foo", "bar");
    assertTrue(map.containsValue("bar"));
  }

  @Test
  public void clear() {
    Map<String, String> map = new MyConcurrentHashMap<>();
    map.put("foo", "bar");
    map.clear();
    assertEquals(0, map.size());
    assertFalse(map.containsKey("foo"));
  }

  @Test
  public void randomTest() {
    var myMap = new MyConcurrentHashMap<>(10);
    int maxIter = 10_000;
    Set<Map.Entry<String, Integer>> entries = new HashSet<>();
    for (int i = 1; i <= maxIter; ++i) {
      Map.Entry<String, Integer> entry = Map.entry(UUID.randomUUID().toString(), i);
      entries.add(entry);
      myMap.put(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, Integer> entry : entries) {
      assertTrue(myMap.containsKey(entry.getKey()));
    }
  }
}
