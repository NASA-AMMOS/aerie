package gov.nasa.jpl.aerie.merlin.framework.dependency;

import java.util.List;

import static gov.nasa.jpl.aerie.merlin.framework.dependency.Dependency.parameterRead;
import static gov.nasa.jpl.aerie.merlin.framework.dependency.Dependency.resourceRead;
import static gov.nasa.jpl.aerie.merlin.framework.dependency.Dependency.resourceWrite;
import static gov.nasa.jpl.aerie.merlin.framework.dependency.DependencyObjects.activityType;
import static gov.nasa.jpl.aerie.merlin.framework.dependency.DependencyObjects.object;
import static gov.nasa.jpl.aerie.merlin.framework.dependency.DependencyType.conditional;
import static gov.nasa.jpl.aerie.merlin.framework.dependency.DependencyType.unconditional;
import static gov.nasa.jpl.aerie.merlin.framework.dependency.EffectType.set;
import static gov.nasa.jpl.aerie.merlin.framework.dependency.TemporalDependency.duringActivity;

public final class ActivityBehavior {

  public static List<Dependency> modelCall(Object object, String methodName){
    if (object instanceof Introspectable introspectableObject) {
      return introspectableObject.getDependencies(methodName);
    } else{
      throw new RuntimeException("Object " + object.toString() + " does not implement Introspectable");
    }
  }

  public static Dependency.ModelDependency.ActivityDependency.ResourceDependency read(String activityType, Object resource){
    return new Dependency.ModelDependency.ActivityDependency.ResourceDependency(
        resourceRead(activityType(activityType), object(resource)),
        duringActivity(),
        unconditional());
  }

  public static Dependency.ActivityDependency.ParameterRead read(String activityType, String parameterName){
    return new Dependency.ActivityDependency.ParameterRead(activityType(activityType), parameterName, duringActivity());
  }

  public static Dependency.ActivityDependency.ResourceDependency sets(String activityType, Object resource, Object value){
    return new Dependency.ActivityDependency.ResourceDependency(
        resourceWrite(
            activityType(activityType),
            object(resource),
            set(object(value))),
        duringActivity(),
        unconditional());
  }

  public static Dependency.ActivityDependency.ResourceDependency increases(
      final String activityType,
      final Object resource,
      final double value,
      final TemporalDependency temporalDependency){
    return new Dependency.ActivityDependency.ResourceDependency(
        resourceWrite(
            activityType(activityType),
            object(resource),
            set(value)),
        temporalDependency,
        unconditional());
  }

  public static Dependency.ActivityDependency.ResourceDependency increases(
      final String activityType,
      final Object resource,
      final Object value,
      final TemporalDependency temporalDependency){
    return new Dependency.ActivityDependency.ResourceDependency(
        resourceWrite(
            activityType(activityType),
            object(resource),
            set(object(value))),
        temporalDependency,
        unconditional());
  }

  public static Dependency.ActivityDependency.ResourceDependency increases(
      final String activityType,
      final Object resource,
      final String parameterValue,
      final String dependingOnParameter,
      final TemporalDependency temporalDependency){
    return new Dependency.ActivityDependency.ResourceDependency(
        resourceWrite(
            activityType(activityType),
            object(resource),
            set(activityType, parameterValue)),
        temporalDependency,
        conditional(List.of(), List.of(parameterRead(activityType(activityType), dependingOnParameter, temporalDependency))));
  }


  public static Dependency.ActivityDependency.ActivityGenerationDependency generates(String generating, String generated, TemporalDependency temporalDependency, String parameterDependent){
    return new Dependency.ActivityDependency.ActivityGenerationDependency(
        activityType(generating),
        activityType(generated),
        temporalDependency,
        conditional(List.of(), List.of(new Dependency.ActivityDependency.ParameterRead(activityType(generating), parameterDependent, duringActivity()))));
  }
}
