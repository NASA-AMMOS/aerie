package gov.nasa.jpl.aerie.merlin.framework.dependency;


import java.util.Arrays;
import java.util.List;

public sealed interface Dependency {
  sealed interface ModelDependency extends Dependency {
    record ResourceRead(DependencyObjects reading, DependencyObjects read) implements ModelDependency {}
    record ResourceWrite(DependencyObjects writing, DependencyObjects written, EffectType effectType) implements ModelDependency {}
  }

  sealed interface ActivityDependency extends Dependency{
    record ActivityGenerationDependency(DependencyObjects.ActivityType generating, DependencyObjects.ActivityType generated, TemporalDependency at, DependencyType dependencyType) implements ActivityDependency {}
    record ParameterRead(DependencyObjects.ActivityType activityType, String parameterName, TemporalDependency at) implements ActivityDependency {}
    record ResourceDependency(ModelDependency modelDependency, TemporalDependency at, DependencyType dependencyType) implements ActivityDependency {}
  }

  static ActivityDependency.ParameterRead parameterRead(DependencyObjects.ActivityType activityType, String parameterName, TemporalDependency at){
    return new ActivityDependency.ParameterRead(activityType, parameterName, at);
  }

  //any element can read, but the only that can be read is the runtime one
  static ModelDependency.ResourceRead resourceRead(DependencyObjects reading, DependencyObjects.RuntimeObject read){
    return new ModelDependency.ResourceRead(reading, read);
  }

  //any element can write, but the only that can be written is the runtime one
  static ModelDependency.ResourceWrite resourceWrite(DependencyObjects writing, DependencyObjects.RuntimeObject written, EffectType effectType){
    return new ModelDependency.ResourceWrite(writing, written, effectType);
  }

  static List<ModelDependency.ResourceWrite> resourceWrites(DependencyObjects writing, DependencyObjects.RuntimeObject... written){
    return Arrays.stream(written).map(w -> resourceWrite(writing, w, EffectType.undefinedEffect())).toList();
  }
}
