package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete;

import java.util.Objects;

public final class DiscreteDynamics<T> {
  private final T value;

  private DiscreteDynamics(final T value) {
    this.value = value;
  }

  public T value() {
    return this.value;
  }

  public static <T> DiscreteDynamics<T> constant(final T value) {
    return new DiscreteDynamics<>(Objects.requireNonNull(value));
  }

  @Override
  public String toString() {
    return "λt. " + this.value;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof DiscreteDynamics)) return false;
    final var other = (DiscreteDynamics<?>) o;

    return Objects.equals(this.value, other.value);
  }
}
