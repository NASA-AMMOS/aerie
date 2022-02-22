package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

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

    public Builder duration(@NotNull ExternalState state, TimeExpression expr){
      this.parametricDur = new DurationExpressionState(new StateQueryParam(state, expr));
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

    protected ActivityCreationTemplate fill(ActivityCreationTemplate template) {
      template.startRange = startsIn;
      template.endRange = endsIn;
      template.startOrEndRange = startsOrEndsIn;
      if(parametricDur!=null){
        if(durationIn!= null){
          throw new RuntimeException("Cannot specify two different types of durations");
        }
        template.parametricDur = parametricDur;
      }
      template.type = type;

      if (durationIn != null) {
        template.durationRange = durationIn;
      }
      //REVIEW: probably want to store permissible rane separate from creation
      //        default value

      template.arguments = arguments;
      template.variableArguments = variableArguments;
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

  /**
   * create activity if possible
   *
   * @param name
   * @param windows
   * @return
   */
  public @NotNull
  ActivityInstance createActivity(String name, Windows windows, boolean instantiateVariableArguments) {
    //REVIEW: how to properly export any flexibility to instance?

    for (var window : windows) {
      //success = STNProcess(window);
      var act = createInstanceForReal(name, window, instantiateVariableArguments);
      if (act!=null) {
        return act;
      }
    }
    return null;

  }

  private ActivityInstance createInstanceForReal(final String name, final Window window, final boolean instantiateVariableArguments) {
    final var act = new ActivityInstance(this.type);
    act.setArguments(this.arguments);
    act.setVariableArguments(this.variableArguments);
    final var tnw = new TaskNetworkAdapter(new TaskNetwork());
    tnw.addAct(name);
    if (window != null) {
      tnw.addEnveloppe(name, "window", window.start, window.end);
    }
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
      System.out.println("Inconsistent temporal constraints, returning empty activity");
      return null;
    }
    final var solved = tnw.getAllData(name);
    //select earliest start time
    final var earliestStart = solved.start().start;
    act.setStartTime(earliestStart);
    if (this.parametricDur == null) {
      //select smallest duration
      act.setDuration(solved.duration().start);
    } else {
      final var computedDur = this.parametricDur.compute(Window.between(earliestStart, earliestStart));
      if (solved.duration().contains(computedDur)) {
        act.setDuration(computedDur);
      } else {
        throw new IllegalArgumentException("Parametric duration is incompatible with temporal constraints");
      }
    }

    for (final var param : this.variableArguments.entrySet()) {
      if (instantiateVariableArguments) {
        act.instantiateVariableArgument(param.getKey());
      }
    }
    return act;
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
  ActivityInstance createActivity(String name) {
    return createInstanceForReal(name,null, true);
  }


}
