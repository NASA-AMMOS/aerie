package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Adaptation {
  /**
   * The name of this adaptation. For instance, "Cassini-MSS".
   *
   * The name should be human-meaningful, such as for selection in a list of adaptations.
   *
   * @return The name of this adaptation.
   */
  String name();

  /**
   * The version of this adaptation. For instance, "2019.06.19-beta".
   *
   * The version need not be human-meaningful, but should act as a disambiguator between
   * different versions of the same adaptation.
   *
   * @return The version of this adaptation.
   */
  String version();
}
