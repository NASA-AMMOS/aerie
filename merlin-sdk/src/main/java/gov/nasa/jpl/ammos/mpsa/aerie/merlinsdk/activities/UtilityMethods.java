package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class UtilityMethods {

    public static List<Window> collapseOverlapping(List<Window> windows) {

        if (windows.size() <= 1) {
            return windows;
        }

        windows.sort(new Comparator<Window>() {
            @Override
            public int compare(Window o1, Window o2) {
                return o1.start.compareTo(o2.start);
            }
        });

        var result = new ArrayList<Window>();
        result.add(windows.get(0));

        for (int i = 1; i < windows.size(); i++) {

            var cur = windows.get(i);
            var collapsed = result.get(result.size() - 1);

            if (collapsed.overlaps(cur)) {
                result.add(Window.leastUpperBound(collapsed, cur));
                result.remove(collapsed);
            } else {
                result.add(Window.between(cur.start, cur.end));
            }
        }

        return result;
    }
}
