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

  enum Executor { Threaded, Replaying }

  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.METHOD)
  @interface EffectModel {
    Executor value() default Executor.Threaded;
  }

  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.METHOD)
  @interface ControllableDuration {
    String parameterName();
  }
}
