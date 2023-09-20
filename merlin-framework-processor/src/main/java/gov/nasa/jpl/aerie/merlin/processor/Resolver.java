package gov.nasa.jpl.aerie.merlin.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import gov.nasa.jpl.aerie.merlin.framework.LabeledValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.processor.TypePattern.ClassPattern;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.TypeRule;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Resolver {
  private final Types typeUtils;
  private final Elements elementUtils;
  private final List<TypeRule> typeRules;

  public Resolver(final Types typeUtils, final Elements elementUtils, final List<TypeRule> typeRules) {
    this.typeUtils = Objects.requireNonNull(typeUtils);
    this.elementUtils = Objects.requireNonNull(elementUtils);
    this.typeRules = Objects.requireNonNull(typeRules);
  }

  public Optional<CodeBlock> instantiateMapperFor(final TypeMirror mirror) {
    final var mapperCode = applyRules(createInitialGoal(mirror));

    // TODO: Do away with this null-checking stuff, somehow.
    if (mirror.getKind() == TypeKind.DECLARED || mirror.getKind() == TypeKind.ARRAY) {
      return mapperCode.map($ -> CodeBlock.of("$L", $));
    } else {
      return mapperCode;
    }
  }

  public Optional<CodeBlock> instantiateNullableMapperFor(final TypeMirror mirror) {
    final var mapperCode = applyRules(createInitialGoal(mirror));

    // TODO: Do away with this null-checking stuff, somehow.
    if (mirror.getKind() == TypeKind.DECLARED || mirror.getKind() == TypeKind.ARRAY) {
      return mapperCode.map($ -> CodeBlock.of(
          "new $T<>(\n$>$>$L$<$<)",
          gov.nasa.jpl.aerie.contrib.serialization.mappers.NullableValueMapper.class,
          $));
    } else {
      return mapperCode;
    }
  }

  private TypePattern createInitialGoal(final TypeMirror mirror) {
    final List<TypePattern> mapperArguments;
    if (mirror.getKind().isPrimitive()) {
      final var typePattern = TypePattern.from(
          elementUtils,
          typeUtils,
          typeUtils.boxedClass((PrimitiveType) mirror).asType());
      typePattern.annotations = TypePattern.getAnnotations(elementUtils, typeUtils, mirror);
      mapperArguments = List.of(typePattern);
    } else {
      mapperArguments = List.of(TypePattern.from(elementUtils, typeUtils, mirror));
    }

    return new ClassPattern(ClassName.get(ValueMapper.class), mapperArguments, Map.of());
  }

  public Optional<CodeBlock> applyRules(final TypePattern goal) {
    if (goal instanceof ClassPattern && ((ClassPattern)goal).name.equals(ClassName.get(Class.class))) {
      final var pattern = ((ClassPattern)goal).arguments.get(0);
      return Optional.of(
          (pattern.render().equals(pattern.erasure()))
              ? CodeBlock.of("$T.class", pattern.erasure())
              : CodeBlock.of("(Class<$T>) (Object) $T.class", pattern.render(), pattern.erasure()));
    } else {
      for (final var rule : this.typeRules) {
        final var codeBlock = this.applyRule(rule, goal);
        if (codeBlock.isPresent()) return codeBlock;
      }
    }
    return Optional.empty();
  }

  private Optional<CodeBlock> applyRule(final TypeRule rule, final TypePattern goal) {
    // Extract type bindings for the type parameters of the rule
    final Map<String, TypePattern> typeMapping;
    try {
      typeMapping = rule.head.match(goal);
    } catch (TypePattern.UnificationException e) {
      return Optional.empty();
    }

    // Ensure the match satisfies the rule's type parameter bounds
    for (final var name : rule.enumBoundedTypeParameters) {
      final var pattern = typeMapping.get(name);
      if (pattern == null) return Optional.empty();
      if (!(pattern instanceof ClassPattern)) return Optional.empty();

      // Only enum bounds are supported, but could expand at some point.
      // Supporting value mapper resolvers for types like:
      // - `List<? extends Foo>` or
      // - `List<? extends Map<? super Foo, ? extends Bar>>`
      // is not straightforward.
      final var patternType = elementUtils.getTypeElement(pattern.erasure().toString()).asType();
      final var enumType = typeUtils.erasure(elementUtils.getTypeElement("java.lang.Enum").asType());
      if (!typeUtils.isSubtype(patternType, enumType)) return Optional.empty();
    }

    // Satisfy the subgoals of the rule
    final var dependencies = new ArrayList<CodeBlock>(rule.parameters.size());
    for (final var parameter : rule.parameters) {
      final var codeBlock = applyRules(parameter.substitute(typeMapping));
      if (codeBlock.isEmpty()) return Optional.empty();
      dependencies.add(codeBlock.get());
    }

    // Build a codeblock satisfying the goal of this rule
    final var builder = CodeBlock.builder();
    builder.add("$T.$L(", rule.factory, rule.method);
    if (dependencies.size() > 0) {
      final var iter = dependencies.iterator();
      builder.add("\n$>$>$L$<$<", iter.next());
      while (iter.hasNext()) {
        builder.add(",\n$>$>$L$<$<", iter.next());
      }
    }
    builder.add(")");

    if (goal instanceof ClassPattern g && g.name.simpleName().equals("ValueMapper")) {
      final Map<String, AnnotationMirror> annotations = g.arguments.get(0).annotations;
      // TODO use these annotations
      return Optional.of(CodeBlock.of("new $T<>($S, $L)", LabeledValueMapper.class, "placeholder_label", builder.build()));
    }
    return Optional.of(builder.build());
  }
}
