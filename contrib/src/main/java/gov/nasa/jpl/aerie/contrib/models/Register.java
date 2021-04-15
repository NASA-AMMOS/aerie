package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.contrib.cells.register.RegisterCell;
import gov.nasa.jpl.aerie.contrib.cells.register.RegisterEffect;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;

public final class Register<Value> implements DiscreteResource<Value> {
  private final CellRef<RegisterEffect<Value>, RegisterCell<Value>> ref;

  private Register(final Value initialValue) {
    this.ref = new CellRef<>(new RegisterCell<>(initialValue));
  }

  public static <Value>
  Register<Value>
  create(final Value initialValue) {
    return new Register<>(initialValue);
  }

  @Override
  public Value getDynamics() {
    return this.ref.get().getValue();
  }

  public boolean isConflicted() {
    return this.ref.get().isConflicted();
  }

  public void set(final Value value) {
    this.ref.emit(RegisterEffect.set(value));
  }
}
