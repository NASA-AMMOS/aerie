package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.contrib.cells.register.RegisterCell;
import gov.nasa.jpl.aerie.contrib.cells.register.RegisterEffect;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;

public final class Register<Value> implements DiscreteResource<Value> {
  private final CellRef<RegisterEffect<Value>, RegisterCell<Value>> ref;

  private Register(final Registrar registrar, final Value initialValue) {
    this.ref = registrar.cell(new RegisterCell<>(initialValue));
  }

  public static <Value>
  Register<Value>
  create(final Registrar registrar, final Value initialValue) {
    return new Register<>(registrar, initialValue);
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
