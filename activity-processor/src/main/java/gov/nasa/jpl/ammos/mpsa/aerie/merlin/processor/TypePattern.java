package gov.nasa.jpl.ammos.mpsa.aerie.merlin.processor;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public abstract class TypePattern {
  private TypePattern() {}

  public abstract TypePattern substitute(Map<String, TypePattern> substitution);

  public abstract Map<String, TypePattern> match(TypePattern other) throws UnificationException;

  public abstract boolean isGround();

  public abstract boolean isSyntacticallyEqualTo(TypePattern other);

  public abstract TypeName render();

  @Override
  public String toString() {
    return this.render().toString();
  }

  private static class UnificationException extends Exception {}


  public static final class TypeVariablePattern extends TypePattern {
    public final String name;

    public TypeVariablePattern(final String name) {
      this.name = Objects.requireNonNull(name);
    }

    @Override
    public TypePattern substitute(final Map<String, TypePattern> substitution) {
      return substitution.getOrDefault(this.name, this);
    }

    @Override
    public Map<String, TypePattern> match(final TypePattern other) {
      assert other.isGround();

      return Map.of(this.name, other);
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
  }

  public static final class PrimitivePattern extends TypePattern {
    public final Primitive primitive;

    public PrimitivePattern(final Primitive primitive) {
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

    public ArrayPattern(final TypePattern element) {
      this.element = Objects.requireNonNull(element);
    }

    @Override
    public ArrayPattern substitute(final Map<String, TypePattern> substitution) {
      final var child = this.element.substitute(substitution);
      if (child == this.element) return this;

      return new ArrayPattern(child);
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
  }

  public static final class ClassPattern extends TypePattern {
    public final ClassName name;
    public final List<TypePattern> arguments;

    public ClassPattern(final ClassName name, final List<TypePattern> arguments) {
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

      return new ClassPattern(this.name, substitutedArguments);
    }

    @Override
    public Map<String, TypePattern> match(final TypePattern o) throws UnificationException {
      if (!(o instanceof ClassPattern)) throw new UnificationException();
      final var other = (ClassPattern) o;

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
        argumentTypeNames[i] = this.arguments.get(i).render();
      }

      return ParameterizedTypeName.get(this.name, argumentTypeNames);
    }
  }

  public static void main(final String[] args) throws UnificationException {
    final var rules = new ArrayList<Triple<
        TypePattern,
        List<TypePattern>,
        BiFunction<List<CodeBlock>, Map<String, CodeBlock>, CodeBlock>>>();

    rules.add(Triple.of(
        new ClassPattern(ClassName.get(Map.class), List.of(
            new TypeVariablePattern("K"),
            new TypeVariablePattern("V")
        )),
        List.of(
            new TypeVariablePattern("K"),
            new TypeVariablePattern("V")),
        (deps, classes) -> CodeBlock.of(
            "$T.map($L, $L)",
            gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.BasicValueMappers.class,
            deps.get(0),
            deps.get(1))));

    rules.add(Triple.of(
        new ClassPattern(ClassName.get(List.class), List.of(
            new TypeVariablePattern("T"))),
        List.of(
            new TypeVariablePattern("T")),
        (deps, classes) -> CodeBlock.of(
            "$T.list($L)",
            gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.BasicValueMappers.class,
            deps.get(0))));

//    // Enums; need to add the ability to check that E is a subtype of Enum.
//    rules.add(Triple.of(
//        new TypeVariablePattern("E"),
//        List.of(),
//        (deps, classes) -> CodeBlock.of(
//          "$T.$enum($L)",
//          classes.get("E")));

    rules.add(Triple.of(
        new ArrayPattern(
            new TypeVariablePattern("T")),
        List.of(
            new TypeVariablePattern("T")),
        (deps, classes) -> CodeBlock.of(
            "$T.array($L, $L)",
            gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.BasicValueMappers.class,
            classes.get("T"),
            deps.get(0))));

    rules.add(Triple.of(
        new ClassPattern(ClassName.get(String.class), List.of()),
        List.of(),
        (deps, classes) -> CodeBlock.of(
            "$T.string()",
            gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.BasicValueMappers.class)));

    rules.add(Triple.of(
        new ClassPattern(ClassName.get(Integer.class), List.of()),
        List.of(),
        (deps, classes) -> CodeBlock.of(
            "$T.$int()",
            gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.PrimitiveValueMappers.class)));

    rules.add(Triple.of(
        new PrimitivePattern(Primitive.FLOAT),
        List.of(),
        (deps, classes) -> CodeBlock.of(
            "$T.$float()",
            gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.PrimitiveValueMappers.class)));

    System.out.println(rules);


    // List<Map<String[][], Map<Integer, List<float[]>>>>
    final var goal =
        new ClassPattern(ClassName.get(List.class), List.of(
            new ClassPattern(ClassName.get(java.util.Map.class), List.of(
                new ArrayPattern(
                    new ArrayPattern(
                        new ClassPattern(ClassName.get(String.class), List.of()))),
                new ClassPattern(ClassName.get(java.util.Map.class), List.of(
                    new ClassPattern(ClassName.get(Integer.class), List.of()),
                    new ClassPattern(ClassName.get(List.class), List.of(
                        new ArrayPattern(
                            new PrimitivePattern(Primitive.FLOAT))))))))));

    final var elementPattern = new TypeVariablePattern("T");
    final var head =
        new ClassPattern(ClassName.get(List.class), List.of(
            elementPattern));

    final var unifier = Map.<String, TypePattern>of(
        elementPattern.name, (
            new ClassPattern(ClassName.get(Map.class), List.of(
                new ArrayPattern(
                    new ArrayPattern(
                        new ClassPattern(ClassName.get(String.class), List.of()))),
                new ClassPattern(ClassName.get(Map.class), List.of(
                    new ClassPattern(ClassName.get(Integer.class), List.of()),
                    new ClassPattern(ClassName.get(List.class), List.of(
                        new ArrayPattern(
                            new PrimitivePattern(Primitive.FLOAT))))))))));

    System.out.println(head);

    System.out.println(unifier);
    System.out.println(head.match(goal));

    System.out.println(goal);
    System.out.println(head.substitute(unifier));
  }
}
