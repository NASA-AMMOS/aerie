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
      final String namespace,
      final ResourcesBuilder<$Schema> builder,
      final Value initialValue,
      final ValueMapper<Value> mapper)
  {
    this.query = builder.model(
        new RegisterModel<>(initialValue),
        (value) -> Pair.of(Optional.of(value), Set.of(value)));

    this.value = builder
        .discrete(
            namespace + ".value",
            now -> RegisterModel.<Value>value().getDynamics(now.ask(this.query)), mapper)
        ::getDynamics;

    this.conflicted = builder
        .discrete(
            namespace + ".conflicted",
            now -> RegisterModel.conflicted.getDynamics(now.ask(this.query)), new BooleanValueMapper())
        ::getDynamics;
  }

  public static <$Schema>
  RegisterModule<$Schema, Double>
  create(
      final String namespace,
      final ResourcesBuilder<$Schema> builder,
      final double initialValue)
  {
    return new RegisterModule<>(namespace, builder, initialValue, new DoubleValueMapper());
  }

  public static <$Schema, E extends Enum<E>>
  RegisterModule<$Schema, E>
  create(
      final String namespace,
      final ResourcesBuilder<$Schema> builder,
      final E initialValue)
  {
    // SAFETY: Every subclass of `Enum<E>` is final, so `Class<? extends Enum<E>> == Class<E>`.
    @SuppressWarnings("unchecked")
    final var klass = (Class<E>) initialValue.getClass();

    return new RegisterModule<>(namespace, builder, initialValue, new EnumValueMapper<>(klass));
  }

  public void set(final Value value) {
    emit(value, this.query);
  }

  public Value get() {
    return ask(this.value);
  }

  public boolean isConflicted() {
    return ask(this.conflicted);
  }
}
