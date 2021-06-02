package gov.nasa.jpl.aerie.merlin.framework.annotations;

import gov.nasa.jpl.aerie.merlin.framework.ActivityMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ActivityType {
  String value();

  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.TYPE)
  @interface WithMapper {
    Class<? extends ActivityMapper<?>> value();
  }

  /// A parameter to a task specification.
  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.FIELD)
  @interface Parameter {
  }

  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.METHOD)
  @interface Validation {
    String value();
  }

  enum Executor { Threaded, Replaying }

  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.METHOD)
  @interface EffectModel {
    Executor value() default Executor.Threaded;
  }
}
