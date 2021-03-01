package gov.nasa.jpl.aerie.contrib.cells.register;

import java.util.Optional;
import java.util.Set;

public class RegisterEffect<T> {

  public final Optional<T> newValue;
  public final Set<T> conflictingValues;

  public RegisterEffect(Optional<T> newValue, Set<T> conflictingValues) {
    this.newValue = newValue;
    this.conflictingValues = conflictingValues;
  }

  public static <T> RegisterEffect<T> set(final T newValue) {
    return new RegisterEffect<>(Optional.of(newValue), Set.of(newValue));
  }
}
