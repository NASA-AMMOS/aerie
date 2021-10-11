package gov.nasa.jpl.aerie.scheduler;

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
  TimeWindows whenValueBetween(T inf, T sup, TimeWindows timeDomain);

  /**
   * Returns periods during which state is below a value
   *
   * @param val the value
   * @param timeDomain the domain over which the search is performed
   * @return a set of time intervals
   */
  TimeWindows whenValueBelow(T val, TimeWindows timeDomain);

  /**
   * Returns periods in timedomain during which state is above a value
   *
   * @param val the value
   * @param timeDomain the domain over which the search is performed
   * @return a set of time intervals
   */
  TimeWindows whenValueAbove(T val, TimeWindows timeDomain);

  /**
   * Returns periods in timedomain during which state is equal to a value
   *
   * @param val the value
   * @param timeDomain the domain over which the search is performed
   * @return a set of time intervals
   */
  TimeWindows whenValueEqual(T val, TimeWindows timeDomain);

  /**
   * Returns periods in timedomain during which state is constant
   *
   * @param timeDomain the domain over which the search is performed
   * @return a set of time ranges (not TimeWindows because they would be merged together)
   */
  Map<Range<Time>, T> getTimeline(TimeWindows timeDomain);

  /**
   * Returns periods in timedomain during which state is not equal to a value
   *
   * @param val the value to compare to
   * @param timeDomain the domain over which the search is performed
   * @return a set of time ranges (not TimeWindows because they would be merged together)
   */
  TimeWindows whenValueNotEqual(T val, TimeWindows timeDomain);


}
