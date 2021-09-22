package gov.nasa.jpl.aerie.scheduler;

/**
 * Above state constraint
 * @param <T> the type of the state on which the constraint applies
 */
public class StateConstraintAbove<T extends Comparable<T>> extends StateConstraint<T> {

    protected StateConstraintAbove(){
        cache = new ValidityCache(){
            @Override
            public TimeWindows fetchValue(Plan plan, TimeWindows intervals) {
                return findWindowsPart(plan, intervals);
            }
        };
    }

    /**
     * Facade for state interface
     * @param plan IN current plan
     * @param windows IN set of time ranges in which search is performed
     * @return a set of time ranges in which the constraint is satisfied
     */
    public TimeWindows findWindowsPart( Plan plan, TimeWindows windows ) {
        TimeWindows wins = this.state.whenValueAbove(this.valueDefinition.get(0), windows);
        return wins;
    }


}
