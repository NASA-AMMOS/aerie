package gov.nasa.jpl.aerie.merlin.processor.instantiators;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import gov.nasa.jpl.aerie.merlin.framework.NoDefaultInstanceException;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityTypeRecord;

import javax.lang.model.element.Modifier;

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
  default MethodSpec makeInstantiateDefaultMethod(final ActivityTypeRecord activityType) {
    var methodBuilder = MethodSpec.methodBuilder("instantiateDefault")
                                  .addModifiers(Modifier.PUBLIC)
                                  .addAnnotation(Override.class)
                                  .returns(TypeName.get(activityType.declaration.asType()));

    // There are no defaults if the activity has AllRequired parameters
    // As a result, no method shall be created.
    // Unless there are no parameters, in which case a default no-arg constructor may be called.
    if (activityType.parameters.isEmpty()) {
      methodBuilder.addStatement("return new $T()", TypeName.get(activityType.declaration.asType()));
    } else {
      methodBuilder.addStatement("throw new $T()", NoDefaultInstanceException.class);
    }

    return methodBuilder.build();
  }

  MethodSpec makeInstantiateMethod(final ActivityTypeRecord activityType);

  MethodSpec makeGetArgumentsMethod(final ActivityTypeRecord activityType);
}
