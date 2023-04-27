package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import com.squareup.javapoet.ClassName;
import gov.nasa.jpl.aerie.merlin.processor.TypePattern;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class TypeRule {
  public final TypePattern head;
  public final Set<String> enumBoundedTypeParameters;
  public final List<TypePattern> parameters;
  public final ClassName factory;
  public final String method;

  public TypeRule(
      final TypePattern head,
      final Set<String> enumBoundedTypeParameters,
      final List<TypePattern> parameters,
      final ClassName factory,
      final String method) {
    this.head = Objects.requireNonNull(head);
    this.enumBoundedTypeParameters = Objects.requireNonNull(enumBoundedTypeParameters);
    this.parameters = Objects.requireNonNull(parameters);
    this.factory = Objects.requireNonNull(factory);
    this.method = Objects.requireNonNull(method);
  }
}
