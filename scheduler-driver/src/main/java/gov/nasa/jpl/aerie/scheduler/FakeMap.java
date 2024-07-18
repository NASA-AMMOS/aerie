package gov.nasa.jpl.aerie.scheduler;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class FakeMap<K, V> {
  private HashMap<K, V> inner;
  private final Function<K, ?> keyFunc;
  private final Function<V, ?> valueFunc;

  public <T> FakeMap(Function<K, T> keyFunc, Function<V, T> valueFunc) {
    this.inner = new HashMap<>();
    this.keyFunc = keyFunc;
    this.valueFunc = valueFunc;
  }

  public V get(final K key) {
    return this.inner.get(key);
  }

  public V put(final K key, final V value) {
    return this.inner.put(key, value);
  }

  public void putAll(FakeMap<K, V> activityDirectives) {
    for (final var entry : activityDirectives.entrySet()) {
      this.put(entry.getKey(), entry.getValue());
    }
  }

  public V remove(final K key) {
    return this.inner.remove(key);
  }

  void putAll(final Map<? extends K, ? extends V> m) {
    for (final var entry : m.entrySet()) {
      this.put(entry.getKey(), entry.getValue());
    }
  }

  public Collection<V> values() {
    return this.inner.values();
  }

  public Set<Map.Entry<K, V>> entrySet() {
    return this.inner.entrySet();
  }

  public static <T, K, U, M> Collector<T, ?, FakeMap<K, U>> collector(
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends U> valueMapper,
      final Function<K, M> keyFunc,
      final Function<U, M> valueFunc)
  {
    return new Collector<T, FakeMap<K, U>, FakeMap<K, U>>() {
      @Override
      public Supplier<FakeMap<K, U>> supplier() {
        return () -> new FakeMap<K, U>(keyFunc, valueFunc);
      }

      @Override
      public BiConsumer<FakeMap<K, U>, T> accumulator() {
        return (map, entry) -> map.put(keyMapper.apply(entry), valueMapper.apply(entry));
      }

      @Override
      public BinaryOperator<FakeMap<K, U>> combiner() {
        return (map1, map2) -> {
          map1.putAll(map2.inner);
          return map1;
        };
      }

      @Override
      public Function<FakeMap<K, U>, FakeMap<K, U>> finisher() {
        return $ -> $;
      }

      @Override
      public Set<Characteristics> characteristics() {
        return Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));
      }
    };
  }
}
