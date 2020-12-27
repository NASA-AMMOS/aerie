package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.processor;

import com.squareup.javapoet.ClassName;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class TypePattern {
  private TypePattern() {}

  public static final class TypeVariablePattern extends TypePattern {
    public final String name;

    public TypeVariablePattern(final String name) {
      this.name = Objects.requireNonNull(name);
    }

    @Override
    public String toString() {
      return "$" + this.name;
    }
  }

  public static final class PrimitivePattern extends TypePattern {
    public final Primitive primitive;

    public PrimitivePattern(final Primitive primitive) {
      this.primitive = Objects.requireNonNull(primitive);
    }

    @Override
    public String toString() {
      return this.primitive.toString();
    }
  }

  public enum Primitive {
    BOOLEAN("boolean"),
    BYTE("byte"),
    SHORT("short"),
    INT("int"),
    LONG("long"),
    CHAR("char"),
    FLOAT("float"),
    DOUBLE("double");

    private final String repr;

    Primitive(final String repr) {
      this.repr = repr;
    }

    @Override
    public String toString() {
      return this.repr;
    }
  }

  public static final class ArrayPattern extends TypePattern {
    public final TypePattern element;

    public ArrayPattern(final TypePattern element) {
      this.element = Objects.requireNonNull(element);
    }

    @Override
    public String toString() {
      return this.element.toString() + "[]";
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
    public String toString() {
      final var builder = new StringBuilder();
      builder.append(this.name);
      if (this.arguments.size() > 0) {
        builder.append("<");
        final var iter = this.arguments.iterator();
        builder.append(iter.next());
        while (iter.hasNext()) {
          builder.append(", ");
          builder.append(iter.next());
        }
        builder.append(">");
      }
      return builder.toString();
    }
  }

  public static void main(final String[] args) {
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

    final var unifier = Map.of(
        elementPattern, new ClassPattern(ClassName.get(Map.class), List.of(
            new ArrayPattern(
                new ArrayPattern(
                    new ClassPattern(ClassName.get(String.class), List.of()))),
            new ClassPattern(ClassName.get(Map.class), List.of(
                new ClassPattern(ClassName.get(Integer.class), List.of()),
                new ClassPattern(ClassName.get(List.class), List.of(
                    new ArrayPattern(
                        new PrimitivePattern(Primitive.FLOAT)))))))));

    // TODO: implement match/2
    // TODO: implement substitute/3

    System.out.println(goal);
    System.out.println(head);
    System.out.println(unifier);
  }
}
