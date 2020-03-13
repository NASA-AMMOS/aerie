package gov.nasa.jpl.ammos.mpsa.aerie.simulation.agents;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.models.SimulationResults;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
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
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ShortParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.StringParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils.Milliseconds;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils.SerializingState;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils.TypeRegistry;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Simulator<States extends StateContainer> {
  static private TypeRegistry registry = new TypeRegistry();
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
    // TODO: Figure out how List and Map state mappers should work.
    //   If we key off of `List.class` only, then we have to be agnostic to the element (or key/value) type parameter.
    //   But `ParameterMapper#getParameterSchema` needs to know precisely what the full structure of the given
    //   type will look like, meaning we have to account for type parameters.
  }

  static private Duration SAMPLING_PERIOD_MS = Duration.fromQuantity(500, TimeUnit.MILLISECONDS);

  private final Plan<States> plan;
  private final MerlinAdaptation<States> adaptation;

  public Simulator(final Plan<States> plan, final MerlinAdaptation<States> adaptation) {
    this.plan = plan;
    this.adaptation = adaptation;
  }

  private <T> SerializingState<T> serializing(final State<T> state) {
    final Method getMethod;
    try {
      // SAFETY: `State<T>` has a method `T get()`
      getMethod = state.getClass().getMethod("get");
    } catch (final NoSuchMethodException ex) {
      throw new Error("Got a State that doesn't implement `get`", ex);
    }

    // SAFETY: `State<T>::get` has return type `T`.
    @SuppressWarnings("unchecked")
    final Class<T> klass = (Class<T>)getMethod.getReturnType();

    return new SerializingState<>(state, registry.get(klass));
  }

  public SimulationResults run() {
    // Get all of the activities from the plan
    final List<Pair<Milliseconds, Activity<States>>> plannedActivities = this.plan.getActivities();
    // TODO: Be notified when the plan changes.

    // TODO: Load planned activities into the scheduler for scheduling.
    // TODO: Be notified when the schedule changes.
    final Instant simulationStartTime = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
    final List<Pair<Instant, ? extends Activity<States>>> scheduledActivities = new ArrayList<>();
    for (final var entry : plannedActivities) {
      final var startTime = simulationStartTime.plus(entry.getKey().value, TimeUnit.MILLISECONDS);
      scheduledActivities.add(Pair.of(startTime, entry.getValue()));
    }

    // TODO: Initialize the requested state models from the adaptation.
    final States stateContainer = this.adaptation.createStateModels();
    // TODO: Work with state models instead of individual states.

    // Initialize a set of tables into which to store state samples periodically.
    // TODO: Work with state models instead of individual states.
    final List<Instant> timestamps = new ArrayList<>();

    final List<SerializingState<?>> serializingStates = new ArrayList<>();
    for (final State<?> state : stateContainer.getStateList()) serializingStates.add(serializing(state));

    // TODO: Make `timelines` a Map keyed off of each state model.
    final List<List<SerializedParameter>> timelines = new ArrayList<>();
    for (final var _serializingState : serializingStates) timelines.add(new ArrayList<>());

    // Simulate the entire plan to completion.
    // Sample all states periodically while simulation is occurring.
    SimulationEngine.simulate(simulationStartTime, scheduledActivities, stateContainer, SAMPLING_PERIOD_MS, (now) -> {
      timestamps.add(now);
      for (int i = 0; i < serializingStates.size(); ++i) {
        timelines.get(i).add(serializingStates.get(i).get());
      }
    });

    return new SimulationResults(timestamps, timelines);
  }
}
