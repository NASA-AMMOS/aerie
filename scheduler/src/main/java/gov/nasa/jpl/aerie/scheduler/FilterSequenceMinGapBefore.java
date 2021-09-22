package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Filter in windows that have another window preceding separated at least by delay
 */

// delay = 4
// passW:  ......[======]...[=======]............[=====].......
// output: ......[======]........................[=====].......
public class FilterSequenceMinGapBefore implements TimeWindowsFilter {

    private Duration delay;

    public FilterSequenceMinGapBefore(Duration delay){
        this.delay = delay;
    }

    @Override
    public TimeWindows filter(Plan plan,TimeWindows windows) {
        Range<Time> before = null;
        Collection<Range<Time>> filtered = new ArrayList<Range<Time>>();
        List<Range<Time>> windowsToFilter = windows.getRangeSet();
        if(windowsToFilter.size()>0) {
            filtered.add(windowsToFilter.get(0));
            for (Range<Time> range : windowsToFilter) {
                if (before != null) {
                    if (range.getMinimum().minus(before.getMaximum()).compareTo(delay) >= 0) {
                        filtered.add(range);
                    }
                }
                before = range;
            }
        }
        return TimeWindows.of(filtered, true);
    }


}
