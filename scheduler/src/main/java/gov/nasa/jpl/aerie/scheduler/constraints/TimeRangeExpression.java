package gov.nasa.jpl.aerie.scheduler.constraints;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.filters.Filters;
import gov.nasa.jpl.aerie.scheduler.constraints.filters.TimeWindowsFilter;
import gov.nasa.jpl.aerie.scheduler.constraints.transformers.TimeWindowsTransformer;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * A time range expression combines timewindows from states, activity expressions, state expressions, and other time
 * range expressions and allows to apply a sequence of filters and transforms to be used in goals
 */
public class TimeRangeExpression {


  /**
   * TODO: for now, acts like a big AND. We need a OR.
   *
   * @param plan x
   * @param domain x
   * @return x
   */
  public Windows computeRange(final SimulationResults simulationResults, final Plan plan, final Windows domain) {

    Windows inter = new Windows(domain);

    if (constantWin != null && !inter.isEmpty()) {
      inter.intersectWith(constantWin);
    }

    if (actTemplate != null && !inter.isEmpty()) {
      Windows actTw = new Windows();
      var minTimepoint = inter.minTimePoint();
      var maxTimepoint = inter.maxTimePoint();
      if(minTimepoint.isPresent() && maxTimepoint.isPresent()) {
        final var anchorActSearch = new ActivityExpression.Builder()
            .basedOn(actTemplate)
            .startsIn(Window.between(
                Duration.max(Duration.ZERO, minTimepoint.get()),
                Window.Inclusivity.Inclusive,
                maxTimepoint.get(),
                Window.Inclusivity.Exclusive)).build();
        final var anchorActs = plan.find(anchorActSearch, simulationResults);
        for (var anchorAct : anchorActs) {
          var endInclusivity = Window.Inclusivity.Exclusive;
          if(anchorAct.getDuration().isZero()){
            endInclusivity = Window.Inclusivity.Inclusive;
          }
          actTw.add(Window.between(anchorAct.getStartTime(), Window.Inclusivity.Inclusive, anchorAct.getEndTime(), endInclusivity));
        }
      }
      inter.intersectWith(actTw);
    }

    for (var otherExpr : timeRangeExpressions) {
      if(inter.isEmpty()) break;
      Windows windowsState = otherExpr.computeRange(simulationResults, plan, domain);
      inter.intersectWith(windowsState);
    }

    for (var expr : stateExpr) {
      if(inter.isEmpty()) break;
      final var domainOfInter = Window.between(inter.minTimePoint().get(), inter.maxTimePoint().get());
      Windows windowsState = expr.evaluate(simulationResults, domainOfInter, null);
      inter.intersectWith(windowsState);
    }

    for (var constState : constantsStates) {
      if(inter.isEmpty()) break;
      final var domainOfInter = Window.between(inter.minTimePoint().get(), inter.maxTimePoint().get());
      final var changePoints = simulationResults.discreteProfiles.get(constState).changePoints(domainOfInter);
      final var timeline = new Windows();
      final var container = new ArrayList<Window>();
      changePoints.iterator().forEachRemaining(container::add);
      container.stream().reduce((a, b) -> {
        timeline.add(Window.window(a.start, Window.Inclusivity.Exclusive, b.start, Window.Inclusivity.Exclusive));
        return b;
      });
      inter.intersectWith(timeline);
    }

    for (var filterOrtransform : filtersAndTransformers) {
      if(inter.isEmpty()) break;
      if (filterOrtransform instanceof TimeWindowsFilter) {
        inter = ((TimeWindowsFilter) filterOrtransform).filter(simulationResults, plan, inter);
      } else if (filterOrtransform instanceof TimeWindowsTransformer) {
        inter = ((TimeWindowsTransformer) filterOrtransform).transformWindows(plan, inter, simulationResults);
      }
    }

    return inter;

  }

  public void setName(String name) {
    this.name = name;
  }

  protected Windows constantWin;
  protected String name = "TRE_" + Math.abs(new Random().nextInt());
  protected List<TimeRangeExpression> timeRangeExpressions;
  protected List<Object> filtersAndTransformers;
  protected List<Expression<Windows>> stateExpr;
  protected List<String> constantsStates;
  private ActivityExpression actTemplate;

  public static TimeRangeExpression constantValuesOf(String nameDiscreteProfile) {
    return new Builder().ofEachValue(nameDiscreteProfile).build();
  }

  public static TimeRangeExpression of(Expression<Windows> sce) {
    return new Builder().from(sce).build();
  }

  public static TimeRangeExpression of(Windows wins) {
    return new Builder().from(wins).build();
  }

  public static class Builder {
    final List<Object> filtersAndTransformers = new ArrayList<>();
    final List<Expression<Windows>> stateExpr = new ArrayList<>();
    final List<String> constantsStates = new ArrayList<>();
    final List<TimeRangeExpression> timeRangeExpressions = new ArrayList<>();

    final List<Windows> constantWin = new ArrayList<>();

    private ActivityExpression actTemplate;


    public Builder thenFilter(TimeWindowsFilter filter) {
      filtersAndTransformers.add(filter);
      return getThis();
    }

    public Builder thenFilter(Function<Window, Boolean> functionalFilter) {
      filtersAndTransformers.add(Filters.functionalFilter(functionalFilter));
      return getThis();
    }

    public Builder thenTransform(TimeWindowsTransformer transformer) {
      filtersAndTransformers.add(transformer);
      return getThis();
    }

    /**
     * state constraint use case
     *
     * @param expr x
     * @return x
     */
    public Builder from(Expression<Windows> expr) {
      this.stateExpr.add(expr);
      return getThis();
    }

    public Builder from(Windows constantWin) {
      this.constantWin.add(constantWin);
      return getThis();
    }

    public Builder from(TimeRangeExpression expr) {
      this.timeRangeExpressions.add(expr);
      return getThis();
    }

    public Builder from(ActivityExpression actTemplate) {
      this.actTemplate = actTemplate;
      return getThis();
    }

    String name;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * relative instant use case
     *
     * @param nameDiscreteProfile name of the discrete profile
     * @param <T> x
     * @return x
     */
    public <T> Builder ofEachValue(String nameDiscreteProfile) {
      this.constantsStates.add(nameDiscreteProfile);
      return getThis();
    }


    public TimeRangeExpression build() {
      TimeRangeExpression tre = new TimeRangeExpression();

      if (constantsStates.isEmpty()
          && stateExpr.isEmpty()
          && actTemplate == null
          && timeRangeExpressions.isEmpty()
          && constantWin.isEmpty()) {
        throw new RuntimeException("either from or constantValuesOf has to be used to build a valid expression");
      }
      tre.filtersAndTransformers = filtersAndTransformers;
      tre.constantsStates = constantsStates;
      tre.stateExpr = stateExpr;
      tre.timeRangeExpressions = timeRangeExpressions;
      tre.actTemplate = actTemplate;

      if (constantWin.size() > 0) {
        Windows cstWind = constantWin.get(0);
        for (var cstWin : constantWin) {
          cstWind.intersectWith(cstWin);
        }
        tre.constantWin = cstWind;
      }

      if (name != null) {
        tre.name = name;
      }
      return tre;
    }

    public Builder getThis() {
      return this;
    }

  }


  private TimeRangeExpression() {

  }

}
