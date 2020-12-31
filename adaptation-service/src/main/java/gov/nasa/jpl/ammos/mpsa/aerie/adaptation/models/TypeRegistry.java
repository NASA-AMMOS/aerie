package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ValueMapper;

import java.util.HashMap;
import java.util.Map;

public final class TypeRegistry {
  // INVARIANT: For every key `Class<T>` in `map`, the associated value implements `ParameterMapper<T>`.
  private final Map<Class<?>, ValueMapper<?>> map = new HashMap<>();

  public <T> void put(final Class<T> klass, final ValueMapper<T> mapper) {
    // SAFETY: The invariant on `this.map` is sustained. The method signature guarantees that, for every T,
    // a given `Class<T>` is associated with a given `ParameterMapper<T>`.
    this.map.put(klass, mapper);
  }

  public <T> ValueMapper<T> get(final Class<T> klass) {
    // SAFETY: The invariant on `this.map` ensures that this cast is valid.
    @SuppressWarnings("unchecked")
    final var result = (ValueMapper<T>)this.map.get(klass);
    return result;
  }

  public <T> ValueMapper<T> getOrDefault(final Class<T> klass, final ValueMapper<T> defaultValue) {
    // SAFETY: The invariant on `this.map` ensures that this cast is valid.
    @SuppressWarnings("unchecked")
    final var result = (ValueMapper<T>)this.map.getOrDefault(klass, defaultValue);
    return result;
  }
}
