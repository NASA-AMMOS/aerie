package gov.nasa.jpl.ammos.mpsa.aerie.contrib.cells.register;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Cell;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.DelimitedDynamics;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.Set;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.DelimitedDynamics.persistent;

public final class RegisterCell<T> implements Cell<Pair<Optional<T>, Set<T>>, RegisterCell<T>> {
  private T _value;
  private boolean _conflicted;

  public RegisterCell(final T initialValue, final boolean conflicted) {
    this._value = initialValue;
    this._conflicted = conflicted;
  }

  public RegisterCell(final T initialValue) {
    this(initialValue, false);
  }

  @Override
  public RegisterCell<T> duplicate() {
    return new RegisterCell<>(this._value, this._conflicted);
  }

  @Override
  public ConcurrentUpdateTrait<T> effectTrait() {
    return new ConcurrentUpdateTrait<>();
  }

  @Override
  public void react(final Pair<Optional<T>, Set<T>> concurrentValues) {
    concurrentValues.getLeft().ifPresent(newValue -> this._value = newValue);
    this._conflicted = (concurrentValues.getRight().size() > 1);
  }


  /// Resources
  public DelimitedDynamics<T> getValue() {
    return persistent(this._value);
  }

  public DelimitedDynamics<Boolean> isConflicted() {
    return persistent(this._conflicted);
  }
}
