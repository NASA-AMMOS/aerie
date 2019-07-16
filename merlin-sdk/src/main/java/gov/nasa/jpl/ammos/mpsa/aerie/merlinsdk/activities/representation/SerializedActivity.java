package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ParameterType;

import java.util.Map;

import static java.util.Collections.unmodifiableMap;

/**
 * A serializable representation of an adaptation-specific activity domain object.
 *
 * Implementors of the {@link ActivityType} protocol may be constructed from parameters (which are
 * themselves implementors of the {@link ParameterType} protocol). A SerializedActivity is an adaptation-
 * agnostic representation of the data in an activity, structured as serializable primitives
 * composed using sequences and maps.
 *
 * For instance, if a FooActivity accepts two parameters, each of which is a 3D point in
 * space, then the serialized activity may look something like:
 *
 *     { "name": "Foo", "parameters": { "source": [1, 2, 3], "target": [4, 5, 6] } }
 *
 * This allows mission-agnostic treatment of activity data for persistence, editing, and
 * inspection, while allowing mission-specific adaptation to work with a domain-relevant
 * object via (de)serialization.
 */
public final class SerializedActivity {
  private final String typeName;
  private final Map<String, SerializedParameter> parameters;

  public SerializedActivity(final String typeName, final Map<String, SerializedParameter> parameters) {
    this.typeName = typeName;
    this.parameters = parameters;
  }

  /**
   * Gets the name of the activity type associated with this serialized data.
   *
   * @return A string identifying the activity type this object may be deserialized with.
   */
  public String getTypeName() {
    return this.typeName;
  }

  /**
   * Gets the serialized parameters associated with this serialized activity.
   *
   * @return A map of serialized parameters keyed by parameter name.
   */
  public Map<String, SerializedParameter> getParameters() {
    return unmodifiableMap(this.parameters);
  }

  @Override
  public String toString() {
    return "SerializedActivity { typeName = " + this.typeName + ", parameters = " + this.parameters.toString() + " }";
  }
}
