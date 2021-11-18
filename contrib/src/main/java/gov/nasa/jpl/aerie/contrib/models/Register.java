package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.contrib.cells.register.RegisterCell;
import gov.nasa.jpl.aerie.contrib.cells.register.RegisterEffect;
import gov.nasa.jpl.aerie.merlin.framework.Cell;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;

import java.util.function.UnaryOperator;

public final class Register<Value> implements DiscreteResource<Value> {
  public final Cell<Value, RegisterCell<Value>> cell;

  private Register(final UnaryOperator<Value> duplicator, final Value initialValue) {
    this.cell = RegisterCell.allocate(duplicator, initialValue, RegisterEffect::set);
  }


  public static <Value> Register<Value> create(final Value initialValue, final UnaryOperator<Value> duplicator) {
    return new Register<>(duplicator, initialValue);
  }

  /**
   * Creates a register for an <b>immutable</b> type, such as an enum or a primitive.
   *
   * It is not necessary to make defensive copies for such types.
   */
  public static <Value> Register<Value> forImmutable(final Value initialValue) {
    return new Register<>($ -> $, initialValue);
  }

  @Override
  public Value getDynamics() {
    return this.cell.get().getValue();
  }

  public boolean isConflicted() {
    return this.cell.get().isConflicted();
  }

  public void set(final Value value) {
    this.cell.emit(value);
  }

  @Deprecated
  @Override
  public boolean equals(final Object obj) {
    return super.equals(obj);
  }
}
