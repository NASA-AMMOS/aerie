package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

/**
 * A mapping between (a) the mission-specific representation of a data type defined by an adaptation (b) to a
 * mission-agnostic representation of that data type.
 */
public interface ValueMapper<T> {
  /**
   * Gets a schema for the kind of value understood by this ValueMapper.
   *
   * @return A parameter schema.
   */
  ParameterSchema getValueSchema();

  /**
    * Produces an adaptation-specific domain value from a mission-agnostic representation.
   *
   * @param serializedValue A mission-agnostic representation of a domain value.
   * @return Either an adaptation-specific domain object, or a deserialization failure.
   */
  Result<T, String> deserializeValue(SerializedParameter serializedValue);

  /**
   * Produces a mission-agnostic representation of an adaptation-specific domain value.
   *
   * @param value An adaptation-specific domain object.
   * @return A mission-agnostic representation of {@code value}.
   */
  SerializedParameter serializeValue(T value);
}
