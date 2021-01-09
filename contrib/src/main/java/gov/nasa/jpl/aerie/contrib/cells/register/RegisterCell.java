package gov.nasa.jpl.aerie.contrib.cells.register;

import gov.nasa.jpl.aerie.merlin.framework.Cell;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.Set;

public final class RegisterCell<T> implements Cell<Pair<Optional<T>, Set<T>>, RegisterCell<T>> {
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
  public ConcurrentUpdateTrait<T> effectTrait() {
    return new ConcurrentUpdateTrait<>();
  }

  @Override
  public void react(final Pair<Optional<T>, Set<T>> concurrentValues) {
    concurrentValues.getLeft().ifPresent(newValue -> this.value = newValue);
    this.conflicted = (concurrentValues.getRight().size() > 1);
  }

  public T getValue() {
    return this.value;
  }

  public Boolean isConflicted() {
    return this.conflicted;
  }
}
