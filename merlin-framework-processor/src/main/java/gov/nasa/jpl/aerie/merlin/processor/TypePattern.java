package gov.nasa.jpl.aerie.merlin.processor;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Unit;
import gov.nasa.jpl.aerie.merlin.processor.generator.MissionModelGenerator;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class TypePattern {
  public final Optional<String> unit;

  private TypePattern(final Optional<String> unit) {
    this.unit = unit;
  }

  public static TypePattern from(final Elements elementUtils, final Types typeUtils, final TypeMirror mirror) {
    final var unit = getAnnotationMirrorByType(
        elementUtils,
        typeUtils,
        mirror,
        Unit.class)
        .flatMap($ -> getAnnotationAttribute($, "value"))
        .map($ -> (String) $.getValue());
    final TypePattern result;
    switch (mirror.getKind()) {
      case BOOLEAN -> result = new PrimitivePattern(Primitive.BOOLEAN, unit);
      case BYTE -> result = new PrimitivePattern(Primitive.BYTE, unit);
      case SHORT -> result = new PrimitivePattern(Primitive.SHORT, unit);
      case INT -> result = new PrimitivePattern(Primitive.INT, unit);
      case LONG -> result = new PrimitivePattern(Primitive.LONG, unit);
      case CHAR -> result = new PrimitivePattern(Primitive.CHAR, unit);
      case FLOAT -> result = new PrimitivePattern(Primitive.FLOAT, unit);
      case DOUBLE -> result = new PrimitivePattern(Primitive.DOUBLE, unit);
      case ARRAY -> result = new ArrayPattern(TypePattern.from(elementUtils, typeUtils, ((ArrayType) mirror).getComponentType()), unit);
      case TYPEVAR -> result = new TypeVariablePattern(mirror.toString());
      case DECLARED -> {
        // DeclaredType element can be cast as TypeElement because it's a Type
        final var className = ClassName.get((TypeElement) ((DeclaredType) mirror).asElement());
        final var typeArguments = ((DeclaredType) mirror).getTypeArguments();
        final var argumentPatterns = new ArrayList<TypePattern>(typeArguments.size());
        for (final var typeArgument : typeArguments) {
          argumentPatterns.add(TypePattern.from(elementUtils, typeUtils, typeArgument));
        }
        result = new ClassPattern(className, argumentPatterns, unit);
      }

      default -> throw new Error("Cannot construct a pattern for type " + mirror);
    }
    return result;
  }

  public static TypePattern from(final Elements elementUtils, final Types typeUtils, final VariableElement element) {
    return from(elementUtils, typeUtils, element.asType());
  }

  public abstract TypePattern substitute(Map<String, TypePattern> substitution);

  public abstract Map<String, TypePattern> match(TypePattern other) throws UnificationException;

  public abstract boolean isGround();

  public abstract boolean isSyntacticallyEqualTo(TypePattern other);

  public abstract TypeName render();

  public abstract TypeName erasure();

  public abstract TypePattern box();

  public static class UnificationException extends Exception {}

  public static final class TypeVariablePattern extends TypePattern {
    public final String name;

    public TypeVariablePattern(final String name) {
      super(Optional.empty());
      this.name = Objects.requireNonNull(name);
    }

    @Override
    public TypePattern substitute(final Map<String, TypePattern> substitution) {
      return substitution.getOrDefault(this.name, this);
    }

    @Override
    public Map<String, TypePattern> match(final TypePattern other) {
      assert other.isGround();

      return Map.of(this.name, other); // TODO add unit information from...?
    }

    @Override
    public boolean isGround() {
      return false;
    }

    @Override
    public boolean isSyntacticallyEqualTo(final TypePattern o) {
      if (!(o instanceof TypeVariablePattern)) return false;
      final var other = (TypeVariablePattern) o;

      return this.name.equals(other.name);
    }

    @Override
    public TypeName render() {
      return TypeVariableName.get(this.name);
    }

    @Override
    public TypeName erasure() {
      return this.render();
    }

    @Override
    public TypePattern box() {
      return this;
    }

    @Override
    public String toString() {
      return this.render().toString() + this.unit.map($ -> " (" + $ + ")").orElse("");
    }
  }

  public static final class PrimitivePattern extends TypePattern {
    public final Primitive primitive;

    public PrimitivePattern(final Primitive primitive, final Optional<String> unit) {
      super(unit);
      this.primitive = Objects.requireNonNull(primitive);
    }

    @Override
    public PrimitivePattern substitute(final Map<String, TypePattern> substitution) {
      return this;
    }

    @Override
    public Map<String, TypePattern> match(final TypePattern other) throws UnificationException {
      if (!this.isSyntacticallyEqualTo(other)) throw new UnificationException();

      return Map.of();
    }

    @Override
    public boolean isGround() {
      return true;
    }

    @Override
    public boolean isSyntacticallyEqualTo(final TypePattern o) {
      if (!(o instanceof PrimitivePattern)) return false;
      final var other = (PrimitivePattern) o;

      return this.primitive.equals(other.primitive);
    }

    @Override
    public TypeName render() {
      return this.primitive.toTypeName();
    }

    @Override
    public TypeName erasure() {
      return this.render();
    }

    @Override
    public TypePattern box() {
      return new ClassPattern((ClassName) this.render().box(), List.of(), this.unit);
    }

    @Override
    public String toString() {
      return this.primitive.toString() + this.unit.map($ -> " (" + $ + ")").orElse("");
    }
  }

  public enum Primitive {
    BOOLEAN(TypeName.BOOLEAN),
    BYTE(TypeName.BYTE),
    SHORT(TypeName.SHORT),
    INT(TypeName.INT),
    LONG(TypeName.LONG),
    CHAR(TypeName.CHAR),
    FLOAT(TypeName.FLOAT),
    DOUBLE(TypeName.DOUBLE);

    private final TypeName repr;

    Primitive(final TypeName repr) {
      this.repr = repr;
    }

    public TypeName toTypeName() {
      return this.repr;
    }
  }

  public static final class ArrayPattern extends TypePattern {
    public final TypePattern element;

    public ArrayPattern(final TypePattern element, final Optional<String> unit) {
      super(unit);
      this.element = Objects.requireNonNull(element);
    }

    @Override
    public ArrayPattern substitute(final Map<String, TypePattern> substitution) {
      final var child = this.element.substitute(substitution);
      if (child == this.element) return this;

      return new ArrayPattern(child, this.unit);
    }

    @Override
    public Map<String, TypePattern> match(final TypePattern o) throws UnificationException {
      if (!(o instanceof ArrayPattern)) throw new UnificationException();
      final var other = (ArrayPattern) o;

      return this.element.match(other.element);
    }

    @Override
    public boolean isGround() {
      return this.element.isGround();
    }

    @Override
    public boolean isSyntacticallyEqualTo(final TypePattern o) {
      if (!(o instanceof ArrayPattern)) return false;
      final var other = (ArrayPattern) o;

      return this.element.isSyntacticallyEqualTo(other.element);
    }

    @Override
    public TypeName render() {
      return ArrayTypeName.of(this.element.render());
    }

    @Override
    public TypeName erasure() {
      return ArrayTypeName.of(this.element.erasure());
    }

    @Override
    public TypePattern box() {
      return this;
    }

    @Override
    public String toString() {
      return this.element.toString() + "[]"  + this.unit.map($ -> " (" + $ + ")").orElse("");
    }
  }

  public static final class ClassPattern extends TypePattern {
    public final ClassName name;
    public final List<TypePattern> arguments;

    public ClassPattern(final ClassName name, final List<TypePattern> arguments, final Optional<String> unit) {
      super(unit);
      this.name = Objects.requireNonNull(name);
      this.arguments = Objects.requireNonNull(arguments);
    }

    @Override
    public ClassPattern substitute(final Map<String, TypePattern> substitution) {
      final var substitutedArguments = new ArrayList<TypePattern>(this.arguments.size());
      for (final var argument : this.arguments) {
        substitutedArguments.add(argument.substitute(substitution));
      }

      // The `allUnchanged` block can be removed without impacting correctness,
      //   but it exists to avoid needless extra allocations whenever possible.
      var allUnchanged = true;
      for (var i = 0; i < this.arguments.size(); i += 1) {
        if (this.arguments.get(i) != substitutedArguments.get(i)) {
          allUnchanged = false;
          break;
        }
      }
      if (allUnchanged) return this;

      return new ClassPattern(this.name, substitutedArguments, this.unit);
    }

    @Override
    public Map<String, TypePattern> match(final TypePattern o) throws UnificationException {
      if (!(o instanceof ClassPattern)) throw new UnificationException();
      final var other = (ClassPattern) o;
      if (!this.name.equals(other.name)) throw new UnificationException();
      if (this.arguments.size() != other.arguments.size()) throw new UnificationException();

      // This specialization by arity can be removed without impacting correctness,
      //   but it exists to avoid needless extra work whenever possible.
      if (this.arguments.size() == 0) {
        return Map.of();
      } else if (this.arguments.size() == 1) {
        return this.arguments.get(0).match(other.arguments.get(0));
      }

      final var collectedSubstitution = new HashMap<String, TypePattern>();
      for (var i = 0; i < this.arguments.size(); i += 1) {
        final var substitution = this.arguments.get(i).match(other.arguments.get(i));

        for (final var entry : substitution.entrySet()) {
          if (!collectedSubstitution.containsKey(entry.getKey())) {
            collectedSubstitution.put(entry.getKey(), entry.getValue());
            continue;
          }

          if (collectedSubstitution.get(entry.getKey()).isSyntacticallyEqualTo(entry.getValue())) {
            continue;
          }

          throw new UnificationException();
        }
      }

      return collectedSubstitution;
    }

    @Override
    public boolean isGround() {
      return this.arguments.stream().allMatch(TypePattern::isGround);
    }

    @Override
    public boolean isSyntacticallyEqualTo(final TypePattern o) {
      if (!(o instanceof ClassPattern)) return false;
      final var other = (ClassPattern) o;

      if (this.arguments.size() != other.arguments.size()) return false;

      for (var i = 0; i < this.arguments.size(); i += 1) {
        if (!this.arguments.get(i).isSyntacticallyEqualTo(other.arguments.get(i))) {
          return false;
        }
      }

      return true;
    }

    @Override
    public TypeName render() {
      if (this.arguments.size() == 0) return this.name;

      final var argumentTypeNames = new TypeName[this.arguments.size()];
      for (var i = 0; i < argumentTypeNames.length; i += 1) {
        argumentTypeNames[i] = this.arguments.get(i).render().box();
      }

      return ParameterizedTypeName.get(this.name, argumentTypeNames);
    }

    @Override
    public TypeName erasure() {
      return this.name;
    }

    @Override
    public TypePattern box() {
      return this;
    }

    @Override
    public String toString() {
      if (this.arguments.size() == 0) {
        return this.name.simpleName() + this.unit.map($ -> " (" + $ + ")").orElse("");
      } else {
        var res = this.name.simpleName() + "<";
        for (var arg : this.arguments) {
          res = res + arg.toString();
        }
        return res + ">" + this.unit.map($ -> " (" + $ + ")").orElse("");
      }
    }
  }

  private static Optional<AnnotationMirror> getAnnotationMirrorByType(final Elements elementUtils, final Types typeUtils, final TypeMirror element, final Class<? extends Annotation> annotationClass)
  {
    final var annotationType = elementUtils
        .getTypeElement(annotationClass.getCanonicalName())
        .asType();

    for (final var x : element.getAnnotationMirrors()) {
      if (typeUtils.isSameType(annotationType, x.getAnnotationType())) {
        return Optional.of(x);
      }
    }

    return Optional.empty();
  }

  private static Optional<AnnotationValue> getAnnotationAttribute(final AnnotationMirror annotationMirror, final String attributeName)
  {
    for (final var entry : annotationMirror.getElementValues().entrySet()) {
      if (Objects.equals(attributeName, entry.getKey().getSimpleName().toString())) {
        return Optional.of(entry.getValue());
      }
    }

    return Optional.empty();
  }
}
