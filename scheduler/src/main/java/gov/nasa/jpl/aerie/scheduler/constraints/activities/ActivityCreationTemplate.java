package gov.nasa.jpl.aerie.scheduler.constraints.activities;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.EquationSolvingAlgorithms;
import gov.nasa.jpl.aerie.scheduler.NotNull;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.StateQueryParam;
import gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions.DurationExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions.DurationExpressionState;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeExpression;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.stn.TaskNetwork;
import gov.nasa.jpl.aerie.scheduler.solver.stn.TaskNetworkAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * criteria used to identify create activity instances in scheduling goals
 *
 * the creation template is a partial specification of what is required of
 * an activity instance in order to meet the goal criteria, but also includes
 * additional information on how to create such matching instances in the
 * event that no matches were found
 *
 * for example "an image of at least 10s (or 30s by default) duration taken
 * with green filter"
 *
 * corresponds roughly to the concept of an "activity preset"
 *
 * creation templates may be fluently constructed via builders that parse like
 * first order logic predicate clauses used in building up scheduling rules
 */
public class ActivityCreationTemplate extends ActivityExpression {

  private static final Logger logger = LoggerFactory.getLogger(ActivityCreationTemplate.class);


  /**
   * ctor is private to prevent inconsistent construction
   *
   * please use the enclosed fluent Builder class instead
   *
   * leaves all criteria fields unspecified
   */
  protected ActivityCreationTemplate() { }

  @Override
  @SuppressWarnings("unchecked")
  public <B extends AbstractBuilder<B, AT>, AT extends ActivityExpression> AbstractBuilder<B, AT> getNewBuilder() {
    return (AbstractBuilder<B, AT>) new Builder();
  }

  public static ActivityCreationTemplate ofType(ActivityType actType) {
    var act = new ActivityCreationTemplate();
    act.type = actType;
    return act;
  }


  /**
   * fluent builder class for constructing creation templates
   *
   * each different term added to the builder via method calls become part of
   * a logical conjection, ie matching activities must meet all of the
   * specified criteria
   *
   * existing terms can be replaced by calling the same method again, ie
   * matching activities must only meet the last-specified term
   *
   * if the scheduling algorithm needs to create a new activity instance, it
   * will use either the last-specified default value for the template or, if
   * this template doesn't specify a value, the activity type's own default
   * value
   *
   * creation templates must always specify an activity type (and for now also
   * a duration)
   * //REVIEW: eventually duration should come from simulation instead
   */
  public static class Builder extends AbstractBuilder<Builder, ActivityCreationTemplate> {

    //REVIEW: perhaps separate search criteria vs default specification,
    //        eg Range(0...5) allowed, but create with 4

    /**
     * create activity instances with given default duration
     *
     * @param duration IN STORED the duration of the activity created by
     *     this template. not null
     * @return the same builder object updated with new criteria
     */
    public @NotNull
    Builder duration(@NotNull Duration duration) {
      this.durationIn = Window.between(duration, duration);
      return getThis();
    }

    public @NotNull
    Builder duration(@NotNull Window duration) {
      this.durationIn = duration;
      return getThis();
    }

    protected DurationExpression parametricDur;

    public Builder duration(@NotNull String nameState, TimeExpression expr){
      this.parametricDur = new DurationExpressionState(new StateQueryParam(nameState, expr));
      return this;
    }

    public Builder duration(@NotNull DurationExpression durExpr){
      this.parametricDur = durExpr;
      return this;
    }

    @Override
    public Builder basedOn(ActivityCreationTemplate template) {
      type = template.type;
      startsIn = template.startRange;
      endsIn = template.endRange;
      durationIn = template.durationRange;
      startsOrEndsIn = template.startOrEndRange;
      arguments = template.arguments;
      variableArguments = template.variableArguments;
      parametricDur = template.parametricDur;
      return getThis();
    }

    /**
     * {@inheritDoc}
     */
    public @NotNull
    Builder getThis() {
      return this;
    }

