package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Operator implements Comparator<Window> {

    /*
    w1 < w2: -1
    w1 == w2: 0
    w1 > w2: 1
     */
    @Override
    public int compare(Window w1, Window w2) {
        Instant w1Start = w1.start;
        Instant w2Start = w2.start;

        if (w1Start.isBefore(w1Start)){
            return -1;
        }
        else if (w1Start.isAfter(w2Start)){
            return 1;
        }
        return 0;
    }

    //assumes list of windows are in time order
    public static List<Window> intersection(List<Window> a, List<Window> b){
        return null;
    }

    //assumes list of windows are in time order
    public static List<Window> union(List<Window> a, List<Window> b){

        List<Window> temp = new ArrayList<>();
        List<Window> union = new ArrayList<>();
        temp.addAll(a);
        temp.addAll(b);

        for (Window window : temp){

            //empty list, add first window
            if (union.isEmpty()){
                union.add(window);
            }

            Window lastAddedWindow = union.get(union.size()-1);
            //no overlap b/w last element in list and next element in temp list
            if (lastAddedWindow.end.isBefore(window.start)){
                union.add(window);
            }

            //there is an overlap
            else {
                lastAddedWindow.end = (
                        lastAddedWindow.end.isBefore(window.end) ?
                                lastAddedWindow.end : window.end);
            }
        }

        return union;
    }


    public static Constraint And(Constraint a, Constraint b){
        Constraint abAnd;

        abAnd = () -> {
            return intersection(a.getWindows(), b.getWindows());
        };

        return abAnd;
    }

    public static Constraint Or(Constraint a, Constraint b){
        Constraint abOr;

        abOr = () -> {
            return union(a.getWindows(), b.getWindows());
        };

        return abOr;
    }
}
