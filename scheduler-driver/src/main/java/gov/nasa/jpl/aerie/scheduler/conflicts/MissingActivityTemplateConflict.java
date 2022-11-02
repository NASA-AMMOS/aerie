package gov.nasa.jpl.aerie.scheduler.conflicts;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.goals.ActivityTemplateGoal;

/**
 * describes plan problem due to lack of a matching instance for a template
 *
 * such conflicts are typically addressed by scheduling additional activities
 * using the corresponding creation template
 */
public class MissingActivityTemplateConflict extends MissingActivityConflict {

  /**
   * ctor creates a new conflict regarding a missing activity
   *
   * @param goal IN STORED the dissatisfied goal that issued the conflict
   * @param temporalContext IN STORED the times in the plan when the goal was
   *     disatisfied enough to induce this conflict (including just the
   *     desired start times of the activity, not necessarily the end time)
   */
  public MissingActivityTemplateConflict(
      ActivityTemplateGoal goal,
      Windows temporalContext, ActivityCreationTemplate template)
  {
    super(goal);

    if (temporalContext == null) {
      throw new IllegalArgumentException(
          "creating missing activity template conflict requires non-null temporal context");
    }
    this.temporalContext = temporalContext;
    this.template = template;
  }

  /**
   * {@inheritDoc}
   *
   * the times over which the missing activity template is causing a problem
   *
   * does not consider other constraints on the possible activity, eg timing
   * with respect to other events or allowable state transitions or even
   * the activity's own duration limits
   *
   * the times encompass just the desired start times of the missing activity
   * @return
   */
  @Override
  public Windows getTemporalContext() {
    return temporalContext;
  }

  /**
   * the goal whose dissatisfaction initially created this conflict
   *
   * @return reference to the dissatisfied goal that caused this conflict
   */
  @Override
  public ActivityTemplateGoal getGoal() {
    return (ActivityTemplateGoal) super.getGoal();
  }

  /**
   * the times over which the activity templates' absence causes a problem
   *
   * see more details at accessor getTemporalContext()
   */
  private final Windows temporalContext;
  /**
   * The conflict can constraint the goal template to guide the search
   */
  private final ActivityCreationTemplate template;

  public ActivityCreationTemplate getActTemplate() {
    return template;
  }
}
