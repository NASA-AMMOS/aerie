package gov.nasa.jpl.aerie.scheduler;


/**
 * Filter keeping windows with a duration inferior or equal to a defined minimum duration
 */
public class FilterMaxDuration extends FilterFunctional{
    private final Duration maxDuration;

    public FilterMaxDuration(Duration filterByDuration) {
        this.maxDuration = filterByDuration;
    }

    @Override
    public TimeWindows filter(Plan plan, TimeWindows windows) {
        TimeWindows result = new TimeWindows(windows);
        result.doNotMergeAdjacent();
        result.filterByDuration(null, this.maxDuration);
        return result;
    }


    @Override
    public boolean shouldKeep(Plan plan, Range<Time> range) {
        return range.getMaximum().minus(range.getMinimum()).compareTo(maxDuration) <= 0;
    }
}
