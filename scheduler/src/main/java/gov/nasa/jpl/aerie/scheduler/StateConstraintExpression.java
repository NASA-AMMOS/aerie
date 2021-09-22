package gov.nasa.jpl.aerie.scheduler;
import java.util.*;


/**
 * Class describing a constraint on a generic state and providing methods to create all state constraints
 */
public class StateConstraintExpression {

    enum BuilderType{
        OR,
        AND
    }


    /**
     * finds the windows over which the constraint is satisfied
     *
     * the output windows are always a subset of the provided input
     * windows, which allows for search optimization during iterative
     * window narrowing
     *
     * @param plan IN the plan context to evaluate the constraint in
     * @param windows IN the narrowed time ranges in the plan in which
     *        to search for constraint satisfiaction
     *
     * @return time windows in the plan over which the constraint is
     *         satisfied and overlap with the given windows
     */
    public  TimeWindows findWindows( Plan plan, TimeWindows windows ){
        TimeWindows tw = sc.findWindows(plan, windows);
        return new TimeWindows(tw);
    }



    /**
     * Constraint builder
     * Default behavior is considering that chaining statements is building conjunctions and that OR should be explicitly state
     */
    public static class Builder  {


        BuilderType builderType = null;

        //Conjonction is locally modeled as a list of constraint before being transformed in proper ConstraintConjunction
        protected List<StateConstraintExpression> constraints = new LinkedList<StateConstraintExpression>();

        boolean forAll = false;

        public StateConstraintExpression.Builder forAll(){
           forAll =true;
            return getThis();
        }

        public StateConstraintExpression.Builder orBuilder(){
            if(builderType != null){
                throw new IllegalArgumentException("Only one type of builder per expression");
            }
            builderType = BuilderType.OR;
            return getThis();
        }


        public StateConstraintExpression.Builder andBuilder(){
            if(builderType != null){
                throw new IllegalArgumentException("Only one type of builder per expression");
            }
            builderType = BuilderType.AND;
            return getThis();
        }

        public StateConstraintExpression.Builder satisfied(StateConstraintExpression ste){
            this.constraints.add(ste);
            return getThis();
        }


        public  <T extends Comparable<T>> StateConstraintExpression.Builder lessThan(ExternalState<T> state, T value) {
            StateConstraintBelow<T> bca = StateConstraintExpression.lessThan(state, value);
            this.constraints.add(new StateConstraintExpression(bca));
            return getThis();
        }

        public  <T extends Comparable<T>> StateConstraintExpression.Builder between(ExternalState<T> state, T value1, T value2 ) {
            StateConstraintBetween<T> bca = StateConstraintExpression.buildBetweenConstraint(state, value1, value2);
            this.constraints.add(new StateConstraintExpression(bca));
            return getThis();
        }
        public  <T extends Comparable<T>> StateConstraintExpression.Builder above(ExternalState<T> state, T value   ) {
            StateConstraintAbove<T> sca = StateConstraintExpression.buildAboveConstraint(state, value);
            this.constraints.add(new StateConstraintExpression(sca));
            return getThis();
        }

        public  <T extends Comparable<T>> StateConstraintExpression.Builder equal(ExternalState<T> state, T value   ) {
            StateConstraintEqual<T> sce = StateConstraintExpression.buildEqualConstraint(state, value);
            this.constraints.add(new StateConstraintExpression(sce));
            return getThis();
        }

        public  <T extends Comparable<T>> StateConstraintExpression.Builder equal(List<? extends ExternalState<T>> states, T value) {
            if(forAll==false){
                throw new IllegalArgumentException("forAll() should have been used before");
            } else{
                for(ExternalState<T> state : states){
                    StateConstraintEqual<T> sce = StateConstraintExpression.buildEqualConstraint(state, value);
                    this.constraints.add(new StateConstraintExpression(sce));
                }
            }
            return getThis();
        }


        public  <T extends Comparable<T>> StateConstraintExpression.Builder notEqual(List<? extends ExternalState<T>> states, T value) {
            if(forAll==false){
                throw new IllegalArgumentException("forAll() should have been used before");
            } else{
                for(ExternalState<T> state : states){
                    StateConstraintNotEqual<T> sce = StateConstraintExpression.buildNotEqualConstraint(state, value);
                    this.constraints.add(new StateConstraintExpression(sce));
                }
            }
            return getThis();
        }





        public  <T extends Comparable<T>> StateConstraintExpression.Builder notEqual(ExternalState<T> state, T value   ) {
            StateConstraintNotEqual<T> sce = StateConstraintExpression.buildNotEqualConstraint(state, value);
            this.constraints.add(new StateConstraintExpression(sce));
            return getThis();
        }

        public StateConstraintExpression build(){
            StateConstraintExpression constr = null;

            if(builderType == null){
                if(constraints.size()==1){
                    builderType = BuilderType.OR;
                }else {
                    throw new IllegalArgumentException("Builder type has to be defined : or / and");
                }
            }

            if(builderType== BuilderType.AND){
                constr = new StateConstraintExpressionConjunction(constraints);
            } else {
                constr = new StateConstraintExpressionDisjunction(constraints);
            }


            return constr;
        }





