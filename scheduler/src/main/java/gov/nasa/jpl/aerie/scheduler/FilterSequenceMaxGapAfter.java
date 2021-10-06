package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter windows that have at least another window preceding ending within a delay
 */
public class FilterSequenceMaxGapAfter implements TimeWindowsFilter {

    private Duration maxDelay;

    public FilterSequenceMaxGapAfter(Duration maxDelay){
        this.maxDelay = maxDelay;
    }

    @Override
    public TimeWindows filter(Plan plan, TimeWindows windows) {
        List<Range<Time>> filtered = new ArrayList<Range<Time>>();
        List<Range<Time>> windowsTo = windows.getRangeSet();
        if(windowsTo.size() > 1) {
            int nextInd = 1;
            while(nextInd < windowsTo.size()){
                Range<Time> after = windowsTo.get(nextInd);
                Range<Time>  cur = windowsTo.get(nextInd-1);

                if (after.getMinimum().minus(cur.getMaximum()).compareTo(maxDelay) <= 0) {
                    filtered.add(cur);
                }
                nextInd++;
            }
        }
        return TimeWindows.of(filtered, true);

    }


}
