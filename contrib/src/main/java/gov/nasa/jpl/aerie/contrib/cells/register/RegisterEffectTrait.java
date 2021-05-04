package gov.nasa.jpl.aerie.contrib.cells.register;

import gov.nasa.jpl.aerie.merlin.protocol.EffectTrait;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class RegisterEffectTrait<T> implements EffectTrait<RegisterEffect<T>> {
  @Override
  public RegisterEffect<T> empty() {
    return new RegisterEffect<>(Optional.empty(), Set.of());
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

    final var set = new HashSet<>(left.conflictingValues);
    set.addAll(right.conflictingValues);

    return new RegisterEffect<>(nextValue, set);
  }
}
