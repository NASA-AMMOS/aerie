package gov.nasa.jpl.aerie.scheduler;

import com.google.common.collect.RangeMap;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

/**
 * A state proxy is a state allowing to map values from another state (T) to a user-defined domain (X)
 */
public class StateProxy<T extends Comparable<T>, X> implements QueriableState<X> {

  private QueriableState<T> state;

  private RangeMap<T, X> proxyValues;


  public StateProxy(QueriableState<T> state, RangeMap<T, X> proxyValues) {
    this.state = state;
    this.proxyValues = proxyValues;
  }

  public X lookup(Duration time) {
    T val = state.getValueAtTime(time);
    X proxy = proxyValues.get(val);
    if (proxy == null) {
      throw new IllegalArgumentException("mapping is not complete");
    }
    return proxy;
  }


  @Override
  public X getValueAtTime(Duration t) {
    return lookup(t);
  }
}
