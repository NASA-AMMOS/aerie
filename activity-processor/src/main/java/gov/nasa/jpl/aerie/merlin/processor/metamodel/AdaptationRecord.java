package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Objects;

public final class AdaptationRecord {
  public final PackageElement $package;
  public final TypeElement topLevelModel;
  public final List<TypeRule> typeRules;
  public final List<ActivityTypeRecord> activityTypes;

  public AdaptationRecord(
      final PackageElement $package,
      final TypeElement topLevelModel,
      final List<TypeRule> typeRules,
      final List<ActivityTypeRecord> activityTypes)
  {
    this.$package = Objects.requireNonNull($package);
    this.topLevelModel = Objects.requireNonNull(topLevelModel);
    this.typeRules = Objects.requireNonNull(typeRules);
    this.activityTypes = Objects.requireNonNull(activityTypes);
  }

  public ClassName getMasterActivityTypesName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "ActivityTypes");
  }

  public ClassName getFactoryName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "GeneratedAdaptationFactory");
  }

  public ClassName getTaskName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "Task");
  }

  public ClassName getModelName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "Model");
  }
}
