package gov.nasa.jpl.aerie.scheduler.constraints;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.Range;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.filters.Filters;
import gov.nasa.jpl.aerie.scheduler.constraints.filters.TimeWindowsFilter;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.ExternalState;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.StateConstraintExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.transformers.TimeWindowsTransformer;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.Time;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

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
  public Windows computeRange(Plan plan, Windows domain) {

    Windows inter = new Windows(domain);
    //particularly important for combining with constant states as they are by definition adjacent values
    //TODO:remove
    //inter.doNotMergeAdjacent();

    if (constantWin != null) {
      inter.intersectWith(constantWin);
    }

    if (actTemplate != null) {
      Windows actTw = new Windows();
      //particularly important for combining with constant states as they are by definition adjacent values
      //TODO:marker to remove
      //actTw.doNotMergeAdjacent();
      var minTimepoint = domain.minTimePoint();
      var maxTimepoint = domain.maxTimePoint();
      if(minTimepoint.isPresent()&& maxTimepoint.isPresent()) {
        final var anchorActSearch = new ActivityExpression.Builder()
            .basedOn(actTemplate)
            .startsIn(Window.between(
                Duration.max(Duration.ZERO, minTimepoint.get()),
                Window.Inclusivity.Inclusive,
                maxTimepoint.get(),
                Window.Inclusivity.Exclusive)).build();
        final var anchorActs = plan.find(anchorActSearch);
        for (var anchorAct : anchorActs) {
          actTw.add(Window.between(anchorAct.getStartTime(), Window.Inclusivity.Inclusive, anchorAct.getEndTime(), Window.Inclusivity.Exclusive));
        }
        inter = actTw;
      }
    }

    for (var otherExpr : timeRangeExpressions) {
      Windows windowsState = otherExpr.computeRange(plan, domain);
      inter.intersectWith(windowsState);
      if (inter.isEmpty()) {
        break;
      }
    }

    for (var expr : stateExpr) {
      Windows windowsState = expr.findWindows(plan, domain);
      inter.intersectWith(windowsState);
      if (inter.isEmpty()) {
        break;
      }
    }

    for (var constState : constantsStates) {
      Windows windowsState = new Windows(toOO(constState.getTimeline(domain).keySet().stream().toList()));
      inter.intersectWith(windowsState);
      if (inter.isEmpty()) {
        break;
      }
    }
    for (var filterOrtransform : filtersAndTransformers) {
      if (filterOrtransform instanceof TimeWindowsFilter) {
        inter = ((TimeWindowsFilter) filterOrtransform).filter(plan, inter);
      } else if (filterOrtransform instanceof TimeWindowsTransformer) {
        inter = ((TimeWindowsTransformer) filterOrtransform).transformWindows(plan, inter);
      }
      if (inter.isEmpty()) {
        break;
      }
    }

    return inter;

  }

  //To avoid squashing of adjacent windows
  public List<Window> toOO(List<Window> windows){
    var list = new ArrayList<Window>();
    for(var win: windows){
      list.add(Window.between(win.start, Window.Inclusivity.Exclusive, win.end, Window.Inclusivity.Exclusive));
    }
    return list;
  }

  public void setName(String name) {
    this.name = name;
  }

  protected Windows constantWin;
  protected String name = "TRE_" + Math.abs(new Random().nextInt());
  protected List<TimeRangeExpression> timeRangeExpressions;
  protected List<Object> filtersAndTransformers;
  protected List<StateConstraintExpression> stateExpr;
  protected List<ExternalState> constantsStates;
  private ActivityExpression actTemplate;

  //TODO:unused now, not sure it is useful
  protected Range<Time> horizon;

  public static TimeRangeExpression constantValuesOf(ExternalState sce) {
    return new Builder().ofEachValue(sce).build();
  }

  public static TimeRangeExpression of(StateConstraintExpression sce) {
    return new Builder().from(sce).build();
  }

  public static TimeRangeExpression of(Windows wins) {
    return new Builder().from(wins).build();
  }

  public static class Builder {
    final List<Object> filtersAndTransformers = new ArrayList<>();
    final List<StateConstraintExpression> stateExpr = new ArrayList<>();
    final List<ExternalState> constantsStates = new ArrayList<>();
    final List<TimeRangeExpression> timeRangeExpressions = new ArrayList<>();

    final List<Windows> constantWin = new ArrayList<>();

    Range<Time> horizon = null;
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


    public Builder onHorizon(Range<Time> horizon) {
      this.horizon = horizon;
      return getThis();
    }

    /**
     * state constraint use case
     *
     * @param expr x
     * @return x
     */
    public Builder from(StateConstraintExpression expr) {
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
     * @param state x
     * @param <T> x
     * @return x
     */
    public <T> Builder ofEachValue(ExternalState state) {
      this.constantsStates.add(state);
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
      tre.horizon = horizon;
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
