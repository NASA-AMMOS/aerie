package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/** A time range expression combines timewindows from states, activity expressions, state expressions, and other time
 * range expressions and allows to apply a sequence of filters and transforms to be used in goals
 *
 */
public class TimeRangeExpression {


    /**
     * TODO: for now, acts like a big AND. We need a OR.
     * @param plan x
     * @param domain x
     * @return x
     */
    public TimeWindows computeRange(Plan plan, TimeWindows domain) {

        TimeWindows inter = new TimeWindows(domain);
        //particularly important for combining with constant states as they are by definition adjacent values
        inter.doNotMergeAdjacent();

        if(constantWin!=null){
            inter.intersection(constantWin);
        }

        if(actTemplate != null) {
            TimeWindows actTw = new TimeWindows();
            //particularly important for combining with constant states as they are by definition adjacent values
            actTw.doNotMergeAdjacent();
            final var anchorActSearch = new ActivityExpression.Builder()
                    .basedOn(actTemplate)
                    .startsIn(new Range<Time>(domain.getMinimum(), domain.getMaximum())).build();
            final var anchorActs = plan.find(anchorActSearch);
            for (var anchorAct : anchorActs) {
                actTw.union(new Range<Time>(anchorAct.getStartTime(), anchorAct.getEndTime()));
            }
            inter = actTw;
        }

        for(var otherExpr : timeRangeExpressions){
            TimeWindows windowsState = otherExpr.computeRange(plan, domain);
            inter.intersection(windowsState);
            if (inter.isEmpty()) {
                break;
            }
        }

        for (var expr : stateExpr) {
            TimeWindows windowsState =  expr.findWindows(plan, domain);
            inter.intersection(windowsState);
            if (inter.isEmpty()) {
                break;
            }
        }

        for (var constState : constantsStates) {
            TimeWindows windowsState = TimeWindows.of(constState.getTimeline(domain).keySet(), true);
            inter.intersection(windowsState);
            if (inter.isEmpty()) {
                break;
            }
        }
        for (var filterOrtransform : filtersAndTransformers) {
            if(filterOrtransform instanceof TimeWindowsFilter){
                inter = ((TimeWindowsFilter)filterOrtransform).filter(plan, inter);
            } else if(filterOrtransform instanceof TimeWindowsTransformer){
                inter = ((TimeWindowsTransformer)filterOrtransform).transformWindows(plan, inter);
            }
            if(inter.isEmpty()){
                break;
            }
        }
        //out.println(name + " -> " + inter);

        return inter;

    }

    public void setName(String name){
        this.name =name;
    }

    protected TimeWindows constantWin;
    protected String name = "TRE_" + Math.abs(new Random().nextInt());
    protected List<TimeRangeExpression> timeRangeExpressions;
    protected List<Object> filtersAndTransformers;
    protected List<StateConstraintExpression> stateExpr;
    protected List<ExternalState<?>> constantsStates;
    private ActivityExpression actTemplate;

    //TODO:unused now, not sure it is useful
    protected Range<Time> horizon;

    public static TimeRangeExpression constantValuesOf(ExternalState<?> sce){
        TimeRangeExpression tre = new Builder().ofEachValue(sce).build();
        return tre;
    }

    public static TimeRangeExpression of(StateConstraintExpression sce) {
        TimeRangeExpression tre = new Builder().from(sce).build();
        return tre;
    }

    public static class Builder  {
        List<Object> filtersAndTransformers = new ArrayList<Object>();
        List<StateConstraintExpression> stateExpr = new ArrayList<StateConstraintExpression>();
        List<ExternalState<?>> constantsStates = new ArrayList<ExternalState<?>>();
        List<TimeRangeExpression> timeRangeExpressions = new ArrayList<TimeRangeExpression>();;
        List<TimeWindows> constantWin = new ArrayList<TimeWindows>();

        Range<Time> horizon = null;
        private ActivityExpression actTemplate;


        public Builder thenFilter(TimeWindowsFilter filter){
            filtersAndTransformers.add(filter);
            return getThis();
        }

        public Builder thenFilter(Function<Range<Time>, Boolean> functionalFilter){
            filtersAndTransformers.add(Filters.functionalFilter(functionalFilter));
            return getThis();
        }

        public Builder thenTransform(TimeWindowsTransformer transformer){
            filtersAndTransformers.add(transformer);
            return getThis();
        }


        public Builder onHorizon(Range<Time> horizon){
            this.horizon = horizon;
            return getThis();
        }

        /**
         * state constraint use case
         * @param expr x
         * @return x
         */
        public Builder from(StateConstraintExpression expr){
            this.stateExpr.add(expr);
            return getThis();
        }

        public Builder from(TimeWindows constantWin){
            this.constantWin.add(constantWin);
            return getThis();
        }

        public Builder from(TimeRangeExpression expr){
            this.timeRangeExpressions.add(expr);
            return getThis();
        }

        public Builder from(ActivityExpression actTemplate){
            this.actTemplate = actTemplate;
            return getThis();
        }

        String name;
        public Builder name(String name){
            this.name = name;
            return this;
        }

        /**
         * relative instant use case
         * @param state x
         * @param <T> x
         * @return x
         */
        public <T> Builder ofEachValue(ExternalState<T> state){
            this.constantsStates.add(state);
            return getThis();

        }


        public TimeRangeExpression build(){
            TimeRangeExpression tre = new TimeRangeExpression();

            if(constantsStates.isEmpty() && stateExpr.isEmpty() && actTemplate == null && timeRangeExpressions.isEmpty() && constantWin.isEmpty()){
                throw new RuntimeException("either from or constantValuesOf has to be used to build a valid expression");
            }
            tre.filtersAndTransformers = filtersAndTransformers;
            tre.constantsStates = constantsStates;
            tre.stateExpr = stateExpr;
            tre.horizon = horizon;
            tre.timeRangeExpressions = timeRangeExpressions;
            tre.actTemplate = actTemplate;

            if(constantWin.size()>0) {
                TimeWindows cstWind = constantWin.get(0);
                for (var cstWin : constantWin) {
                    cstWind.intersection(cstWin);
                }
                tre.constantWin = cstWind;
            }

            if(name != null) {
                tre.name = name;
            }
            return tre;
        }

        public Builder getThis(){
            return this;
        }

    }


    private TimeRangeExpression(){

    }

}
