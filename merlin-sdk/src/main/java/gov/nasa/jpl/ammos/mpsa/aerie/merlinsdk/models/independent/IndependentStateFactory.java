package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.events.IndependentStateEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.BooleanValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.EnumValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.LongValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.StringValueMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class IndependentStateFactory {
  private final Map<String, SerializedValue> settableStates = new HashMap<>();
  private final Map<String, Double> cumulableStates = new HashMap<>();

  private final Function<String, StateQuery<SerializedValue>> model;
  private final Consumer<IndependentStateEvent> emitter;

  public IndependentStateFactory(
      final Function<String, StateQuery<SerializedValue>> model,
      final Consumer<IndependentStateEvent> emitter
  ) {
    this.model = model;
    this.emitter = emitter;
  }

  public <T> SettableState<T> settable(final String name, final T initialValue, final ValueMapper<T> mapper) {
    this.settableStates.put(name, mapper.serializeValue(initialValue));

    final var query = this.model.apply(name);

    return new SettableState<>(
        name,
        () -> mapper.deserializeValue(query.get()).getSuccessOrThrow(),
        pred -> query.when(x -> pred.test(mapper.deserializeValue(x).getSuccessOrThrow())),
        value -> this.emitter.accept(IndependentStateEvent.set(name, mapper.serializeValue(value))));
  }

  public DoubleState cumulative(final String name, final double initialValue) {
    final var mapper = new DoubleValueMapper();

    this.cumulableStates.put(name, initialValue);

    final var query = this.model.apply(name);

    return new DoubleState(
        name,
        () -> mapper.deserializeValue(query.get()).getSuccessOrThrow(),
        pred -> query.when(x -> pred.test(mapper.deserializeValue(x).getSuccessOrThrow())),
        value -> this.emitter.accept(IndependentStateEvent.add(name, value)));
  }

  @SuppressWarnings("unchecked")
  public <T extends Enum<T>> SettableState<T> enumerated(final String name, final T initialValue) {
    // SAFETY: Enums are implicitly final, and the only way to define one is to use the `enum` syntax,
    //   so `Class<? extends Enum<T>> == Class<T>`.
    return this.settable(name, initialValue, new EnumValueMapper<>((Class<T>) initialValue.getClass()));
  }

  public SettableState<Double> real(final String name, final double initialValue) {
    return this.settable(name, initialValue, new DoubleValueMapper());
  }

  public SettableState<Long> integer(final String name, final long initialValue) {
    return this.settable(name, initialValue, new LongValueMapper());
  }

  public SettableState<Boolean> bool(final String name, final boolean initialValue) {
    return this.settable(name, initialValue, new BooleanValueMapper());
  }

  public SettableState<String> string(final String name, final String initialValue) {
    return this.settable(name, initialValue, new StringValueMapper());
  }

  public Map<String, SerializedValue> getSettableStates() {
    return Collections.unmodifiableMap(this.settableStates);
  }

  public Map<String, Double> getCumulableStates() {
    return Collections.unmodifiableMap(this.cumulableStates);
  }
}