    protected ActivityCreationTemplate fill(final ActivityCreationTemplate template) {
      template.startRange = this.startsIn;
      template.endRange = this.endsIn;
      template.startOrEndRange = this.startsOrEndsIn;
      if (this.parametricDur != null){
        if (this.durationIn != null){
          throw new RuntimeException("Cannot specify two different types of durations");
        }
        template.parametricDur = this.parametricDur;
      }

      if (this.type.getDurationType() instanceof DurationType.Uncontrollable) {
        if (this.parametricDur != null) {
          throw new RuntimeException("Cannot define parametric duration on activity of type "
                                     + this.type.getName()
                                     + " because its DurationType is Uncontrollable");
        }
        if(this.acceptableAbsoluteTimingError.isZero()){
          logger.warn("Root-finding is likely to fail as activity has an uncontrollable duration and the timing "
          + "precision is 0. Setting it for you at 1s. Next time, use withTimingPrecision() when building the template.");
          this.acceptableAbsoluteTimingError = Duration.of(500, Duration.MILLISECOND);
        }
      }

      template.type = this.type;

      if (this.durationIn != null) {
        template.durationRange = this.durationIn;
      }
      //REVIEW: probably want to store permissible rane separate from creation
      //        default value

      template.arguments = this.arguments;
      template.variableArguments = this.variableArguments;
      return template;
    }

    /**
     * cross-check all specified terms and construct a creation template
     *
     * creates a new template object based on the conjunction of all of the
     * criteria specified so far in this builder, with creation default
     * values as specified in this builder to override the activity type
     * default values
     *
     * multiple specifications of the same term sequentially overwrite the
     * prior term specification
     *
     * @return a newly constructed activity creation template that either
     *     matches activities meeting the conjunction of criteria
     *     specified or else creates new instances with given defaults
     */
    public ActivityCreationTemplate build() {


      if (type == null) {
        throw new IllegalArgumentException(
            "activity creation template requires non-null activity type");
      }
      final var template = new ActivityCreationTemplate();
      fill(template);
      return template;
    }

  }

  protected DurationExpression parametricDur;

  public boolean hasParametricDuration(){
    return parametricDur != null;
  }

  public DurationExpression getParametricDuration(){
    return parametricDur;
  }

  public void setParametricDuration(DurationExpression durExpression){
    this.parametricDur = durExpression;
  }

  /**
   * create activity if possible
   *
   * @param name
   * @param windows
   * @return
   */
  public @NotNull
  Optional<ActivityInstance> createActivity(String name, Windows windows, boolean instantiateVariableArguments, SimulationFacade facade, Plan plan, PlanningHorizon planningHorizon) {
    //REVIEW: how to properly export any flexibility to instance?
    for (var window : windows) {
      var act = createInstanceForReal(name, window, instantiateVariableArguments, facade, plan, planningHorizon);
      if (act.isPresent()) {
        return act;
      }
    }
    return Optional.empty();

  }

