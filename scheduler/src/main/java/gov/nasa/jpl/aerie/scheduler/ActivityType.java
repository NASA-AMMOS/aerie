package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
   * the identifier associated with this activity type
   */
  final String name;

  /**
   * a list of constraints associated to this activity type
   */
  StateConstraintExpression activityConstraints;

  /**
   * the information required to simulate this activity type
   */
  TaskSpecType<?, ?, ?> specType;

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
   */
  public ActivityType(String name, TaskSpecType<?, ?, ?> specType) {
    checkNotNull(name, "creating activity type with null name");
    this.name = name;
    this.specType = specType;
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

  public static Parameter getParameterSpecification(List<Parameter> params, String name) {
    var parameterSpecifications = params.stream()
        .filter(var -> var.name().equals(name))
        .collect(Collectors.toList());
    if(parameterSpecifications.isEmpty()){
      return null;
    }
    assert(parameterSpecifications.size()==1);
    return parameterSpecifications.get(0);
  }

  /**
   * fetches the identifier associated with this activity type
   *
   * @return the identifier associated with this activity type
   */
  public String getName() {
    return name;
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

  public TaskSpecType<?, ?, ?> getSpecType(){
    return specType;
  }

  public boolean isParamLegal(String name){
    if(specType!= null){
      var paramSpec = getParameterSpecification(specType.getParameters(), name);
      return paramSpec != null;
    }
    return true;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ActivityType that = (ActivityType) o;
    return Objects.equals(name, that.name)
           && Objects.equals(activityConstraints, that.activityConstraints)
           && Objects.equals(specType, that.specType);
  }

}
