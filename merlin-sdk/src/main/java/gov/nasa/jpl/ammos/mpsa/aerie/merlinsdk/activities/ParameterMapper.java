package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

/**
 * A mapping between (a) the mission-specific representation of a data type defined by an adaptation (b) to a
 * mission-agnostic representation of that data type.
 */
public interface ParameterMapper<T> {
  /**
   * Gets a schema for the parameter type understood by this ParameterMapper.
   *
   * @return A parameter schema.
   */
  ParameterSchema getParameterSchema();

  /**
   * Produces an adaptation-specific parameter domain object from a mission-agnostic representation.
   *
   * @param serializedParameter A mission-agnostic representation of a parameter instance.
   * @return Either an adaptation-specific domain object, or a deserialization failure.
   */
  Result<T, String> deserializeParameter(SerializedParameter serializedParameter);

  /**
   * Produces a mission-agnostic representation of an adaptation-specific parameter domain object.
   *
   * @param parameter An adaptation-specific domain object.
   * @return A mission-agnostic representation of {@code parameter}.
   */
  SerializedParameter serializeParameter(T parameter);
}
