package gov.nasa.jpl.aerie.merlin.processor.instantiators;

import com.squareup.javapoet.MethodSpec;
import gov.nasa.jpl.aerie.merlin.processor.ActivityDefinitionStyle;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityParameterRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityTypeRecord;

import javax.lang.model.element.TypeElement;
import java.util.List;

public interface ActivityMapperInstantiator {
  /**
   * Returns the default template factory method or constructor for a given activity
   * type depending on whether it was written as a Java 16 record-style activity or
   * a traditional non-record class activity.

   * Ex. Non-record class
   * returns "new BiteBananaActivity()"

   * Ex. Record-Style
   * returns "new BiteBananaActivity.defaults()"
   * where "defaults" is the factory method annotated with the @Template annotation
   */
  MethodSpec makeInstantiateDefaultMethod(final ActivityTypeRecord activityType);

  MethodSpec makeInstantiateMethod(final ActivityTypeRecord activityType);

  MethodSpec makeGetArgumentsMethod(final ActivityTypeRecord activityType);

  List<ActivityParameterRecord> getActivityParameters(final TypeElement activityTypeElement);
}
