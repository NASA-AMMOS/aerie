package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * Filters elements in a sequence of windows
 * Several flavors :
 * - keeps first element
 * - keep last element
 * - keep an element at a given position (-1 is the last element, -2 is the element before the last etc)
 */
public class FilterElementSequence implements TimeWindowsFilter {


    private int elementIndex;

    private FilterElementSequence(int numberInSequence){
        elementIndex = numberInSequence;
    }

    public static FilterElementSequence first(){
        return new FilterElementSequence(0);
    }

    public static FilterElementSequence last(){
        return new FilterElementSequence(-1);
    }
    public static FilterElementSequence numbered(int i){
        return new FilterElementSequence(i);
    }

    @Override
    public TimeWindows filter(Plan plan, TimeWindows windows) {
        List<Range<Time>> ret = new ArrayList<Range<Time>>();

        if(!windows.isEmpty()) {

            List<Range<Time>> ranges = windows.getRangeSet();
            if (this.elementIndex >= 0 && this.elementIndex < ranges.size()) {
                ret.add(ranges.get(this.elementIndex));
            } else if (elementIndex < 0 && Math.abs(this.elementIndex) <= ranges.size()) {
                ret.add(ranges.get(ranges.size()  +elementIndex ));
            }
        }

        return TimeWindows.of(ret, true);
    }
}
