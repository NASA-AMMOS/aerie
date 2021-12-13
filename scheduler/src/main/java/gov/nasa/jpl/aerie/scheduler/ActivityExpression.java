package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.HashMap;
import java.util.Map;

/**
 * the criteria used to identify activity instances in scheduling goals
 *
 * the template is a partial specification of an activity instance that
 * can be used to identify candidate activity instances in the plan. it
 * amounts to matching predicate or simple database record query
 *
 * the template can be used by scheduling goals to identify both activities
 * that serve to satisfy the goal as well as other activities that trigger
 * some other conditions in the goal.
 *
 * for example "an image activity of at least 10s duration taken with the
 * green filter" or "every orbit trim maneuver after JOI"
 *
 * templates may be fluently constructed via builders that parse like first
 * order logic predicate clauses, used in building up scheduling rules
 */
public class ActivityExpression {

  private Windows startOrEndRangeW;

  @SuppressWarnings("unchecked")
  public <B extends AbstractBuilder<B, AT>, AT extends ActivityExpression> AbstractBuilder<B, AT> getNewBuilder() {
    return (AbstractBuilder<B, AT>) new Builder();
  }

  /**
   * ctor is private to prevent inconsistent construction
   *
   * please use the enclosed fluent Builder class instead
   *
   * leaves all criteria fields unspecified
   */
  protected ActivityExpression() { }

  /**
   * a fluent builder class for constructing consistent template queries
   *
   * using the builder is intended to read like a predicate logic clause
   *
   * each different term added to the builder via method calls become part of
   * a logical conjection, ie matching activities must meet all of the
   * specified criteria
   *
   * existing terms can be replaced by calling the same method again, ie
   * matching activities must only meet the last-specified term
   *
   * the builder checks for consistency among all specified terms at least by
   * the final build() call
   *
   * @param <B> concrete builder type, used to ensure right builder returned
   *     by each chained operation (ref curiously recuring template
   *     pattern)
   * @param <AT> concrete activity template type constructed by the builder
   */
  public abstract static class AbstractBuilder<B extends AbstractBuilder<B, AT>, AT extends ActivityExpression> {


    Map<String, Object> parameters = new HashMap<String, Object>();

    public <T> B withParameter(String param, QueriableState<T> state, TimeExpression timeToQuery) {
      parameters.put(param, new StateQueryParam<T>(state, timeToQuery));
      return getThis();
    }

    public <T> B withParameter(String param, QueriableState<T> state) {
      parameters.put(param, new StateQueryParam<T>(state, TimeExpression.atStart()));
      return getThis();
    }

    public B withParameter(String param, Object val) {
      parameters.put(param, val);
      return getThis();
    }

    /**
     * requires activities be of a specific activity type
     *
     * the matching instance is allowed to have a type that is derived from
     * the requested type, akin to java instanceof semantics
     *
     * @param type IN STORED required activity type for matching instances,
     *     which should not change while the template exists, or null
     *     if no specific type is required
     * @return the same builder object updated with new criteria
     */
    public @NotNull
    B ofType(@Nullable ActivityType type) {
      this.type = type;
      return getThis();
    }

    protected @Nullable
    ActivityType type;

    /**
     * requires activities have a scheduled start time in a specified range
     *
     * activities without a concrete scheduled start time will not match
     *
     * @param range IN STORED the range of allowed values for start time, or
     *     null if no specific start time is required. should not change
     *     while the template exists. the range itself determines if
     *     inclusive or exclusive at its end points
     * @return the same builder object updated with new criteria
     */
    public @NotNull
    B startsIn(@Nullable Window range) {
      this.startsIn = range;
      return getThis();
    }

    protected @Nullable Window startsIn;

    /**
     * requires activities have a scheduled start or end time in a specified range
     *
     * activities without a concrete scheduled start time will not match
     *
     * @param range IN STORED the range of allowed values for start time, or
     *     null if no specific start time is required. should not change
     *     while the template exists. the range itself determines if
     *     inclusive or exclusive at its end points
     * @return the same builder object updated with new criteria
     */
    public @NotNull
    B startsOrEndsIn(@Nullable Window range) {
      this.startsOrEndsIn = range;
      return getThis();
    }

    protected @Nullable Window startsOrEndsIn;

    /**
     * requires activities have a scheduled start or end time in a specified range
     *
     * activities without a concrete scheduled start time will not match
     *
     * @param windows IN STORED the range of allowed values for start time, or
     *     null if no specific start time is required. should not change
     *     while the template exists. the range itself determines if
     *     inclusive or exclusive at its end points
     * @return the same builder object updated with new criteria
     */
    public @NotNull
    B startsOrEndsIn(@Nullable Windows windows) {
      this.startsOrEndsInW = windows;
      return getThis();
    }

    protected @Nullable
    Windows startsOrEndsInW;

