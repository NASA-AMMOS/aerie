package gov.nasa.jpl.ammos.mpsa.aerie.merlin.processor.metamodel;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Objects;

public final class AdaptationRecord {
  public final PackageElement $package;
  public final TypeElement topLevelModule;
  public final List<ActivityTypeRecord> activityTypes;

  public AdaptationRecord(
      final PackageElement $package,
      final TypeElement topLevelModule,
      final List<ActivityTypeRecord> activityTypes)
  {
    this.$package = Objects.requireNonNull($package);
    this.topLevelModule = Objects.requireNonNull(topLevelModule);
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

  public ClassName getModuleName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "ModuleX");
  }
}
