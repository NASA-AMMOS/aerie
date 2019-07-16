package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// A state used as input for an activity.
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface InputState {
  /// The key for this state in the state dictionary.
  String value();
}
