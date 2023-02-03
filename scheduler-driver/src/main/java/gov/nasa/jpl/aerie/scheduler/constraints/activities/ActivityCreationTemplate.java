package gov.nasa.jpl.aerie.scheduler.constraints.activities;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Spans;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.scheduler.EquationSolvingAlgorithms;
import gov.nasa.jpl.aerie.scheduler.NotNull;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
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
public class ActivityCreationTemplate extends ActivityExpression implements Expression<Spans> {

  private static final Logger logger = LoggerFactory.getLogger(ActivityCreationTemplate.class);


  /**
   * ctor is private to prevent inconsistent construction
   *
   * please use the enclosed fluent Builder class instead
   *
   * leaves all criteria elements unspecified
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
      this.durationIn = Interval.between(duration, duration);
      return getThis();
    }

    public @NotNull
    Builder duration(@NotNull Interval duration) {
      this.durationIn = duration;
      return getThis();
    }

    // parametric dur
    //-> either state value
    // or
    //recognize the dur in the parameters instead of a specific builder ?


    @Override
    public Builder basedOn(ActivityCreationTemplate template) {
      type = template.type;
      startsIn = template.startRange;
      endsIn = template.endRange;
      durationIn = template.durationRange;
      startsOrEndsIn = template.startOrEndRange;
      arguments = template.arguments;
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

      if (this.type.getDurationType() instanceof DurationType.Uncontrollable) {
        if(this.acceptableAbsoluteTimingError.isZero()){
          //TODO: uncomment when precision can be set by user
          //logger.warn("Root-finding is likely to fail as activity has an uncontrollable duration and the timing "
          //+ "precision is 0. Setting it for you at 1s. Next time, use withTimingPrecision() when building the template.");
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

  /**
   * create activity if possible
   *
   * @param name the activity name
   * @param windows the windows in which the activity can be instantiated
   * @return the instance of the activity (if successful; else, an empty object) wrapped as an Optional.
   */
  public @NotNull
  Optional<SchedulingActivityDirective> createActivity(String name, Windows windows, SimulationFacade facade, Plan plan, PlanningHorizon planningHorizon, EvaluationEnvironment evaluationEnvironment) {
    //REVIEW: how to properly export any flexibility to instance?
    for (var window : windows.iterateEqualTo(true)) {
      var act = createInstanceForReal(name, window, facade, plan, planningHorizon, evaluationEnvironment);
      if (act.isPresent()) {
        return act;
      }
    }
    return Optional.empty();
  }

  private Optional<SchedulingActivityDirective> createInstanceForReal(final String name, final Interval interval, SimulationFacade facade, Plan plan, PlanningHorizon planningHorizon, EvaluationEnvironment evaluationEnvironment) {
    final var tnw = new TaskNetworkAdapter(new TaskNetwork());
    tnw.addAct(name);
    if (interval != null) {
      tnw.addEnveloppe(name, "interval", interval.start, interval.end);
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
      logger.warn("Inconsistent temporal constraints, returning Optional.empty() instead of activity");
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
          final var actToSim = SchedulingActivityDirective.of(
              type,
              start,
              null,
              SchedulingActivityDirective.instantiateArguments(
                  arguments,
                  start,
                  facade.getLatestConstraintSimulationResults(),
                  evaluationEnvironment,
                  type),
              null,
              null,
              true);
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

        final var result = new EquationSolvingAlgorithms
            .SecantDurationAlgorithm()
            .findRoot(
                f,
                startInterval.start,
                startInterval.end,
                endInterval.start.plus(durationHalfEndInterval),
                durationHalfEndInterval,
                durationHalfEndInterval,
                startInterval.start,
                startInterval.end,
                20);

        Duration dur = null;
        if(!f.isApproximation()){
          //f is calling simulation -> we do not need to resimulate this activity later
          dur = result.fx().minus(result.x());
        }
        // TODO: When scheduling is allowed to create activities with anchors, this constructor should pull from an expanded creation template
        return Optional.of(SchedulingActivityDirective.of(
            type,
            result.x(),
            dur,
            SchedulingActivityDirective.instantiateArguments(
                this.arguments, result.x(),
                facade.getLatestConstraintSimulationResults(),
                evaluationEnvironment,
                type),
            null,
            null,
            true));
      } catch (EquationSolvingAlgorithms.ZeroDerivativeException zeroOrInfiniteDerivativeException) {
        logger.debug("Rootfinding encountered a zero-derivative");
      } catch (EquationSolvingAlgorithms.InfiniteDerivativeException infiniteDerivativeException) {
        logger.debug("Rootfinding encountered an infinite-derivative");
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
    } else if (this.type.getDurationType() instanceof DurationType.Controllable dt) {
      //select earliest start time, STN guarantees satisfiability
      final var earliestStart = solved.start().start;
      final var instantiatedArguments = SchedulingActivityDirective.instantiateArguments(
          this.arguments,
          earliestStart,
          facade.getLatestConstraintSimulationResults(),
          evaluationEnvironment,
          type);

      final var durationParameterName = dt.parameterName();
        //handle variable duration parameter here
      final Duration setActivityDuration;
      if (instantiatedArguments.containsKey(durationParameterName)) {
        final var argumentDuration = Duration.of(instantiatedArguments.get(durationParameterName).asInt().get(), Duration.MICROSECOND);
        if (solved.duration().contains(argumentDuration)) {
          setActivityDuration = argumentDuration;
        } else{
          logger.debug("Controllable duration set by user is incompatible with temporal constraints associated to the activity template");
          return Optional.empty();
        }
      } else {
        //REVIEW: should take default duration of activity type maybe ?
        setActivityDuration = solved.end().start.minus(solved.start().start);
      }
      // TODO: When scheduling is allowed to create activities with anchors, this constructor should pull from an expanded creation template
      return Optional.of(SchedulingActivityDirective.of(
          type,
          earliestStart,
          setActivityDuration,
          SchedulingActivityDirective.instantiateArguments(
              this.arguments,
              earliestStart,
              facade.getLatestConstraintSimulationResults(),
              evaluationEnvironment,
              type),
          null,
          null,
          true));
    } else{
     throw new UnsupportedOperationException("Duration type other than Uncontrollable and Controllable are not suppoerted");
    }
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
  Optional<SchedulingActivityDirective> createActivity(String name, SimulationFacade facade, Plan plan, PlanningHorizon planningHorizon, EvaluationEnvironment evaluationEnvironment) {
    return createInstanceForReal(name,null, facade, plan, planningHorizon, evaluationEnvironment);
  }


}
