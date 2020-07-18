package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// A parameter to an activity.
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ParameterType {
}
