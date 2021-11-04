package gov.nasa.jpl.aerie.contrib.cells.register;

import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;

import java.util.Objects;
import java.util.function.Function;

public final class RegisterCell<T> {
  private T value;
  private boolean conflicted;

  public RegisterCell(final T initialValue, final boolean conflicted) {
    this.value = Objects.requireNonNull(initialValue);
    this.conflicted = conflicted;
  }

  public static <Event, T> CellRef<Event, RegisterCell<T>>
  allocate(final T initialValue, final Function<Event, RegisterEffect<T>> interpreter) {
    return CellRef.allocate(
        new RegisterCell<>(initialValue, false),
        new RegisterApplicator<>(),
        new RegisterEffect.Trait<>(),
        interpreter);
  }

  public T getValue() {
    return this.value;
  }

  public boolean isConflicted() {
    return this.conflicted;
  }

  @Override
  public String toString() {
    return "{value=%s, conflicted=%s}".formatted(this.getValue(), this.isConflicted());
  }

  public static final class RegisterApplicator<T> implements Applicator<RegisterEffect<T>, RegisterCell<T>> {
    @Override
    public RegisterCell<T> duplicate(final RegisterCell<T> cell) {
      return new RegisterCell<>(cell.value, cell.conflicted);
    }

    @Override
    public void apply(final RegisterCell<T> cell, final RegisterEffect<T> effect) {
      effect.newValue.ifPresent(newValue -> cell.value = newValue);

      if (effect.writes > 0) {
        cell.conflicted = (effect.writes > 1);
      }
    }
  }
}
