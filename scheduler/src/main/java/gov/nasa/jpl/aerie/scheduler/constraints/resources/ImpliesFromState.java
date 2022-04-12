package gov.nasa.jpl.aerie.scheduler.constraints.resources;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;
import java.util.TreeMap;

/**
 * very basic implies working only on booleans
 *
 */
//E is the type of this state and T of the other state on which it is based on
public class ImpliesFromState implements ExternalState {

  public <T, E> T getKey(Map<T, E> map, E value) {
    for (Map.Entry<T, E> entry : map.entrySet()) {
      if (entry.getValue().equals(value)) {
        return entry.getKey();
      }
    }
    return null;
  }

  @Override
  public SerializedValue getValueAtTime(Duration t) {
    return valueMapping.get(state.getValueAtTime(t));
  }

  @Override
  public Windows whenValueBetween(SerializedValue inf, SerializedValue sup, Windows timeDomain) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Windows whenValueBelow(SerializedValue val, Windows timeDomain) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Windows whenValueAbove(SerializedValue val, Windows timeDomain) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Windows whenValueEqual(SerializedValue val, Windows timeDomain) {
    return state.whenValueEqual(getKey(valueMapping, val), timeDomain);
  }

  @Override
  public Map<Window, SerializedValue> getTimeline(Windows timeDomain) {
    Map<Window, SerializedValue> toReturn = new TreeMap<>();
    state.getTimeline(timeDomain).forEach((key, value) -> toReturn.put(key, valueMapping.get(value)));
    return toReturn;
  }

  @Override
  public Windows whenValueNotEqual(SerializedValue val, Windows timeDomain) {
    return state.whenValueNotEqual(getKey(valueMapping, val), timeDomain);
  }


  public ImpliesFromState(ExternalState state, Map<SerializedValue, SerializedValue> valueMapping) {
    this.valueMapping = valueMapping;
    this.state = state;
  }

  private final ExternalState state;
  /**
   * table stating that the value of this state in function of value of external state
   */
  private final Map<SerializedValue, SerializedValue> valueMapping;


}
