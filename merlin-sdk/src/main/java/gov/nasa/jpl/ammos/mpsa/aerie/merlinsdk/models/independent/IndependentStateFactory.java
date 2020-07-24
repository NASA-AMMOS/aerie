package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.events.IndependentStateEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.BooleanParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.EnumParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.LongParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.StringParameterMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class IndependentStateFactory {
  private final Map<String, SerializedParameter> settableStates = new HashMap<>();
  private final Map<String, Double> cumulableStates = new HashMap<>();

  private final Function<String, StateQuery<SerializedParameter>> model;
  private final Consumer<IndependentStateEvent> emitter;

  public IndependentStateFactory(
      final Function<String, StateQuery<SerializedParameter>> model,
      final Consumer<IndependentStateEvent> emitter
  ) {
    this.model = model;
    this.emitter = emitter;
  }

  public <T> SettableState<T> settable(final String name, final T initialValue, final ParameterMapper<T> mapper) {
    this.settableStates.put(name, mapper.serializeParameter(initialValue));

    final var query = this.model.apply(name);

    return new SettableState<>(
        name,
        () -> mapper.deserializeParameter(query.get()).getSuccessOrThrow(),
        pred -> query.when(x -> pred.test(mapper.deserializeParameter(x).getSuccessOrThrow())),
        value -> this.emitter.accept(IndependentStateEvent.set(name, mapper.serializeParameter(value))));
  }

  public DoubleState cumulative(final String name, final double initialValue) {
    final var mapper = new DoubleParameterMapper();

    this.cumulableStates.put(name, initialValue);

    final var query = this.model.apply(name);

    return new DoubleState(
        name,
        () -> mapper.deserializeParameter(query.get()).getSuccessOrThrow(),
        pred -> query.when(x -> pred.test(mapper.deserializeParameter(x).getSuccessOrThrow())),
        value -> this.emitter.accept(IndependentStateEvent.add(name, value)));
  }

  @SuppressWarnings("unchecked")
  public <T extends Enum<T>> SettableState<T> enumerated(final String name, final T initialValue) {
    // SAFETY: Enums are implicitly final, and the only way to define one is to use the `enum` syntax,
    //   so `Class<? extends Enum<T>> == Class<T>`.
    return this.settable(name, initialValue, new EnumParameterMapper<>((Class<T>) initialValue.getClass()));
  }

  public SettableState<Double> real(final String name, final double initialValue) {
    return this.settable(name, initialValue, new DoubleParameterMapper());
  }

  public SettableState<Long> integer(final String name, final long initialValue) {
    return this.settable(name, initialValue, new LongParameterMapper());
  }

  public SettableState<Boolean> bool(final String name, final boolean initialValue) {
    return this.settable(name, initialValue, new BooleanParameterMapper());
  }

  public SettableState<String> string(final String name, final String initialValue) {
    return this.settable(name, initialValue, new StringParameterMapper());
  }

  public Map<String, SerializedParameter> getSettableStates() {
    return Collections.unmodifiableMap(this.settableStates);
  }

  public Map<String, Double> getCumulableStates() {
    return Collections.unmodifiableMap(this.cumulableStates);
  }
}
