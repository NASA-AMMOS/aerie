package gov.nasa.jpl.aerie.scheduler.constraints.resources;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

/**
 * A state proxy is a state allowing to map values from another state (T) to a user-defined domain (X)
 */
public class StateProxy implements QueriableState {

  private final QueriableState state;

  private final Map<SerializedValue, SerializedValue> proxyValues;


  public StateProxy(QueriableState state, Map<SerializedValue, SerializedValue> proxyValues) {
    this.state = state;
    this.proxyValues = proxyValues;
  }

  public SerializedValue lookup(Duration time) {
    var val = state.getValueAtTime(time);
    var proxy = proxyValues.get(val);
    if (proxy == null) {
      throw new IllegalArgumentException("mapping is not complete");
    }
    return proxy;
  }


  @Override
  public SerializedValue getValueAtTime(Duration t) {
    return lookup(t);
  }
}
