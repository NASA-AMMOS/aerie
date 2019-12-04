package gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ParameterMapper;

public final class SerializingState<T> {
  public final State<T> state;
  public final ParameterMapper<T> mapper;

  public SerializingState(final State<T> state, final ParameterMapper<T> mapper) {
    this.state = state;
    this.mapper = mapper;
  }

  public SerializedParameter get() {
    return this.mapper.serializeParameter(this.state.get());
  }
}
