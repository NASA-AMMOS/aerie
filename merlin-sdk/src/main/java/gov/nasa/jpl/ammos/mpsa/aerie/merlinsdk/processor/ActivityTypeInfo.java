package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

import org.apache.commons.lang3.tuple.Pair;

import javax.lang.model.type.DeclaredType;
import java.util.ArrayList;
import java.util.List;

final class ActivityTypeInfo {
  /**
   * The compile-time representation of the Java class for this activity type.
   */
  public DeclaredType javaType = null;

  /**
   * The set of parameter types this activity depends on, keyed by parameter name.
   */
  public List<Pair<String, ParameterTypeReference>> parameters = new ArrayList<>();

  /**
   * The brief description for this activity type.
   */
  public String briefDescription = null;

  /**
   * The verbose description for this activity type.
   */
  public String verboseDescription = null;

  /**
   * The name of this activity type.
   */
  public String name = null;

  /**
   * The @subsystem Javadoc tag for this activity type.
   */
  public String subsystem = null;

  /**
   * The @contact Javadoc tag for this activity type.
   */
  public String contact = null;

  /**
   * Produce a debug representation of this type.
   *
   * @return A debug representation of this type.
   */
  public String toString() {
    return "ActivityInfo { \n" +
        "  javaType = " + this.javaType.toString() + ", \n" +
        "  briefDescription = " + (this.briefDescription == null ? "<null>" : this.briefDescription) + ", \n" +
        "  verboseDescription = " + (this.verboseDescription == null ? "<null>" : this.verboseDescription) + ", \n" +
        "  name = " + (this.name == null ? "<null>" : this.name) + ", \n" +
        "  subsystem = " + (this.subsystem == null ? "<null>" : this.subsystem) + ", \n" +
        "  contact = " + (this.contact == null ? "<null>" : this.contact) + "\n" +
        "}";
  }
}
