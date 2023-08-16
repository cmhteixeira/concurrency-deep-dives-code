package com.cmhteixeira;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class MyConcurrentHashMap2<K, V> implements ConcurrentMap<K, V> {

  private static final int DEFAULT_CAPACITY = 1024;
  private static final double TARGET_LOAD_FACTOR = 0.75f;
  //  private int m; // number of buckets
  private int n; // number of elements

  private Node<K, V>[] table;

  public MyConcurrentHashMap2() {
    this(DEFAULT_CAPACITY);
  }

  public MyConcurrentHashMap2(int initialCapacity) {
    this.n = 0;
    this.table = new Node[initialCapacity];
  }

  private static int bucket(Object obj, int m) {
    return (int) ((obj.hashCode() & 0x00000000ffffffffL) % m);
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
    for (Node<K, V> chain : table) {
      if (chain != null) {
        do {
          if (chain.value == value) return true;
        } while (chain.next != null);
      }
    }
    return false;
  }

  @Override
  public V get(Object key) {
    int bucket = bucket(key, m());
    var chain = table[bucket];
    if (chain == null) return null;
    Node<K, V> node = chain;
    while (node != null) {
      if (Objects.equals(node.key, key)) return node.getValue();
      node = node.next;
    }
    return null;
  }

  private int m() {
    return this.table.length;
  }

  @Override
  public V put(K key, V value) {
    if (key == null) throw new IllegalArgumentException("Keys cannot be null");
    if (((double) this.n / m()) > TARGET_LOAD_FACTOR) {
      Node<K, V>[] newTable = new Node[m() * 2];
      for (Node<K, V> chain : table) {
        Node<K, V> node = chain;
        while (node != null) {
          putInternal2(node.getKey(), node.getValue(), newTable);
          node = node.next;
        }
      }
      table = newTable;
    }
    return putInternal(key, value);
  }

  private V putInternal(K key, V value) {
    int bucket = bucket(key, m());
    var chain = table[bucket];
    if (chain == null) table[bucket] = new Node<>(key, value);
    else {
      Node<K, V> node = chain;
      do {
        if (Objects.equals(node.getKey(), key)) {
          V res = node.getValue();
          node.setValue(value);
          return res;
        }
        node = node.next;
      } while (node != null);
      table[bucket] = new Node<>(key, value, chain);
    }
    ++n;
    return null;
  }

  private static <K, V> V putInternal2(K key, V value, Node<K, V>[] table) {
    int bucket = bucket(key, table.length);
    var chain = table[bucket];
    if (chain == null) table[bucket] = new Node<>(key, value);
    else {
      Node<K, V> node = chain;
      do {
        if (Objects.equals(node.getKey(), key)) {
          V res = node.getValue();
          node.setValue(value);
          return res;
        }
        node = node.next;
      } while (node != null);
      table[bucket] = new Node<>(key, value, chain);
    }
    return null;
  }

  @Override
  public V remove(Object key) {
    return removeFinal(key, null);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
      this.put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    Arrays.fill(table, null);
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
    return removeFinal(key, value) != null;
  }

  V removeFinal(Object key, Object value) {
    int bucket = bucket(key, m());
    var chain = table[bucket];
    if (chain == null) return null;
    if ((chain.key == key && value == null) || (chain.key == key && chain.value == value)) {
      V res = chain.getValue();
      table[bucket] = chain.next;
      --n;
      return res;
    }
    Node<K, V> node = chain;
    while (node.next != null) {
      if ((node.next.getKey() == key && value == null)
          || (node.next.getKey() == key && node.next.value == value)) {
        V res = node.next.getValue();
        node.next = node.next.next;
        --n;
        return res;
      }
      node = chain.next;
    }
    return null;
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    throw new UnsupportedOperationException("Concurrency not yet implemented.");
  }

  @Override
  public V replace(K key, V value) {
    throw new UnsupportedOperationException("Concurrency not yet implemented.");
  }

  public static class Node<K, V> implements Map.Entry<K, V> {

    K key;
    V value;

    Node<K, V> next;

    public Node(K key, V value) {
      this.key = Objects.requireNonNull(key);
      this.value = Objects.requireNonNull(value);
    }

    public Node(K key, V value, Node<K, V> next) {
      this.key = Objects.requireNonNull(key);
      this.value = Objects.requireNonNull(value);
      this.next = next;
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

    @Override
    public String toString() {
      return "Node{" + "key=" + key + ", value=" + value + ", next=" + next + '}';
    }
  }
}
