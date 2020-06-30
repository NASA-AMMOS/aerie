package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.events.IndependentStateEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class IndependentStateFactory {
  private final Map<String, Double> registeredStates = new HashMap<>();
  private final Function<String, StateQuery<Double>> model;
  private final Consumer<IndependentStateEvent> emitter;

  public IndependentStateFactory(final Function<String, StateQuery<Double>> model, final Consumer<IndependentStateEvent> emitter) {
    this.model = model;
    this.emitter = emitter;
  }

  public ConsumableState createConsumableState(String name, double initialValue) {
    this.registeredStates.put(name, initialValue);
    return new ConsumableState(name, this.model, this.emitter);
  }

  public SettableState createSettableState(String name, double initialValue) {
    this.registeredStates.put(name, initialValue);
    return new SettableState(name, this.model, this.emitter);
  }

  public Map<String, Double> getRegisteredStates() {
    return Collections.unmodifiableMap(this.registeredStates);
  }
}
