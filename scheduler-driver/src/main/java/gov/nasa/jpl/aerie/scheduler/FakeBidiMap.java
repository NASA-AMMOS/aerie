package gov.nasa.jpl.aerie.scheduler;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class FakeBidiMap<K, V> {
  private final DualHashBidiMap<K, V> inner;
  private final Function<K, ?> keyFunc;
  private final Function<V, ?> valueFunc;

  public <T> FakeBidiMap(Function<K, T> keyFunc, Function<V, T> valueFunc) {
    this.inner = new DualHashBidiMap<>();
    this.keyFunc = keyFunc;
    this.valueFunc = valueFunc;
  }

  public int size() {
    return this.inner.size();
  }

  public boolean isEmpty() {
    return this.inner.isEmpty();
  }

  public boolean containsKey(final K key) {
    return this.inner.containsKey(key);
  }

  public V get(final K key) {
    return this.inner.get(key);
  }

  public V put(final K k, final V v) {
    return this.inner.put(k, v);
  }

  public V remove(final K key) {
    return this.inner.remove(key);
  }

  public K getKey(final V o) {
    return this.inner.getKey(o);
  }

  public BidiMap<V, K> inverseBidiMap() {
    return this.inner.inverseBidiMap();
  }

  public Set<V> values() {
    return this.inner.values();
  }

  public Set<Map.Entry<K, V>> entrySet() {
    return this.inner.entrySet();
  }
}
