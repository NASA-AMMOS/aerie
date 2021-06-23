package gov.nasa.jpl.aerie.contrib.cells.register;

import gov.nasa.jpl.aerie.merlin.framework.Cell;

public final class RegisterCell<T> implements Cell<RegisterEffect<T>, RegisterCell<T>> {
  private T value;
  private boolean conflicted;

  public RegisterCell(final T initialValue, final boolean conflicted) {
    this.value = initialValue;
    this.conflicted = conflicted;
  }

  public RegisterCell(final T initialValue) {
    this(initialValue, false);
  }

  @Override
  public RegisterCell<T> duplicate() {
    return new RegisterCell<>(this.value, this.conflicted);
  }

  @Override
  public RegisterEffectTrait<T> effectTrait() {
    return new RegisterEffectTrait<>();
  }

  @Override
  public void react(final RegisterEffect<T> concurrentValues) {
    concurrentValues.newValue.ifPresent(newValue -> this.value = newValue);

    if (concurrentValues.conflictingValues.size() > 0) {
      this.conflicted = (concurrentValues.conflictingValues.size() > 1);
    }
  }

  public T getValue() {
    return this.value;
  }

  public Boolean isConflicted() {
    return this.conflicted;
  }
}
