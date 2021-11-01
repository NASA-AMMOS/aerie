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

  //Marks the default activity instance whose arguments are all defaulted
  //Primarily used for All Optional Parameter types
  //Add handling for @target on no-arg constructors and static instances
  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.METHOD)
  @interface Template {}

  //Declares Factory method for instantiation
  //Primarily used for Some Optional Parameter types
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @interface Factory {
    String value() default "";

    //Computes defaults for a given factory
    //Usage will involve dependency injection from a modeler provided default class
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
    @interface WithDefaults {
      Class<?> value();
    }
  }

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

  enum Executor { Threaded, Replaying, Default }

  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.METHOD)
  @interface EffectModel {
    Executor value() default Executor.Default;
  }
}
