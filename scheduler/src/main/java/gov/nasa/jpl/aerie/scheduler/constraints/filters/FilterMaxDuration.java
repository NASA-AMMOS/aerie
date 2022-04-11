package gov.nasa.jpl.aerie.scheduler.constraints.filters;


import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

/**
 * Filter keeping windows with a duration inferior or equal to a defined minimum duration
 */
public class FilterMaxDuration extends FilterFunctional {
  private final Duration maxDuration;

  public FilterMaxDuration(Duration filterByDuration) {
    this.maxDuration = filterByDuration;
  }

  @Override
  public Windows filter(Plan plan, Windows windows) {
    Windows result = new Windows(windows);
    result = result.filterByDuration(Duration.ZERO, this.maxDuration);
    return result;
  }


  @Override
  public boolean shouldKeep(Plan plan, Window range) {
    return range.duration().noLongerThan(maxDuration);
  }
}
