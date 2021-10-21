package gov.nasa.jpl.aerie.contrib.cells.register;

import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RegisterEffect<T> {
  public final Optional<T> newValue;
  public final List<T> conflictingValues;

  private RegisterEffect(final Optional<T> newValue, final List<T> conflictingValues) {
    this.newValue = newValue;
    this.conflictingValues = conflictingValues;
  }

  public static <T> RegisterEffect<T> doNothing() {
    return new RegisterEffect<>(Optional.empty(), List.of());
  }

  public static <T> RegisterEffect<T> set(final T newValue) {
    return new RegisterEffect<>(Optional.of(newValue), List.of(newValue));
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
          (suffix.conflictingValues.isEmpty()) ? prefix.conflictingValues : suffix.conflictingValues);
    }

    @Override
    public RegisterEffect<T> concurrently(final RegisterEffect<T> left, final RegisterEffect<T> right) {
      final var nextValue =
          (left.newValue.isEmpty() || right.newValue.isEmpty())
              ? left.newValue.or(() -> right.newValue)
              : Optional.<T>empty();

      final var conflictingValues = new ArrayList<T>(left.conflictingValues.size() + right.conflictingValues.size());
      conflictingValues.addAll(left.conflictingValues);
      conflictingValues.addAll(right.conflictingValues);

      return new RegisterEffect<>(nextValue, conflictingValues);
    }
  }
}
