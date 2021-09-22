package gov.nasa.jpl.aerie.scheduler;

import java.util.List;


/**
 * this filter turns any filter into a filter with resets
 */
public class TransformWithReset implements TimeWindowsTransformer{


    public TransformWithReset(TimeRangeExpression reset, TimeWindowsTransformer filter){
        this.transform = filter;
        this.resetExpr = reset;
    }

TimeWindowsTransformer transform;
TimeRangeExpression resetExpr;

    @Override
    public TimeWindows transformWindows(Plan plan, TimeWindows windows) {

        TimeWindows ret = new TimeWindows();

        if(!windows.isEmpty()){

            //TODO: this is wrong, we should not pass windows
            List<Range<Time>> resetPeriods = resetExpr.computeRange(plan, TimeWindows.of(new Range<Time>(windows.getMinimum(), windows.getMaximum()))).getRangeSet();

            for(var window : resetPeriods) {
                TimeWindows cur = windows.intersectionNew(window);
                ret.union(transform.transformWindows(plan, cur));
            }
        }

        return ret;
    }


}
