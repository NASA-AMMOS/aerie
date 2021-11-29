package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;

/**
 * Interface to a generic state managed by another entity
 *
 * @param <T> the type managed by the state
 */
public interface ExternalState<T> extends QueriableState<T> {


  /**
   * Returns periods during which the state is between a lower and an upper bound
   *
   * @param inf the lower bounds
   * @param sup the upper bound
   * @param timeDomain the domain over which the search is performed
   * @return a set of time intervals
   */
  Windows whenValueBetween(T inf, T sup, Windows timeDomain);

  /**
   * Returns periods during which state is below a value
   *
   * @param val the value
   * @param timeDomain the domain over which the search is performed
   * @return a set of time intervals
   */
  Windows whenValueBelow(T val, Windows timeDomain);

  /**
   * Returns periods in timedomain during which state is above a value
   *
   * @param val the value
   * @param timeDomain the domain over which the search is performed
   * @return a set of time intervals
   */
  Windows whenValueAbove(T val, Windows timeDomain);

  /**
   * Returns periods in timedomain during which state is equal to a value
   *
   * @param val the value
   * @param timeDomain the domain over which the search is performed
   * @return a set of time intervals
   */
  Windows whenValueEqual(T val, Windows timeDomain);

  /**
   * Returns periods in timedomain during which state is constant
   *
   * @param timeDomain the domain over which the search is performed
   * @return a set of time ranges (not TimeWindows because they would be merged together)
   */
  Map<Window, T> getTimeline(Windows timeDomain);

  /**
   * Returns periods in timedomain during which state is not equal to a value
   *
   * @param val the value to compare to
   * @param timeDomain the domain over which the search is performed
   * @return a set of time ranges (not TimeWindows because they would be merged together)
   */
  Windows whenValueNotEqual(T val, Windows timeDomain);


}
