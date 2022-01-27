package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * general re-usable description of a modeled system behavior
 *
 * wraps up information regarding how the execution of a specific kind of
 * system behavior would be invoked and how it would unfold, including
 * any parameters that can be used to control the execution
 *
 * the details in the descriptions may be provided by the system modeler or
 * may be learned from observing previous executions
 */
public class ActivityType {

  /**
   * ctor creates a new empty activity type container
   *
   * @param name IN the identifier of the activity type
   */
  public ActivityType(String name) {
    checkNotNull(name, "creating activity type with null name");
    this.name = name;
  }


  /**
   * ctor creates a new empty activity type container
   *
   * @param name IN the identifier of the activity type
   * @param constraints constraints for the activity type
   */
  public ActivityType(String name, StateConstraintExpression constraints) {
    this(name);
    checkNotNull(constraints, "creating activity type with null constraints");
    this.activityConstraints = constraints;
  }


  /**
   * fetches the identifier associated with this activity type
   *
   * @return the identifier associated with this activity type
   */
  public String getName() {
    return name;
  }

  public void setParameter(String name, Object value) {
    checkNotNull(name, "setting parameter with null name");
    checkNotNull(value, "setting parameter with null value");
    parameters.put(name, value);
  }


  /**
   * fetches the set of constraints required by instances of this activity type
   *
   * the validity of the activity behavior predictions contained in this
   * type are dependent on all of the activity type constraints being met
   *
   * @return an immutable, possibly empty, list of the constraints associated
   *     with this activity type and inherited by all matching activity
   *     instances
   */
  public StateConstraintExpression getStateConstraints() {
    return activityConstraints;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  /**
   * the identifier associated with this activity type
   */
  String name;
  /**
   * a list of constraints associated to this activity type
   */
  StateConstraintExpression activityConstraints;
  //TODO: this is never initialized.
  public TaskSpecType<?,?> specType;
  Map<String, Object> parameters = new HashMap<String, Object>();

}
