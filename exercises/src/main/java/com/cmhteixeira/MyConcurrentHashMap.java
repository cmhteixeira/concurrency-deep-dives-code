package com.cmhteixeira;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class MyConcurrentHashMap<K, V> implements ConcurrentMap<K, V> {

  private static int DEFAULT_CAPACITY = 1024;
  private static double TARGET_LOAD_FACTOR = 0.75;
  private int m; // number of buckets
  private int n; // number of elements

  private LinkedList<?>[] backingArray;

  public MyConcurrentHashMap() {
    this.m = DEFAULT_CAPACITY;
    this.n = 0;
    this.backingArray = (LinkedList<Entry>[]) new Object[DEFAULT_CAPACITY];
  }

  private int bucket(Object obj) {
    return obj.hashCode() % m;
  }

  @Override
  public int size() {
    return n;
  }

  @Override
  public boolean isEmpty() {
    return n == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    return false;
  }

  @Override
  public V get(Object key) {
    return null;
  }

  @Override
  public V put(K key, V value) {
    int bucket = bucket(key);
  }

  @Override
  public V remove(Object key) {
    return null;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {}

  @Override
  public void clear() {}

  @Override
  public Set<K> keySet() {
    return null;
  }

  @Override
  public Collection<V> values() {
    return null;
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return null;
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return null;
  }

  @Override
  public boolean remove(Object key, Object value) {
    return false;
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return false;
  }

  @Override
  public V replace(K key, V value) {
    return null;
  }

  private class MyEntry {
    K key;
    List<V> chain;

    MyEntry(K key, List<V> chain) {
      this.chain = chain;
      this.key = key;
    }
  }
}
