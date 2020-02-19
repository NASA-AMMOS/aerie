package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.List;

public class And {

    public List<Window> intsection(List<Window> a, List<Window> b){
        return null;
    }


    public Constraint And(Constraint a, Constraint b){

        Constraint abAnd;

        abAnd = () -> {
            return intsection(a.getWindows(), b.getWindows());
        };

        return abAnd;
    }

}
