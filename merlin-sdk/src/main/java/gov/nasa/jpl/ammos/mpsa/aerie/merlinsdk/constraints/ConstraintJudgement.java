package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.List;

public class ConstraintJudgement {

    public static String activityDurationRequirement(Constraint dataActivityOccuring, Constraint stateCondition){

        List<Window> whenActivityOccurs = dataActivityOccuring.getWindows();
        List<Window> whenConditionsMet = stateCondition.getWindows();


        List<Window> intersect = Operator.intersection(whenActivityOccurs, whenConditionsMet);

        //TODO: make sure there are no windows that could be collapsed e.g. [1,2][2,3]
        if (intersect.size() != whenActivityOccurs.size()){
            return "Constraint Violatation";
        }

        int i = 0;

        while (i < intersect.size()){
            if (!intersect.get(i).equals(whenActivityOccurs.get(i))){
                return "Constraint Vioaltion";
            }
            i++;
        }
        return "No Violation";
    }

}
