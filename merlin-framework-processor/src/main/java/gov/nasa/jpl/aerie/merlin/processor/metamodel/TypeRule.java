package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import com.squareup.javapoet.ClassName;
import gov.nasa.jpl.aerie.merlin.processor.TypePattern;

import javax.lang.model.element.AnnotationMirror;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record TypeRule(
    TypePattern head,
    Set<String> enumBoundedTypeParameters,
    List<TypePattern> parameters,
    ClassName factory,
    String method,
    Optional<String> label
) {
  public TypeRule(
      final TypePattern head,
      final Set<String> enumBoundedTypeParameters,
      final List<TypePattern> parameters,
      final ClassName factory,
      final String method)
  {
    this(head, enumBoundedTypeParameters, parameters, factory, method, Optional.empty());
  }
  public TypeRule(
      final TypePattern head,
      final Set<String> enumBoundedTypeParameters,
      final List<TypePattern> parameters,
      final ClassName factory,
      final String method,
      final String name)
  {
    this(head, enumBoundedTypeParameters, parameters, factory, method, Optional.of(name));
  }
}
