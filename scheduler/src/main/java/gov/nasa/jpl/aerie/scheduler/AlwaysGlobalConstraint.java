package gov.nasa.jpl.aerie.scheduler;

public class AlwaysGlobalConstraint extends GlobalConstraint{

    private final StateConstraint<?> sc;

    public AlwaysGlobalConstraint(StateConstraint<?> sc){
        this.sc = sc;
        throw new IllegalArgumentException("Not implemented");

    }

    @Override
    public ConstraintState isEnforced(Plan plan, TimeWindows windows) {
        throw new IllegalArgumentException("Not implemented");
    }

}
