package gov.nasa.jpl.aerie.contrib.cells.register;

import gov.nasa.jpl.aerie.merlin.protocol.model.Aggregator;

import java.util.Objects;

public final class RegisterEffect<T> {
  public final T newValue;
  public final boolean conflicted;

  private RegisterEffect(final T newValue, final boolean conflicted) {
    this.newValue = newValue;
    this.conflicted = conflicted;
  }

  private static final RegisterEffect<?> EMPTY = new RegisterEffect<>(null, false);
  private static final RegisterEffect<?> CONFLICTED = new RegisterEffect<>(null, true);

  @SuppressWarnings("unchecked")
  public static <T> RegisterEffect<T> doNothing() {
    return (RegisterEffect<T>) EMPTY;
  }

  public static <T> RegisterEffect<T> set(final T newValue) {
    return new RegisterEffect<>(Objects.requireNonNull(newValue), false);
  }

  @SuppressWarnings("unchecked")
  public static <T> RegisterEffect<T> conflict() {
    return (RegisterEffect<T>) CONFLICTED;
  }

  @Override
  public String toString() {
    return (this.newValue != null) ? "set(%s)".formatted(this.newValue) : "noop()";
  }


  public static final class RegisterAggregator<T> implements Aggregator<RegisterEffect<T>> {
    @Override
    public RegisterEffect<T> empty() {
      return RegisterEffect.doNothing();
    }

    @Override
    public RegisterEffect<T> sequentially(final RegisterEffect<T> prefix, final RegisterEffect<T> suffix) {
      // If `suffix` isn't a no-op, it overrules `prefix`.
      if (suffix.conflicted || suffix.newValue != null) {
        return suffix;
      } else {
        return prefix;
      }
    }

    @Override
    public RegisterEffect<T> concurrently(final RegisterEffect<T> left, final RegisterEffect<T> right) {
      // If neither is null, there's a conflict. Otherwise, pick the one that isn't null.
      if (left.newValue == null) {
        return right;
      } else if (right.newValue == null) {
        return left;
      } else {
        return conflict();
      }
    }
  }
}
