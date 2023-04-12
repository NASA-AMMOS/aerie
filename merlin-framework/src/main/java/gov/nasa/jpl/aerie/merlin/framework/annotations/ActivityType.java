package gov.nasa.jpl.aerie.merlin.framework.annotations;

import gov.nasa.jpl.aerie.merlin.framework.ActivityMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

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
    Class<? extends ActivityMapper<?, ?, ?>> value();
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

  /**
   * Use when an activity has a constant statically-known duration.
   *
   * Apply to either a static {@link Duration} field or a static no-arg method
   * that returns {@link Duration}. For correctness, it is recommended that you use the field or method
   * in the effect model to ensure that the duration is what you say it is - the duration is verified
   * by the scheduler, but only after a (potentially expensive) simulation.
   *
   * This annotation is optional, but it is highly recommended if applicable. The scheduler will assume
   * your activity has an uncontrollable variable duration if no duration annotation is present, which
   * will cause extra simulations in scheduling.
   */
  @Retention(RetentionPolicy.CLASS)
  @Target({ ElementType.FIELD, ElementType.METHOD })
  @interface FixedDuration {}
}