    /**
     * requires activities have a scheduled end time in a specified range
     *
     * activities without a concrete scheduled start time will not match
     *
     * @param range IN STORED the range of allowed values for start time, or
     *     null if no specific start time is required. should not change
     *     while the template exists. the range itself determines if
     *     inclusive or exclusive at its end points
     * @return the same builder object updated with new criteria
     */
    public @NotNull
    B endsIn(@Nullable Window range) {
      this.endsIn = range;
      return getThis();
    }

    protected @Nullable Window endsIn;

    public @NotNull
    B startsIn(Windows ranges) {
      this.startsInR = ranges;
      return getThis();
    }

    protected Windows startsInR;

    /**
     * requires activities have a simulated duration in a specified range
     *
     * activities without a concrete simulated duration will not match
     *
     * @param range IN STORED the range of allowed values for duration, or
     *     null if no specific duration is required. should not change
     *     while the template exists. the range itself determines if
     *     inclusive or exclusive at its end points
     * @return the same builder object updated with new criteria
     */
    public @NotNull
    B durationIn(@Nullable Window range) {
      this.durationIn = range;
      return getThis();
    }

    protected @Nullable Window durationIn;

    /**
     * requires activities have instance name matching given regular expression
     *
     * the regular expression semantics is as for java.util.regex.Pattern
     *
     * @param pattern IN the regular expression for the allowed activity
     *     instance names, or null if no specific name pattern is required
     * @return the same builder object updated with new criteria
     */
    public @NotNull
    B nameMatches(@Nullable String pattern) {
      this.nameMatches = pattern;
      return getThis();
    }

    protected @Nullable String nameMatches;

    /**
     * bootstraps a new query builder based on existing template
     *
     * the new builder may then be modified without impacting the existing
     * template criteria, eg by adding additional new terms or replacing
     * existing terms
     *
     * @param template IN the template whose criteria should be duplicated
     *     into this builder. must not be null.
     * @return the same builder object updated with new criteria
     */
    public abstract @NotNull
    B basedOn(@NotNull AT template);

    /**
     * bootstraps a new query builder based on an existing activity instance
     *
     * the new builder may then be modified without impacting the existing
     * activity instance, eg by adding additional new terms or replacing
     * existing terms
     *
     * @param existingAct IN the activity instance that serves as the
     *     prototype for the new search criteria. must not be null.
     * @return the same builder object updated with new criteria
     */
    public @NotNull
    B basedOn(@NotNull ActivityInstance existingAct) {
      type = existingAct.getType();

      if (existingAct.getStartTime() != null) {
        startsIn = Window.at(existingAct.getStartTime());
      }

      if (existingAct.getDuration() != null) {
        durationIn = Window.at(existingAct.getDuration());
      }

      if (existingAct.getName() != null) {
        nameMatches = existingAct.getName();
      }

      //FINISH: extract all param values as == criteria

      return getThis();
    }

    /**
     * returns this builder object for further chaining, typed at concrete level
     *
     * @return the concrete builder type object for further method chaining
     */
    public abstract @NotNull
    B getThis();

    /**
     * collect and cross-check all specified terms and construct the template
     *
     * creates a new template object based on a conjunction of all of the
     * criteria specified so far in this builder
     *
     * multiple specifications of the same term sequentially overwrite the
     * prior term specification
     *
     * the terms are checked for high level self-consistency, but it is still
     * possible to construct predicates that will never match any activities
     *
     * @return a newly constructed template that matches activities meeting
     *     the conjunction of all criteria specified to the builder
     */
    public abstract @NotNull
    AT build();

  }

  /**
   * {@inheritDoc}
   *
   * concrete builder used to create instances of ActivityTemplate (and not a more
   * specific type like ActivityCreationTemplate)
   */
  public static class Builder extends AbstractBuilder<Builder, ActivityExpression> {

    /**
     * {@inheritDoc}
     */
    public @NotNull
    Builder getThis() {
      return this;
    }

    @Override
    public @NotNull
    Builder basedOn(@NotNull ActivityExpression template) {
      type = template.type;
      startsIn = template.startRange;
      endsIn = template.endRange;
      durationIn = template.durationRange;
      startsOrEndsIn = template.startOrEndRange;
      nameMatches = (template.nameRE != null) ? template.nameRE.pattern() : null;
      parameters = template.parameters;

      return getThis();
    }


    protected ActivityExpression fill(ActivityExpression template) {
      template.type = type;
      template.startRange = startsIn;
      template.endRange = endsIn;
      template.durationRange = durationIn;
      template.startOrEndRange = startsOrEndsIn;
      template.startOrEndRangeW = startsOrEndsInW;
      template.nameRE = (nameMatches != null)
          ? java.util.regex.Pattern.compile(nameMatches) : null;

      template.parameters = parameters;
      return template;
    }

    /**
     * {@inheritDoc}
     */
    public @NotNull
    ActivityExpression build() {
      final var template = new ActivityExpression();
      fill(template);
      return template;
    }
  }


