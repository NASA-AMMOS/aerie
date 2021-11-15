package gov.nasa.jpl.aerie.scheduler;


/**
 * Filter keeping windows with a duration superior or equal to a defined minimum duration
 */
public class FilterMinDuration extends FilterFunctional {
  private final Duration minDuration;

  public FilterMinDuration(Duration filterByDuration) {
    this.minDuration = filterByDuration;
  }

  @Override
  public TimeWindows filter(Plan plan, TimeWindows windows) {
    TimeWindows result = new TimeWindows(windows);
    result.doNotMergeAdjacent();
    result.filterByDuration(this.minDuration, null);
    return result;
  }

  @Override
  public boolean shouldKeep(Plan plan, Range<Time> range) {
    return range.getMaximum().minus(range.getMinimum()).compareTo(minDuration) >= 0;
  }

}
