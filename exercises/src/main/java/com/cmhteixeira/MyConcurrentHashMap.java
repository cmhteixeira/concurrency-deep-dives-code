package com.cmhteixeira;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class MyConcurrentHashMap<K, V> implements ConcurrentMap<K, V> {

  private static int DEFAULT_CAPACITY = 1024;
  private static double TARGET_LOAD_FACTOR = 0.75;
  //  private int m; // number of buckets
  private int n; // number of elements

  private LinkedList<Entry<K, V>>[] backingArray;

  public MyConcurrentHashMap() {
    //    this.m = DEFAULT_CAPACITY;
    this.n = 0;
    this.backingArray = (LinkedList<Entry<K, V>>[]) new LinkedList[DEFAULT_CAPACITY];
  }

  private int bucket(Object obj) {
    return (int) ((obj.hashCode() & 0x00000000ffffffffL) % m());
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
    return get(key) != null;
  }

  @Override
  public boolean containsValue(Object value) {
    for (LinkedList<Entry<K, V>> chain : backingArray) {
      if (chain != null) {
        if (chain.stream().anyMatch(j -> j.getValue() == value)) return true;
      }
    }
    return false;
  }

  @Override
  public V get(Object key) {
    int bucket = bucket(key);
    var chain = backingArray[bucket];
    if (chain == null) return null;
    for (int i = 0; i < chain.size(); ++i) {
      Entry<K, V> entry = chain.get(0);
      if (entry.getKey() == key) return entry.getValue();
    }
    return null;
  }

  private int m() {
    return this.backingArray.length;
  }

  @Override
  public V put(K key, V value) {
    if (key == null) throw new IllegalArgumentException("Keys cannot be null");
    double loadFactor = (double) this.n / m();
    if (loadFactor > 0.75) {
      int newCapacity = m() * 2;
      LinkedList<Map.Entry<K, V>>[] newArray = (LinkedList<Entry<K, V>>[]) new Object[newCapacity];
      System.arraycopy(backingArray, 0, newArray, 0, m());
      backingArray = newArray;
      // TODO: I have to re-hash everything.
      return null;
    } else return putInternal(key, value);
  }

  private V putInternal(K key, V value) {
    int bucket = bucket(key);
    var t = backingArray[bucket];
    if (t == null || t.isEmpty()) {
      LinkedList<Entry<K, V>> chain = new LinkedList<>();
      chain.add(new MyEntry(key, value));
      backingArray[bucket] = chain;
    } else {
      for (int i = 0; i < t.size(); ++i) {
        Entry<K, V> entry = t.get(0);
        if (entry.getKey() == key) {
          V res = entry.getValue();
          entry.setValue(value);
          return res;
        }
      }
      t.add(new MyEntry(key, value));
    }
    ++n;
    return null;
  }

  @Override
  public V remove(Object key) {
    int bucket = bucket(key);
    var t = backingArray[bucket];
    if (t == null || t.isEmpty()) return null;
    else {
      for (int i = 0; i < t.size(); ++i) {
        Entry<K, V> entry = t.get(i);
        if (entry.getKey() == key) {
          V res = entry.getValue();
          t.remove(i);
          --n;
          return res;
        }
      }
    }
    return null;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
      this.put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    Arrays.fill(backingArray, null);
    n = 0;
  }

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
    throw new UnsupportedOperationException("Concurrency not yet implemented.");
  }

  @Override
  public boolean remove(Object key, Object value) {
    int bucket = bucket(key);
    var t = backingArray[bucket];
    if (t == null || t.isEmpty()) return false;
    else {
      for (int i = 0; i < t.size(); ++i) {
        Entry<K, V> entry = t.get(i);
        if (entry.getKey() == key & entry.getValue() == value) {
          t.remove(i);
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    throw new UnsupportedOperationException("Concurrency not yet implemented.");
  }

  @Override
  public V replace(K key, V value) {
    throw new UnsupportedOperationException("Concurrency not yet implemented.");
  }

  private class MyEntry implements Map.Entry<K, V> {

    K key;
    V value;

    MyEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public K getKey() {
      return this.key;
    }

    @Override
    public V getValue() {
      return this.value;
    }

    @Override
    public V setValue(V value) {
      V res = this.value;
      this.value = value;
      return res;
    }
  }
}
