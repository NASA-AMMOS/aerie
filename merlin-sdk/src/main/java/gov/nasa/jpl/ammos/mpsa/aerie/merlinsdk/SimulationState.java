package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;

import java.util.Map;

public interface SimulationState {
  void applyInScope(Runnable scope);
  Map<String, State<?>> getStates();
}
