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

  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.TYPE)
  @interface WithExecutor {
    Executor value();
  }

  enum Executor { Threaded, Replaying }

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
}
