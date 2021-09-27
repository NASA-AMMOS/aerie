package gov.nasa.jpl.aerie.scheduler;

import java.util.List;


/**
 * this filter turns any filter into a filter with resets
 */
public class FilterWithReset implements TimeWindowsFilter {


    public FilterWithReset(TimeRangeExpression reset, TimeWindowsFilter filter){
        this.filter = filter;
        this.resetExpr = reset;
    }

TimeWindowsFilter filter;
TimeRangeExpression resetExpr;

    @Override
    public TimeWindows filter(Plan plan, TimeWindows windows) {

        TimeWindows ret = new TimeWindows();

        if(!windows.isEmpty()){

            //TODO: this is wrong, we should not pass windows
            List<Range<Time>> resetPeriods = resetExpr.computeRange(plan, TimeWindows.of(new Range<Time>(windows.getMinimum(), windows.getMaximum()))).getRangeSet();

            for(var window : resetPeriods) {
                TimeWindows cur = windows.intersectionNew(window);
                ret.union(filter.filter(plan, cur));
            }
        }

        return ret;
    }


}
