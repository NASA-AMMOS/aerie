package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.contrib.cells.register.RegisterCell;
import gov.nasa.jpl.aerie.contrib.cells.register.RegisterEffect;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.framework.Model;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;

import java.util.Set;

public final class Register<Value> extends Model {
  private final CellRef<RegisterEffect<Value>, RegisterCell<Value>> ref;
  public final DiscreteResource<Value> value;
  public final DiscreteResource<Boolean> conflicted;

  public Register(
      final Registrar registrar,
      final Value initialValue,
      final ValueMapper<Value> mapper)
  {
    super(registrar);

    this.ref = registrar.cell(new RegisterCell<>(initialValue));

    this.value = registrar.resource(
        "value",
        () -> this.ref.get().getValue(),
        mapper);
    this.conflicted = registrar.resource(
        "conflicted",
        () -> this.ref.get().isConflicted(),
        new BooleanValueMapper());
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

  public void set(final Value value) {
    this.ref.emit(RegisterEffect.set(value));
  }

  public Value get() {
    return this.value.get();
  }

  public boolean isConflicted() {
    return this.conflicted.get();
  }

  public Condition isOneOf(final Set<Value> values) {
    return this.value.isOneOf(values);
  }

  @SafeVarargs
  public final Condition isOneOf(final Value... values) {
    return this.isOneOf(Set.of(values));
  }

  public Condition is(final Value value) {
    return this.isOneOf(value);
  }
}