        /**
         * returns the current builder object (but typed at the lowest level)
         *
         * should be implemented by the builder at the bottom of the type heirarchy
         *
         * @return reference to the current builder object (specifically typed)
         */
        protected StateConstraintExpression.Builder getThis(){
            return this;
        };



    }




    /**
     * Builds a below constraint
     * @param state the state on which the constraint applies
     * @param value the value below which the state should be to satisfy the constraint
     * @param <T> the state type
     * @return a below state constraint
     */
    protected static <T extends Comparable<T>> StateConstraintBelow<T> lessThan(ExternalState<T> state, T value){
        return lessThan(state, value, null);
    }

    /**
     * Builds an above constraint
     * @param state the state on which the constraint applies
     * @param value the value above which the state should be to satisfy the constraint
     * @param <T> the state type
     * @param timeDomain x
     * @return a above state constraint
     */
    protected static <T extends Comparable<T>> StateConstraintBelow<T> lessThan(ExternalState<T> state, T value, TimeWindows timeDomain){
        StateConstraintBelow<T> sc = new StateConstraintBelow<T>();
        sc.setDomainUnary(value);
        sc.setState(state);
        sc.setTimeDomain(timeDomain);
        return sc;
    }



    /**
     * Builds an above constraint
     * @param state the state on which the constraint applies
     * @param value the value above which the state should be to satisfy the constraint
     * @param <T> the state type
     * @return a above state constraint
     */
    protected static <T extends Comparable<T>> StateConstraintAbove<T> buildAboveConstraint(ExternalState<T> state, T value){
        return buildAboveConstraint(state, value, null);
    }


    /**
     * Builds an above constraint
     * @param state the state on which the constraint applies
     * @param value the value above which the state should be to satisfy the constraint
     * @param timeDomain x
     * @param <T> the state type
     * @return a above state constraint
     */
    protected static <T extends Comparable<T>> StateConstraintAbove<T> buildAboveConstraint(ExternalState<T> state, T value, TimeWindows timeDomain){
        StateConstraintAbove<T> sc = new StateConstraintAbove<T>();
        sc.setDomainUnary(value);
        sc.setState(state);
        sc.setTimeDomain(timeDomain);
        return sc;
    }


    /**
     * Builds an above constraint
     * @param state the state on which the constraint applies
     * @param value1 the lower bounds of the interval in which the state should be to satisfy the constraint
     * @param value2 the upper bounds of the interval in which the state should be to satisfy the constraint
     * @param <T> the state type
     * @return a between state constraint
     */
    protected static <T extends Comparable<T>> StateConstraintBetween<T> buildBetweenConstraint(ExternalState<T> state, T value1, T value2){
        return buildBetweenConstraint( state, value1, value2, null);
    }

    /**
     * Builds an above constraint
     * @param state the state on which the constraint applies
     * @param value1 the lower bounds of the interval in which the state should be to satisfy the constraint
     * @param value2 the upper bounds of the interval in which the state should be to satisfy the constraint
     * @param timeDomain x
     * @param <T> the state type
     * @return a between state constraint
     */
    protected static <T extends Comparable<T>> StateConstraintBetween<T> buildBetweenConstraint(ExternalState<T> state, T value1, T value2, TimeWindows timeDomain){
        StateConstraintBetween<T> sc = new StateConstraintBetween<T>();
        sc.setValueDefinition(Arrays.asList(value1, value2));
        sc.setState(state);
        sc.setTimeDomain(timeDomain);
        return sc;
    }

    /**
     * Builds an equal constraint
     * @param state the state on which the constraint applies
     * @param value the value to which the state should be equal to satisfy the constraint
     * @param <T> the state type
     * @return an equal state constraint
     */
    protected static <T extends Comparable<T>> StateConstraintEqual<T> buildEqualConstraint(ExternalState<T> state, T value){
        return buildEqualConstraint( state, value, null);
    }

    /**
     * Builds an equal constraint
     * @param state the state on which the constraint applies
     * @param value the value to which the state should be equal to satisfy the constraint
     * @param timeDomain x
     * @param <T> the state type
     * @return an equal state constraint
     */
    protected static <T extends Comparable<T>> StateConstraintEqual<T> buildEqualConstraint(ExternalState<T> state, T value, TimeWindows timeDomain){
        StateConstraintEqual<T> sc = new StateConstraintEqual<T>();
        sc.setDomainUnary(value);
        sc.setState(state);
        sc.setTimeDomain(timeDomain);
        return sc;
    }

    /**
     * Builds an equal constraint
     * @param state the state on which the constraint applies
     * @param value the value to which the state should be equal to satisfy the constraint
     * @param <T> the state type
     * @return an equal state constraint
     */
    protected static <T extends Comparable<T>> StateConstraintNotEqual<T> buildNotEqualConstraint(ExternalState<T> state, T value){
        StateConstraintNotEqual<T> sc = new StateConstraintNotEqual<T>();
        sc.setDomainUnary(value);
        sc.setState(state);
        sc.setTimeDomain(null);
        return sc;
    }


    protected  <T extends Comparable<T>> StateConstraintExpression(StateConstraint<T> sc){
        this.sc = sc;
    }

    @SuppressWarnings("rawtypes")
    StateConstraint sc;


}
