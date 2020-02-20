package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.ArrayList;
import java.util.Collections;
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
        if (w1.start.isBefore(w2.start)){
            return -1;
        }
        else if (w1.start.isAfter(w2.start)){
            return 1;
        }
        return 0;
    }

    /*
    Assumption: in both methods intersection and union,
    a and b are composed of in-order, disjoint intervals
    */

    public static List<Window> intersection(List<Window> a, List<Window> b){

        List<Window> intersection = new ArrayList<>();
        int i = 0, j = 0;

        while (i < a.size() && j < b.size()){

            Instant aStart = a.get(i).start;
            Instant bStart = b.get(j).start;
            Instant aEnd = a.get(i).end;
            Instant bEnd = b.get(j).end;

            //get the last start and the first end for the two windows
            Instant maxStart = (aStart.isAfter(bStart) ? aStart : bStart);
            Instant minEnd = (aEnd.isBefore(bEnd) ? aEnd : bEnd);

            //see if there is an overlap
            //also should isBefore check == in addition to <?
            if (maxStart.isBefore(minEnd)){
                intersection.add(Window.between(maxStart, minEnd));
            }

            if (aEnd.isBefore(bEnd)){
                i++;
            }
            else {
                j++;
            }
        }

        return intersection;
    }

    public static List<Window> union(List<Window> a, List<Window> b){

        List<Window> temp = new ArrayList<>();

        temp.addAll(a);
        temp.addAll(b);

        List<Window> union = new ArrayList<>();

        Collections.sort(temp, new Operator());

        for (Window window : temp){

            //empty list, add first window
            if (union.isEmpty()){
                union.add(window);
                continue;
            }

            Window lastAddedWindow = union.get(union.size()-1);
            //no overlap b/w last element in list and next element in temp list
            if (lastAddedWindow.end.isBefore(window.start)){
                union.add(window);
            }

            //there is an overlap
            else {
               Instant end = (
                        lastAddedWindow.end.isAfter(window.end) ?
                                lastAddedWindow.end : window.end);

               Window merged = Window.between(lastAddedWindow.start, end);

               union.remove(union.size()-1);
               union.add(merged);
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
