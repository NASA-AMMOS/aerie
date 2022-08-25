package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MissionModelRecord {
  public final PackageElement $package;
  public final TypeElement topLevelModel;
  public final List<TypeRule> typeRules;
  public final List<ActivityTypeRecord> activityTypes;
  public final boolean expectsPlanStart;
  public final Optional<ConfigurationTypeRecord> modelConfigurationType;

  public MissionModelRecord(
      final PackageElement $package,
      final TypeElement topLevelModel,
      final boolean expectsPlanStart,
      final Optional<ConfigurationTypeRecord> modelConfigurationType,
      final List<TypeRule> typeRules,
      final List<ActivityTypeRecord> activityTypes)
  {
    this.$package = Objects.requireNonNull($package);
    this.topLevelModel = Objects.requireNonNull(topLevelModel);
    this.expectsPlanStart = expectsPlanStart;
    this.modelConfigurationType = Objects.requireNonNull(modelConfigurationType);
    this.typeRules = Objects.requireNonNull(typeRules);
    this.activityTypes = Objects.requireNonNull(activityTypes);
  }

  public ClassName getMerlinPluginName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "GeneratedMerlinPlugin");
  }

  public ClassName getSchedulerPluginName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "GeneratedSchedulerPlugin");
  }

  public ClassName getFactoryName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "GeneratedMissionModelFactory");
  }

  public ClassName getSchedulerModelName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "GeneratedSchedulerModel");
  }

  public ClassName getActivityActionsName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "ActivityActions");
  }

  public ClassName getTypesName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "ActivityTypes");
  }
}
