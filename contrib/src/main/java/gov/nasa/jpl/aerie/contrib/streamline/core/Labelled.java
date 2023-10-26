package gov.nasa.jpl.aerie.contrib.streamline.core;

import java.util.Objects;

/**
 * Attaches name and context to a datum.
 */
public record Labelled<V>(V data, String name) {
  public static <V> Labelled<V> labelled(String name, V data) {
    return new Labelled<>(data, name);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Labelled<?> labelled = (Labelled<?>) o;
    return Objects.equals(data, labelled.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data);
  }
}
