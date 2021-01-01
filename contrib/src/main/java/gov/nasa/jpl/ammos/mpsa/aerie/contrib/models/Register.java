package gov.nasa.jpl.ammos.mpsa.aerie.contrib.models;

import gov.nasa.jpl.ammos.mpsa.aerie.contrib.cells.register.RegisterCell;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class Register<$Schema, Value> extends Model<$Schema> {
  private final ValueMapper<Value> mapper;

  private final Query<$Schema, Value, RegisterCell<Value>> query;
  public final DiscreteResource<$Schema, Value> value;
  public final DiscreteResource<$Schema, Boolean> conflicted;

  public Register(
      final ResourcesBuilder.Cursor<$Schema> builder,
      final Value initialValue,
      final ValueMapper<Value> mapper)
  {
    super(builder);

    this.mapper = Objects.requireNonNull(mapper);

    this.query = builder.model(
        new RegisterCell<>(initialValue),
        (value) -> Pair.of(Optional.of(value), Set.of(value)));

    this.value = builder.discrete(
        "value",
        now -> now.ask(this.query).getValue(),
        mapper);

    this.conflicted = builder.discrete(
        "conflicted",
        now -> now.ask(this.query).isConflicted(),
        new BooleanValueMapper());
  }

  public static <$Schema>
  Register<$Schema, Double>
  create(final ResourcesBuilder.Cursor<$Schema> builder, final double initialValue) {
    return new Register<>(builder, initialValue, new DoubleValueMapper());
  }

  public static <$Schema, E extends Enum<E>>
  Register<$Schema, E>
  create(final ResourcesBuilder.Cursor<$Schema> builder, final E initialValue) {
    // SAFETY: Every subclass of `Enum<E>` is final, so `Class<? extends Enum<E>> == Class<E>`.
    @SuppressWarnings("unchecked")
    final var klass = (Class<E>) initialValue.getClass();

    return new Register<>(builder, initialValue, new EnumValueMapper<>(klass));
  }

  public void set(final Value value) {
    emit(value, this.query);
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
