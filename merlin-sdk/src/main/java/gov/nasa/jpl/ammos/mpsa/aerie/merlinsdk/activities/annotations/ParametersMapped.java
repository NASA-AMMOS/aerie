package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityParameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// A serialization mapper for parameters.
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ParametersMapped {
  /// The set of parameter types to which this mapper should be registered.
  Class<? extends ActivityParameter>[] value() default {};
}
