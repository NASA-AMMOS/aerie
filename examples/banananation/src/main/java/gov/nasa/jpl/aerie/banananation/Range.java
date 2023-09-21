package gov.nasa.jpl.aerie.banananation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE_USE)
public @interface Range {
  int min();
  int max();
}
