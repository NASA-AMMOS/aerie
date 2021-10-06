package gov.nasa.jpl.aerie.scheduler;

/**
 * Abstract class defining methods that must be implemented by global constraints such as mutex or cardinality
 * Also provides a directory for creating these constraints
 */
public abstract class GlobalConstraintWithIntrospection extends GlobalConstraint {

    //specific to introspectable constraint : find the windows in which we can insert activities without violating
    //the constraint
    public abstract TimeWindows findWindows(Plan plan, TimeWindows windows, Conflict conflict);


}
