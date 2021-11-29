package gov.nasa.jpl.aerie.contrib.cells.register;

import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

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


  public static final class Trait<T> implements EffectTrait<RegisterEffect<T>> {
    @Override
    public RegisterEffect<T> empty() {
      return RegisterEffect.doNothing();
    }

    @Override
    public RegisterEffect<T> sequentially(final RegisterEffect<T> prefix, final RegisterEffect<T> suffix) {
      if (suffix.newValue != null) {
        // The suffix is a set (with or without a subsequent conflict); it strictly dominates the prefix.
        return suffix;
      } else if (!suffix.conflicted) {
        // The suffix is a no-op; take the prefix.
        return prefix;
      } else if (prefix.newValue != null) {
        // The suffix is a pure conflict, and the prefix performed a valid write.
        return new RegisterEffect<>(prefix.newValue, suffix.conflicted);
      } else {
        // Both suffix and prefix are pure conflicts, so the suffix dominates the prefix.
        return suffix;
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
