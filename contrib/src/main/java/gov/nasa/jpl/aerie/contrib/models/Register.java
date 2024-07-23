package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.contrib.cells.register.RegisterCell;
import gov.nasa.jpl.aerie.contrib.cells.register.RegisterEffect;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;

import java.util.function.UnaryOperator;

public final class Register<Value> implements DiscreteResource<Value> {
  public final CellRef<Value, RegisterCell<Value>> ref;
  private final UnaryOperator<Value> duplicator;

  private Register(final UnaryOperator<Value> duplicator, final Value initialValue, final ValueMapper<Value> mapper) {
    this.ref = RegisterCell.allocate(duplicator, initialValue, RegisterEffect::set, mapper);
    this.duplicator = duplicator;
  }


  public static <Value> Register<Value> create(final Value initialValue, final UnaryOperator<Value> duplicator, final ValueMapper<Value> mapper) {
    return new Register<>(duplicator, initialValue, mapper);
  }

  public static Register<Double> forImmutable(final double initialValue) {
    return new Register<>($ -> $, initialValue, new DoubleValueMapper());
  }

  public static Register<Integer> forImmutable(final int initialValue) {
    return new Register<>($ -> $, initialValue, new IntegerValueMapper());
  }

  public static Register<String> forImmutable(final String initialValue) {
    return new Register<>($ -> $, initialValue, new StringValueMapper());
  }

  public static Register<Boolean> forImmutable(final boolean initialValue) {
    return new Register<>($ -> $, initialValue, new BooleanValueMapper());
  }

  /**
   * Creates a register for an <b>immutable</b> type, such as an enum or a primitive.
   *
   * It is not necessary to make defensive copies for such types.
   */
  public static <Value> Register<Value> forImmutable(final Value initialValue, final ValueMapper<Value> mapper) {
    return new Register<>($ -> $, initialValue, mapper);
  }

  @Override
  public Value getDynamics() {
    return this.duplicator.apply(this.ref.get().value);
  }

  public boolean isConflicted() {
    return this.ref.get().isConflicted();
  }

  public void set(final Value value) {
    this.ref.emit(value);
  }

  @Deprecated
  @Override
  public boolean equals(final Object obj) {
    return super.equals(obj);
  }
}