  private Optional<ActivityInstance> createInstanceForReal(final String name, final Window window, final boolean instantiateVariableArguments, SimulationFacade facade, Plan plan, PlanningHorizon planningHorizon) {
    final var act = new ActivityInstance(this.type);
    act.setArguments(this.arguments);
    act.setVariableArguments(this.variableArguments);
    final var tnw = new TaskNetworkAdapter(new TaskNetwork());
    tnw.addAct(name);
    if (window != null) {
      tnw.addEnveloppe(name, "window", window.start, window.end);
    }
    tnw.addEnveloppe(name, "planningHorizon", planningHorizon.getStartAerie(), planningHorizon.getEndAerie());
    if (this.startRange != null) {
      tnw.addStartInterval(name, this.startRange.start, this.startRange.end);
    }
    if (this.endRange != null) {
      tnw.addEndInterval(name, this.endRange.start, this.endRange.end);
    }
    if (this.durationRange != null) {
      tnw.addDurationInterval(name, this.durationRange.start, this.durationRange.end);
    }
    final var success = tnw.solveConstraints();
    if (!success) {
      logger.warn("Inconsistent temporal constraints, returning empty activity");
      return Optional.empty();
    }
    final var solved = tnw.getAllData(name);

    //the domain of user/scheduling temporal constraints have been reduced with the STN,
    //now it is time to find an assignment compatible
    //CASE 1: activity has an uncontrollable duration
    if(this.type.getDurationType() instanceof DurationType.Uncontrollable){
      final var f = new EquationSolvingAlgorithms.Function<Duration>(){
        //As simulation is called, this is not an approximation
        @Override
        public boolean isApproximation(){
          return false;
        }

        @Override
        public Duration valueAt(final Duration start) {
          final var actToSim = new ActivityInstance(act);
          actToSim.setStartTime(start);
          actToSim.instantiateVariableArguments(facade.getLatestConstraintSimulationResults());
          try {
            facade.simulateActivity(actToSim);
            final var dur = facade.getActivityDuration(actToSim);
            facade.removeActivitiesFromSimulation(List.of(actToSim));
            return dur.map(start::plus).orElse(Duration.MAX_VALUE);
          } catch (SimulationFacade.SimulationException e) {
            return Duration.MAX_VALUE;
          }
        }

      };
      try {
        var endInterval = solved.end();
        var startInterval = solved.start();

        final var durationHalfEndInterval = endInterval.duration().dividedBy(2);

        final var result = new EquationSolvingAlgorithms.SecantDurationAlgorithm().findRoot(f,
                                                                                            startInterval.start,
                                                                                            startInterval.end,
                                                                                            endInterval.start.plus(durationHalfEndInterval),
                                                                                            durationHalfEndInterval,
                                                                                            durationHalfEndInterval,
                                                                                            startInterval.start,
                                                                                            startInterval.end,
                                                                                            20);
        act.setStartTime(result.x());
        if(!f.isApproximation()){
          //f is calling simulation -> we do not need to resimulate this activity later
          act.setDuration(result.fx().minus(result.x()));
        }
        return Optional.of(act);
      } catch (EquationSolvingAlgorithms.ZeroDerivative zeroDerivative) {
        logger.debug("Rootfinding encountered a zero-derivative");
      } catch (EquationSolvingAlgorithms.DivergenceException e) {
        logger.debug("Rootfinding diverged");
        logger.debug(e.history.history().toString());
      } catch (EquationSolvingAlgorithms.ExceededMaxIterationException e) {
        logger.debug("Too many iterations");
        logger.debug(e.history.history().toString());
      } catch (EquationSolvingAlgorithms.NoSolutionException e) {
        logger.debug("No solution");
      }
      return Optional.empty();
      //CASE 2: activity has a controllable duration
    } else{
      //select earliest start time, STN guarantees satisfiability
      final var earliestStart = solved.start().start;
      act.setStartTime(earliestStart);
      if (this.parametricDur == null) {
        //select smallest duration
        act.setDuration(solved.end().start.minus(solved.start().start));
      } else {
        final var computedDur = this.parametricDur.compute(Window.between(earliestStart, earliestStart),
                                                           facade.getLatestConstraintSimulationResults());
        if (solved.duration().contains(computedDur)) {
          act.setDuration(computedDur);
        } else {
          throw new IllegalArgumentException("Parametric duration is incompatible with temporal constraints");
        }
      }
    }

    for (final var param : this.variableArguments.entrySet()) {
      if (instantiateVariableArguments) {
        act.instantiateVariableArgument(param.getKey(), facade.getLatestConstraintSimulationResults());
      }
    }
    return Optional.of(act);
  }

  /**
   * generate a new activity instance based on template defaults
   *
   * used by scheduling logic to instance a new activity that can satisfy
   * all of the template criteria in the case another matching activity
   * could not be found
   *
   * uses any defaults specified in the template to override the otherwise
   * prevailing defaults from the activity type itself
   *
   * @param name IN the activity instance identifier to associate to the
   *     newly constructed activity instance
   * @return a newly constructed activity instance with values chosen
   *     according to any specified template criteria
   */
  public @NotNull
  Optional<ActivityInstance> createActivity(String name, SimulationFacade facade, Plan plan, PlanningHorizon planningHorizon) {
    return createInstanceForReal(name,null, true, facade, plan, planningHorizon);
  }


}
