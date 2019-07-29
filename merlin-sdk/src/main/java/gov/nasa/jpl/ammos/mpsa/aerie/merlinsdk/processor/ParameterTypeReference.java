package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

final class ParameterTypeReference {
  /**
   * Whether the referenced parameter type is primitive.
   *
   * A "primitive" parameter type is one known to the framework by default.
   */
  public boolean isPrimitive = false;

  /**
   * The (qualified) name of the referenced parameter type.
   *
   * If the parameter type is primitive, this is simply "double", "string", etc.
   * If the parameter type is not primitive, this is the fully qualified name of the class defining the parameter type.
   */
  public String typeName = null;
}
