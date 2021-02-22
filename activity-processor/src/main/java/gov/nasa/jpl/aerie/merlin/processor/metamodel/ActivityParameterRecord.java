package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.Objects;

public final class ActivityParameterRecord {
  public final String name;
  public final TypeMirror type;
  public final Element element;

  public ActivityParameterRecord(final String name, final TypeMirror type, final Element element) {
    this.name = Objects.requireNonNull(name);
    this.type = Objects.requireNonNull(type);
    this.element = Objects.requireNonNull(element);
  }
}
