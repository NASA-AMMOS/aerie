package gov.nasa.jpl.aerie.contrib.cells.register;

import java.util.List;
import java.util.Optional;

public class RegisterEffect<T> {

  public final Optional<T> newValue;
  public final List<T> conflictingValues;

  public RegisterEffect(Optional<T> newValue, List<T> conflictingValues) {
    this.newValue = newValue;
    this.conflictingValues = conflictingValues;
  }

  public static <T> RegisterEffect<T> set(final T newValue) {
    return new RegisterEffect<>(Optional.of(newValue), List.of(newValue));
  }
}
