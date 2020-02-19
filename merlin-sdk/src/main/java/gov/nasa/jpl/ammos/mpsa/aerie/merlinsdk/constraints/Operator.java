package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.List;

public class Operator {

    public static List<Window> intersection(List<Window> a, List<Window> b){
        return null;
    }

    public static List<Window> union(List<Window> a, List<Window> b){
        return null;
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
