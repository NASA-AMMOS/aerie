package gov.nasa.jpl.aerie.scheduler;

import java.util.List;

/**
 * Abstract class defining methods that must be implemented by global constraints such as mutex or cardinality
 * Also provides a directory for creating these constraints
 */
public abstract class GlobalConstraint  {

    public abstract ConstraintState isEnforced(Plan plan, TimeWindows windows);

    public abstract TimeWindows findWindows(Plan plan, TimeWindows windows, Conflict conflict);


    public static NAryMutexConstraint atMostOneOf(List<ActivityExpression> types){
        return NAryMutexConstraint.buildMutexConstraint(types);
    }

}
