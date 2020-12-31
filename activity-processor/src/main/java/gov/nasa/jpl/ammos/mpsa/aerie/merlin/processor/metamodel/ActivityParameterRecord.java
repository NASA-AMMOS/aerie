package gov.nasa.jpl.ammos.mpsa.aerie.merlin.processor.metamodel;

import javax.lang.model.type.TypeMirror;
import java.util.Objects;

public final class ActivityParameterRecord {
  public final String name;
  public final TypeMirror type;

  public ActivityParameterRecord(final String name, final TypeMirror type) {
    this.name = Objects.requireNonNull(name);
    this.type = Objects.requireNonNull(type);
  }
}
