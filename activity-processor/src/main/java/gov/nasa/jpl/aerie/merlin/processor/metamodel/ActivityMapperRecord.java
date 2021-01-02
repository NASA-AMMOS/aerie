package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.PackageElement;
import java.util.Objects;

public final class ActivityMapperRecord {
  public final ClassName name;
  public final boolean isCustom;

  public ActivityMapperRecord(final ClassName name, final boolean isCustom) {
    this.name = Objects.requireNonNull(name);
    this.isCustom = isCustom;
  }

  public static ActivityMapperRecord custom(final ClassName name) {
    return new ActivityMapperRecord(name, true);
  }

  public static ActivityMapperRecord
  generatedFor(final ClassName activityTypeName, final PackageElement adaptationElement) {
    final var adaptationPackage = adaptationElement.getQualifiedName().toString();
    final var activityPackage = activityTypeName.packageName();

    final String generatedSuffix;
    if ((activityPackage + ".").startsWith(adaptationPackage + ".")) {
      generatedSuffix = activityPackage.substring(adaptationPackage.length());
    } else {
      generatedSuffix = activityPackage;
    }

    final var mapperName = ClassName.get(
        adaptationPackage + ".generated" + generatedSuffix,
        activityTypeName.simpleName() + "Mapper");

    return new ActivityMapperRecord(mapperName, false);
  }
}
