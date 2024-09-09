package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record MissionModelRecord(
    PackageElement $package,
    TypeElement topLevelModel,
    boolean expectsPlanStart,
    Optional<InputTypeRecord> modelConfigurationType,
    List<TypeRule> typeRules,
    List<ActivityTypeRecord> activityTypes,
    //from compound to list of tasknettemplate
    Map<TypeElement, List<TypeElement>> methodsByCompound) {

  public ClassName getMerlinPluginName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "GeneratedMerlinPlugin");
  }

  public ClassName getSchedulerPluginName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "GeneratedSchedulerPlugin");
  }

  public ClassName getModelTypeName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "GeneratedModelType");
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

  public ClassName getAutoValueMappersName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "AutoValueMappers");
  }

  public ClassName getActivityReferenceBuildersName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "ActivityReferences");
  }
}
