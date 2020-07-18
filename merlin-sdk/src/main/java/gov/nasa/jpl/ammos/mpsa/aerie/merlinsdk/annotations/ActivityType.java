package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An activity type.
 */
// Retain at runtime so that activity serializers can switch on the activity type name.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ActivityType {
  /// The serialized name of this activity type.
  String name();
  boolean generateMapper() default false;
}
