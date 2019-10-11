package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;

import java.util.Optional;

/**
 * A mission-agnostic representation of the parameter types defined by an adaptation.
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
   * @return An adaptation-specific parameter instance implementing the {@link ActivityParameter} interface,
   *   or an empty {@link Optional} if this mapper cannot deserialize the provided parameter.
   */
  Optional<T> deserializeParameter(SerializedParameter serializedParameter);

  /**
   * Produces a mission-agnostic representation of an adaptation-specific parameter domain object.
   *
   * @param parameter An adaptation-specific domain object implementing the {@link ActivityParameter} interface.
   * @return A mission-agnostic representation of {@code parameter}, or an empty {@link Optional}
   *   if this mapper does not understand the provided parameter instance.
   */
  Optional<SerializedParameter> serializeParameter(T parameter);
}
