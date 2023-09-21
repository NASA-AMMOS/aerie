package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.merlin.framework.annotations.AutoValueMapper;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//@AutoValueMapper.Annotation("unit")
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE_USE)
public @interface Unit {
  String value();
}
