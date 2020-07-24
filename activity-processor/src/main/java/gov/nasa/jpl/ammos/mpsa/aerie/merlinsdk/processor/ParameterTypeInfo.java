package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

import org.apache.commons.lang3.tuple.Pair;

import javax.lang.model.type.DeclaredType;
import java.util.ArrayList;
import java.util.List;

final class ParameterTypeInfo {
  /**
   * The compile-time representation of the Java class for this parameter type.
   */
  public DeclaredType javaType = null;

  /**
   * The set of sub-parameter types this parameter type depends on, keyed by sub-parameter name.
   */
  public List<Pair<String, ParameterTypeReference>> parameters = new ArrayList<>();

  /**
   * The brief description for this parameter type.
   */
  public String briefDescription = null;

  /**
   * The verbose description for this parameter type.
   */
  public String verboseDescription = null;
}
