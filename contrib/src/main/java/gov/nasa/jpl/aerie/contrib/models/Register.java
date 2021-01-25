package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.contrib.cells.register.RegisterCell;
import gov.nasa.jpl.aerie.contrib.cells.register.RegisterEffect;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;

public final class Register<Value> implements DiscreteResource<Value> {
  private final CellRef<RegisterEffect<Value>, RegisterCell<Value>> ref;

  public Register(final Registrar registrar, final Value initialValue, final ValueMapper<Value> mapper) {
    this.ref = registrar.cell(new RegisterCell<>(initialValue));

    registrar.resource("value", this, mapper);
    registrar.resource("conflicted", this::isConflicted, new BooleanValueMapper());
  }

  public static
  Register<Double>
  create(final Registrar registrar, final double initialValue) {
    return new Register<>(registrar, initialValue, new DoubleValueMapper());
  }

  public static
  Register<Integer>
  create(final Registrar registrar, final int initialValue) {
    return new Register<>(registrar, initialValue, new IntegerValueMapper());
  }

  public static <E extends Enum<E>>
  Register<E>
  create(final Registrar registrar, final E initialValue) {
    // SAFETY: Every subclass of `Enum<E>` is final, so `Class<? extends Enum<E>> == Class<E>`.
    @SuppressWarnings("unchecked")
    final var klass = (Class<E>) initialValue.getClass();

    return new Register<>(registrar, initialValue, new EnumValueMapper<>(klass));
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
