package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.contrib.cells.register.RegisterCell;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.Model;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class Register<$Schema, Value> extends Model<$Schema> {
  private final ValueMapper<Value> mapper;

  private final Query<$Schema, Pair<Optional<Value>, Set<Value>>, RegisterCell<Value>> query;
  public final DiscreteResource<$Schema, Value> value;
  public final DiscreteResource<$Schema, Boolean> conflicted;

  public Register(
      final Registrar<$Schema> registrar,
      final Value initialValue,
      final ValueMapper<Value> mapper)
  {
    super(registrar);

    this.mapper = Objects.requireNonNull(mapper);

    this.query = registrar.cell(new RegisterCell<>(initialValue));

    this.value = registrar.resource(
        "value",
        DiscreteResource.atom(this.query, RegisterCell::getValue),
        mapper);
    this.conflicted = registrar.resource(
        "conflicted",
        DiscreteResource.atom(this.query, RegisterCell::isConflicted),
        new BooleanValueMapper());
  }

  public static <$Schema>
  Register<$Schema, Double>
  create(final Registrar<$Schema> registrar, final double initialValue) {
    return new Register<>(registrar, initialValue, new DoubleValueMapper());
  }

  public static <$Schema, E extends Enum<E>>
  Register<$Schema, E>
  create(final Registrar<$Schema> registrar, final E initialValue) {
    // SAFETY: Every subclass of `Enum<E>` is final, so `Class<? extends Enum<E>> == Class<E>`.
    @SuppressWarnings("unchecked")
    final var klass = (Class<E>) initialValue.getClass();

    return new Register<>(registrar, initialValue, new EnumValueMapper<>(klass));
  }

  public void set(final Value value) {
    emit(Pair.of(Optional.of(value), Set.of(value)), this.query);
  }

  public Value get() {
    return this.value.ask(now());
  }

  public boolean isConflicted() {
    return this.conflicted.ask(now());
  }

  public Condition<$Schema> isOneOf(final Set<Value> values) {
    return this.value.isOneOf(values, this.mapper);
  }

  @SafeVarargs
  public final Condition<$Schema> isOneOf(final Value... values) {
    return this.isOneOf(Set.of(values));
  }

  public Condition<$Schema> is(final Value value) {
    return this.isOneOf(value);
  }
}
