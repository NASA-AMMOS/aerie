package gov.nasa.jpl.mpsa.old;

import gov.nasa.jpl.mpsa.conditions.Condition;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;

public interface ActivityDefinition {

    List<Condition> conditions = new ArrayList();

    default void decompose(){
        System.err.println("Nothing to decompose");
    }

    default void effects() throws InterruptedException {
        System.err.println("No model defined");
    }

    default void sequence() {
        System.err.println("No sequence defined");
    }

    default void addCondition(Condition condition){
        this.conditions.add(condition);
    }

    default void setParent(Activity act) { }
}