  /**
   * range of allowed values for matching activity scheduled start times
   *
   * activities with null start time do not match any non-null range
   *
   * null if no limit on start time
   *
   * the range itself determines if endpoints are inclusive or exclusive
   */
  protected @Nullable Window startRange;

  /**
   * range of allowed values for matching activity scheduled end times
   *
   * activities with null start time do not match any non-null range
   *
   * null if no limit on start time
   *
   * the range itself determines if endpoints are inclusive or exclusive
   */
  protected @Nullable Window endRange;
  /**
   * range of allowed values for matching activity scheduled end times
   *
   * activities with null start time do not match any non-null range
   *
   * null if no limit on start time
   *
   * the range itself determines if endpoints are inclusive or exclusive
   */
  protected @Nullable Window startOrEndRange;


  /**
   * range of allowed values for matching activity simulated durations
   *
   * activities with null duration do not match any non-null range
   *
   * null if no limit on duration
   *
   * the range itself determines if endpoints are inclusive or exclusive
   */
  protected @Nullable Window durationRange;

  /**
   * the bounding super-type for matching activities
   *
   * activities with types derived from target type also match
   *
   * null if no limit on activity type
   */
  protected @Nullable
  ActivityType type;

  /**
   * regular expression of matching activity instance names
   *
   * activities with null names do not match any non-null regular expression
   *
   * null if no limit on activity instance name
   */
  protected @Nullable java.util.regex.Pattern nameRE;

  /**
   * fetch the range of allowed starting times matched by this template
   *
   * @return the allowed range of start times for matching activities, or null
   *     if no limit on start time
   */
  public @Nullable
  Window getStartRange() { return startRange; }

  /**
   * fetch the range of allowed simulation durations matched by this template
   *
   * @return the allowed range of durations for matching activities, or null
   *     if no limit on duration
   */
  public @Nullable
  Window getDurationRange() { return durationRange; }

  /**
   * fetch the bounding super type of activities matched by this template
   *
   * @return the super type for matching activities, or null if no limit on
   *     activity type
   */
  public @Nullable
  ActivityType getType() { return type; }

  /**
   * creates a template matching a given activity type (or its subtypes)
   *
   * shorthand factory method used in the common case of constraining
   * activities by their type, equievelent to new ActivityTyemplate.
   * Builder().ofType(t).build().
   *
   * @param type IN STORED the required activity type for matching activities.
   *     not null.
   * @return an activity template that matches only activities with the
   *     specified super type
   */
  public static @NotNull
  ActivityExpression ofType(@NotNull ActivityType type) {
    return new Builder().ofType(type).build();
  }


  Map<String, Object> parameters = new HashMap<String, Object>();

  /**
   * determines if the given activity matches all criteria of this template
   *
   * if no criteria have been specified, any activity matches the template
   *
   * @param act IN the activity to evaluate against the template criteria.
   *     not null.
   * @return true iff the given activity meets all of the criteria specified
   *     by this template, or false if it does not meet one or more of
   *     the template criteria
   */
  public boolean matches(@NotNull ActivityInstance act) {
    boolean match = true;

    //REVIEW: literal object equality is probably correct for type
    match = match && (type == null || type == act.getType());

    if (match && startRange != null) {
      final var startT = act.getStartTime();
      match = (startT != null) && startRange.contains(startT);
    }

    if (match && startOrEndRange != null) {
      final var startT = act.getStartTime();
      final var endT = act.getEndTime();
      match =
          ((startT != null) && startOrEndRange.contains(startT)) || (endT != null) && startOrEndRange.contains(endT);
    }

    if (match && startOrEndRangeW != null) {
      final var startT = act.getStartTime();
      final var endT = act.getEndTime();
      match = ((startT != null) && startOrEndRangeW.includes(Window.at(startT))
              || (endT != null) && startOrEndRangeW.includes(Window.at(endT)));
    }

    if (match && endRange != null) {
      final var endT = act.getEndTime();
      match = (endT != null) && endRange.contains(endT);
    }

    if (match && durationRange != null) {
      final var dur = act.getDuration();
      match = (dur != null) && durationRange.contains(dur);
    }

    if (match && nameRE != null) {
      final var name = act.getName();
      match = nameRE.matcher(name).matches();
    }

    if (match && parameters != null) {
      Map<String, Object> params = act.getParameters();
      for (var param : parameters.entrySet()) {
        if (params.containsKey(param.getKey())) {

          if (param.getValue() instanceof StateQueryParam) {
            var sqp =(StateQueryParam) param.getValue();
            var timeOfQuery = sqp.timeExpr.computeTime(null,Window.between(act.getStartTime(), act.getEndTime()));
            if(!timeOfQuery.isSingleton()){
              throw new RuntimeException("TimeExpression must return Singleton Window for StateQuery parameters");
            }
            match = params
                .get(param.getKey())
                .equals(sqp.state.getValueAtTime(timeOfQuery.start));
          }else {
            match = params.get(param.getKey()).equals(param.getValue());
          }

        }
        if (!match) {
          break;
        }
      }
    }

    return match;
  }


}
