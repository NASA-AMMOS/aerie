package gov.nasa.jpl.aerie.merlin.framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//we are going to use ActivityType for compounds but the mission model generator will have to make sure
// it does not have a run method() and then we'll put an empty run method ourselves
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Compound {
}
