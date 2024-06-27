package gov.nasa.jpl.aerie.merlin.framework.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//tells us that we don't need a Mission object (and thus simulation) to decompose
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface ParametricDecomposition{}

