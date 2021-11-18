package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Map;
import java.util.TreeMap;

/**
 * very basic implies working only on booleans
 *
 * @param <T> x
 * @param <E> x
 */
//E is the type of this state and T of the other state on which it is based on
public class ImpliesFromState<T extends Comparable<T>, E extends Comparable<E>> implements ExternalState<E> {

  public <T, E> T getKey(Map<T, E> map, E value) {
    for (Map.Entry<T, E> entry : map.entrySet()) {
      if (entry.getValue().equals(value)) {
        return entry.getKey();
      }
    }
    return null;
  }

  @Override
  public E getValueAtTime(Duration t) {
    return valueMapping.get(state.getValueAtTime(t));
  }

  @Override
  public Windows whenValueBetween(E inf, E sup, Windows timeDomain) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Windows whenValueBelow(E val, Windows timeDomain) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Windows whenValueAbove(E val, Windows timeDomain) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Windows whenValueEqual(E val, Windows timeDomain) {
    return state.whenValueEqual(getKey(valueMapping, val), timeDomain);
  }

  @Override
  public Map<Window, E> getTimeline(Windows timeDomain) {
    Map<Window, E> toReturn = new TreeMap<>();
    state.getTimeline(timeDomain).forEach((key, value) -> toReturn.put(key, valueMapping.get(value)));
    return toReturn;
  }

  @Override
  public Windows whenValueNotEqual(E val, Windows timeDomain) {
    return state.whenValueNotEqual(getKey(valueMapping, val), timeDomain);
  }


  public ImpliesFromState(ExternalState<T> state, Map<T, E> valueMapping) {
    this.valueMapping = valueMapping;
    this.state = state;
  }

  private ExternalState<T> state;
  /**
   * table stating that the value of this state in function of value of external state
   */
  private Map<T, E> valueMapping;


}
