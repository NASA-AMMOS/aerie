package gov.nasa.jpl.aerie.scheduler.goals;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.All;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.LinkedList;
import java.util.List;

/**
 * describes some criteria that is desired in the solution plans
 */
public class Goal {

  /** Set to true if partial satisfaction is ok, the scheduler will try to do its best */
  private boolean partialSatisfaction = false;

  public boolean isPartiallySatisfiable() {
    return partialSatisfaction;
  }

  /**
   * the builder can construct goals piecemeal via a series of specifier calls
   *
   * the builder's piecemal specifier method calls all modify the builder and
   * return it so that such calls may be chained directly. multiple specifier
   * calls will variously override previous specification or add on to the
   * specification as documented by the individual specifier. the overall
   * consistency of the specifiers is not finally enforced until the goal is
   * built, but may be checked ahead of that time.
   *
   * the actual goal object is created at a final call to the build() method,
   * which may also enforce consistency checks among prior specifiers. the
   * builder remains viable after a build() call for further specification and
   * creation of additional distinct goal objects.
   *
   * the builder uses the curriously recurring template pattern to ensure
   * that it creates objects of the specific goal type but still allows the
   * derived builders to leverage higher level specifier methods.
   */
  public abstract static class Builder<T extends Builder<T>> {

    /**
     * sets the human-legible name of the goal
     *
     * this specifier is required. it replaces any previous specification.
     *
     * @param name IN the human legible name of the goal
     * @return this builder, ready for additional specification
     */
    public T named(String name) {
      this.name = name;
      return getThis();
    }

    protected String name;

    /**
     * sets the beginning of the time interval over which the goal is relevant
     *
     * this specifier is required unless a forAllTimeIn() is specified. it
     * replaces any previous specification. it must be paired with an ending()
     * specification. it must not cooccur with a forAllTimeIn() specification.
     *
     * @param start IN the beginning of the time range that the goal is relevant
     * @return this builder, ready for additional specification
     */
    public T startingAt(Duration start) {
      this.starting = start;
      return getThis();
    }

    protected Duration starting;

    /**
     * sets the end of the time interval over which the goal is relevant
     *
     * this specifier is required unless a forAllTimeIn() is specified. it
     * replaces any previous specification. it must be paired with an
     * starting() specification. it must not cooccur with a forAllTimeIn()
     * specification.
     *
     * @param end IN the end of the time range that the goal is relevant
     * @return this builder, ready for additional specification
     */
    public T endingAt(Duration end) {
      this.ending = end;
      return getThis();
    }

    protected Duration ending;

    /**
     * sets the contiguous time interval over which the goal is relevant
     *
     * this specifier is required unless a paired startingAt()/endingAt() is
     * specified. it replaces any previous specification. it must not cooccur
     * with a starting() or ending() specification.
     *
     * @param range IN the time range that the goal is relevant
     * @return this builder, ready for additional specification
     */
    public T forAllTimeIn(Window range) {
      this.range = range;
      return getThis();
    }

    protected Window range;


    /**
     * allows to attach state constraints to the goal
     *
     * @param constraint IN the state constraints
     * @return this builder, ready for additional specification
     */
    public T attachStateConstraint(Expression<Windows> constraint) {
      this.resourceConstraints.add(constraint);
      return getThis();
    }

    protected  final List<Expression<Windows>> resourceConstraints = new LinkedList<>();

    public T partialSatisfaction() {
      this.partialSatisfaction = true;
      return getThis();
    }

    boolean partialSatisfaction;

    /**
     * uses all pending specifications to construct a matching new goal object
     *
     * this is typically the last client call after a chain of specifiers, but
     * the builder object remains viable to be further specified and build
     * additional goals
     *
     * @return a newly allocated goal object matching all specifications
     */
    public Goal build() { return fill(new Goal()); }

    /**
     * returns the current builder object (but typed at the lowest level)
     *
     * should be implemented by the builder at the bottom of the type heirarchy
     *
     * @return reference to the current builder object (specifically typed)
     */
    protected abstract T getThis();


    /**
     * populates the provided goal with specifiers from this builder and above
     *
     * typically called by any derived builder classes to fill in the
     * specifiers managed at this builder level and above
     *
     * @param goal IN/OUT a goal object to be filled with specifiers from this
     *     level of builder and above
     * @return the provided goal object with details filled in
     */
    protected Goal fill(Goal goal) {
      goal.name = name;

      goal.partialSatisfaction = true;

      goal.resourceConstraints = null;
      if (this.resourceConstraints.size() > 0) {
        if (this.resourceConstraints.size() > 1) {
          goal.resourceConstraints = new All(resourceConstraints);
        } else {
          goal.resourceConstraints = resourceConstraints.get(0);
        }
      }

      //REVIEW: collapse boolean logic
      if (((starting != null || ending != null) && (range != null))
          || (starting == null && ending == null && range == null)) {
        throw new IllegalArgumentException(
            "creating goal requires either startingAt/endingAt terms or an \"forAllTime\" range, but not both");
      }
      if (range != null) {
        goal.temporalContext = range;
      } else {
        goal.temporalContext = Window.between(starting, ending);
      }

      return goal;
    }

  }//Builder


  /**
   * fetches the human-legible identifier of the goal
   *
   * @return a human-legible identifier for this goal, never null
   */
  public String getName() {
    return name;
  }

  /**
   * fetch the contiguous range of time over which the goal applies
   *
   * @return the contiguous range of time over which the goal applies
   */
  public Window getTemporalContext() { return temporalContext; }

  /**
   * identifies issues in a plan that diminishes this goal's satisfaction
   *
   * the method must return the same issues in the same order given the
   * same input plan, but they need not be otherwise sorted
   *
   * an empty return list indicates that no issues could be identified in the
   * plan that this goal would care to improve upon
   *
   * @param plan IN: the plan that this goal should be evaluated against
   * @param simulationResults
   * @return a list of issues in the plan that diminish goal satisfaction
   */
  public java.util.Collection<Conflict> getConflicts(Plan plan, final SimulationResults simulationResults) {
    return java.util.Collections.emptyList();
  }

  public Expression<Windows> getResourceConstraints() {
    return resourceConstraints;
  }

  /**
   * ctor creates a new empty goal without identification
   *
   * client code should use derived type builders to instance goals
   */
  protected Goal() { }

  /**
   * ctor creates a new empty goal with default priority
   *
   * @param name IN the human legible name of the goal
   */
  protected Goal(String name) {
    if (name == null) {
      throw new IllegalArgumentException(
          "creating goal with null name");
    }
    this.name = name;
  }

  /**
   * the human-legible identifier of the goal
   *
   * never null
   */
  protected String name;

  /**
   * the contiguous range of time over which the goal applies
   */
  protected Window temporalContext;

  /**
   * state constraints applying to the goal
   */
  protected Expression<Windows> resourceConstraints;

}
