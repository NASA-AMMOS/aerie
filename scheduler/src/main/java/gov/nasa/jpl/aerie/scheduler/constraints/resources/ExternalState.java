package gov.nasa.jpl.aerie.scheduler.constraints.resources;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

/**
 * Interface to a generic state managed by another entity
 *
 */
public interface ExternalState extends QueriableState {


  /**
   * Returns periods during which the state is between a lower and an upper bound
   *
   * @param inf the lower bounds
   * @param sup the upper bound
   * @param timeDomain the domain over which the search is performed
   * @return a set of time intervals
   */
  Windows whenValueBetween(SerializedValue inf, SerializedValue sup, Windows timeDomain);

  /**
   * Returns periods during which state is below a value
   *
   * @param val the value
   * @param timeDomain the domain over which the search is performed
   * @return a set of time intervals
   */
  Windows whenValueBelow(SerializedValue val, Windows timeDomain);

  /**
   * Returns periods in timedomain during which state is above a value
   *
   * @param val the value
   * @param timeDomain the domain over which the search is performed
   * @return a set of time intervals
   */
  Windows whenValueAbove(SerializedValue val, Windows timeDomain);

  /**
   * Returns periods in timedomain during which state is equal to a value
   *
   * @param val the value
   * @param timeDomain the domain over which the search is performed
   * @return a set of time intervals
   */
  Windows whenValueEqual(SerializedValue val, Windows timeDomain);

  /**
   * Returns periods in timedomain during which state is constant
   *
   * @param timeDomain the domain over which the search is performed
   * @return a set of time ranges (not TimeWindows because they would be merged together)
   */
  Map<Window, SerializedValue> getTimeline(Windows timeDomain);

  /**
   * Returns periods in timedomain during which state is not equal to a value
   *
   * @param val the value to compare to
   * @param timeDomain the domain over which the search is performed
   * @return a set of time ranges (not TimeWindows because they would be merged together)
   */
  Windows whenValueNotEqual(SerializedValue val, Windows timeDomain);


}
