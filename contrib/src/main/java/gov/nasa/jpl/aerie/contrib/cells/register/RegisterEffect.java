package gov.nasa.jpl.aerie.contrib.cells.register;

import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import java.util.Optional;

public final class RegisterEffect<T> {
  public final Optional<T> newValue;
  public final int writes;

  private RegisterEffect(final Optional<T> newValue, final int writes) {
    this.newValue = newValue;
    this.writes = writes;
  }

  private static final RegisterEffect<?> EMPTY = new RegisterEffect<>(Optional.empty(), 0);

  @SuppressWarnings("unchecked")
  public static <T> RegisterEffect<T> doNothing() {
    return (RegisterEffect<T>) EMPTY;
  }

  public static <T> RegisterEffect<T> set(final T newValue) {
    return new RegisterEffect<>(Optional.of(newValue), 1);
  }

  @Override
  public String toString() {
    return (this.newValue.isPresent()) ? "set(%s)".formatted(this.newValue.get()) : "noop()";
  }


  public static final class Trait<T> implements EffectTrait<RegisterEffect<T>> {
    @Override
    public RegisterEffect<T> empty() {
      return RegisterEffect.doNothing();
    }

    @Override
    public RegisterEffect<T> sequentially(final RegisterEffect<T> prefix, final RegisterEffect<T> suffix) {
      return new RegisterEffect<>(
          suffix.newValue.or(() -> prefix.newValue),
          (suffix.writes == 0) ? prefix.writes : suffix.writes);
    }

    @Override
    public RegisterEffect<T> concurrently(final RegisterEffect<T> left, final RegisterEffect<T> right) {
      final var nextValue =
          (left.newValue.isEmpty() || right.newValue.isEmpty())
              ? left.newValue.or(() -> right.newValue)
              : Optional.<T>empty();

      return new RegisterEffect<>(nextValue, left.writes + right.writes);
    }
  }
}
