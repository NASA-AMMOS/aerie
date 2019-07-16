package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// A serialization mapper for activities.
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ActivitiesMapped {
  /// The set of activity types to which this mapper should be registered.
  Class<? extends Activity>[] value() default {};
}
