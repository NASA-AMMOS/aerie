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
  private final String name;

  /**
   * a list of constraints associated to this activity type
   */
  private final StateConstraintExpression activityConstraints;

  /**
   * the information required to simulate this activity type
   */
  private final TaskSpecType<?, ?, ?> specType;

  /**
   * ctor creates a new empty activity type container
   *
   * @param name IN the identifier of the activity type
   */
  public ActivityType(final String name) {
    checkNotNull(name, "creating activity type with null name");
    this.name = name;
    this.activityConstraints = null;
    this.specType = null;
  }

  /**
   * ctor creates a new empty activity type container
   *
   * @param name IN the identifier of the activity type
   */
  public ActivityType(final String name, final TaskSpecType<?, ?, ?> specType) {
    checkNotNull(name, "creating activity type with null name");
    this.name = name;
    this.activityConstraints = null;
    this.specType = specType;
  }

  /**
   * ctor creates a new empty activity type container
   *
   * @param name IN the identifier of the activity type
   * @param constraints constraints for the activity type
   */
  public ActivityType(final String name, final StateConstraintExpression constraints) {
    checkNotNull(name, "creating activity type with null name");
    checkNotNull(constraints, "creating activity type with null constraints");
    this.name = name;
    this.activityConstraints = constraints;
    this.specType = null;
  }

  static Parameter getParameterSpecification(final List<Parameter> params, final String name) {
    final var parameterSpecifications = params.stream()
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
    return this.name;
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
  StateConstraintExpression getStateConstraints() {
    return this.activityConstraints;
  }

  TaskSpecType<?, ?, ?> getSpecType(){
    return this.specType;
  }

  boolean isParamLegal(final String name){
    if(this.specType != null){
      final var paramSpec = getParameterSpecification(this.specType.getParameters(), name);
      return paramSpec != null;
    }
    return true;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final var that = (ActivityType) o;
    return Objects.equals(this.name, that.name)
           && Objects.equals(this.activityConstraints, that.activityConstraints)
           && Objects.equals(this.specType, that.specType);
  }

}
