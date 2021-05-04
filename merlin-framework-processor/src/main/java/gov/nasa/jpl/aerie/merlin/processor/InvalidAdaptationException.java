package gov.nasa.jpl.aerie.merlin.processor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;

public class InvalidAdaptationException extends Exception {
  public final Element element;
  public final AnnotationMirror annotation;
  public final AnnotationValue attribute;

  public InvalidAdaptationException(
      final String message,
      final Element element,
      final AnnotationMirror annotation,
      final AnnotationValue attribute)
  {
    super(message);
    this.element = element;
    this.annotation = annotation;
    this.attribute = attribute;
  }

  public InvalidAdaptationException(final String message, final Element element, final AnnotationMirror annotation) {
    this(message, element, annotation, null);
  }

  public InvalidAdaptationException(final String message, final Element element) {
    this(message, element, null);
  }

  public InvalidAdaptationException(final String message) {
    this(message, null, null);
  }
}
