package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Module;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models.RegisterModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.BooleanValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.EnumValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.Set;

public final class RegisterModule<$Schema, Value> extends Module<$Schema> {
  private final Query<$Schema, Value, RegisterModel<Value>> query;
  public final DiscreteResource<History<? extends $Schema>, Value> value;
  public final DiscreteResource<History<? extends $Schema>, Boolean> conflicted;

  public RegisterModule(
      final ResourcesBuilder.Cursor<$Schema> builder,
      final Value initialValue,
      final ValueMapper<Value> mapper)
  {
    super(builder);

    this.query = builder.model(
        new RegisterModel<>(initialValue),
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
  RegisterModule<$Schema, Double>
  create(final ResourcesBuilder.Cursor<$Schema> builder, final double initialValue) {
    return new RegisterModule<>(builder, initialValue, new DoubleValueMapper());
  }

  public static <$Schema, E extends Enum<E>>
  RegisterModule<$Schema, E>
  create(final ResourcesBuilder.Cursor<$Schema> builder, final E initialValue) {
    // SAFETY: Every subclass of `Enum<E>` is final, so `Class<? extends Enum<E>> == Class<E>`.
    @SuppressWarnings("unchecked")
    final var klass = (Class<E>) initialValue.getClass();

    return new RegisterModule<>(builder, initialValue, new EnumValueMapper<>(klass));
  }

  public void set(final Value value) {
    emit(value, this.query);
  }

  public Value get() {
    return this.value.getDynamics(now()).getDynamics();
  }

  public boolean isConflicted() {
    return this.conflicted.getDynamics(now()).getDynamics();
  }
}
