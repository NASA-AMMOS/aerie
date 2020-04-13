package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.SimulationResults;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.TypeRegistry;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.BooleanParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ByteParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.CharacterParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.FloatParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.IntegerParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.LongParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ShortParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.StringParameterMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.defer;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.delay;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.now;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.spawn;

public final class Simulator {
  // TODO: The adaptation must specify mappers for each mission resource; we cannot reliably determine the appropriate
  //   mapping based solely on runtime reflection-based information. This registry is a temporary workaround
  //   for the most primitive kinds of resources.
  private static final TypeRegistry registry = new TypeRegistry();
  static {
    registry.put(Double.class, new DoubleParameterMapper());
    registry.put(Float.class, new FloatParameterMapper());
    registry.put(Boolean.class, new BooleanParameterMapper());
    registry.put(Byte.class, new ByteParameterMapper());
    registry.put(Short.class, new ShortParameterMapper());
    registry.put(Integer.class, new IntegerParameterMapper());
    registry.put(Long.class, new LongParameterMapper());
    registry.put(Character.class, new CharacterParameterMapper());
    registry.put(String.class, new StringParameterMapper());
  }

  private final Adaptation adaptation;

  public Simulator(final Adaptation adaptation) {
    this.adaptation = adaptation;
  }

  public SimulationResults run(
      final Duration samplingDuration,
      final Duration samplingPeriod,
      final Collection<Pair<Duration, Activity>> scheduledActivities
  ) {
    // TODO: Initialize the requested state models from the adaptation.
    final var stateContainer = this.adaptation.newSimulationState();
    // TODO: Work with state models instead of individual states.

    // Initialize a set of tables into which to store state samples periodically.
    // TODO: Work with state models instead of individual states.
    final var timestamps = new ArrayList<Instant>();

    final var states = stateContainer.getStates();

    final var timelines = new HashMap<String, List<SerializedParameter>>(states.size());
    for (final var entry : states.entrySet()) timelines.put(entry.getKey(), new ArrayList<>());

    // Simulate the entire plan to completion.
    // Sample all states periodically while simulation is occurring.
    SimulationEngine.simulate(SimulationInstant.ORIGIN, states.values(), () -> {
      // Spawn all scheduled activities.
      for (final Pair<Duration, Activity> entry : scheduledActivities) {
        defer(entry.getLeft(), entry.getRight());
      }

      // Spawn a sampler.
      if (samplingDuration.isPositive() && samplingPeriod.isPositive()) {
        final var endTime = now().plus(samplingDuration);
        spawn(() -> {
          timestamps.add(now());
          for (final var entry : timelines.entrySet()) {
            final var stateName = entry.getKey();
            final var timeline = entry.getValue();
            timeline.add(serializeState(states.get(stateName)));
          }

          while (now().isBefore(endTime)) {
            delay(Duration.min(samplingPeriod, now().durationTo(endTime)));

            timestamps.add(now());
            for (final var entry : timelines.entrySet()) {
              final var stateName = entry.getKey();
              final var timeline = entry.getValue();
              timeline.add(serializeState(states.get(stateName)));
            }
          }
        });
      }
    });

    return new SimulationResults(timestamps, timelines);
  }

  private static <T> SerializedParameter serializeState(final State<T> state) {
    final T value = state.get();
    final ParameterMapper<T> mapper = registry.get((Class<T>) value.getClass());
    return mapper.serializeParameter(value);
  }
}
