package gov.nasa.jpl.aerie.merlin.processor;

import com.squareup.javapoet.CodeBlock;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Objects;

public final class Resolver {
  private final Types typeUtils;
  private final Elements elementUtils;

  public Resolver(final Types typeUtils, final Elements elementUtils) {
    this.typeUtils = Objects.requireNonNull(typeUtils);
    this.elementUtils = Objects.requireNonNull(elementUtils);
  }

  public CodeBlock instantiateMapperFor(final TypeMirror mirror) {
    // TODO: Do away with this null-checking stuff, somehow.
    if (mirror.getKind() == TypeKind.DECLARED || mirror.getKind() == TypeKind.ARRAY) {
      return CodeBlock.of(
          "new $T<>(\n$L)",
          gov.nasa.jpl.aerie.contrib.serialization.mappers.NullableValueMapper.class,
          getMapperFor(mirror));
    } else {
      return getMapperFor(mirror);
    }
  }

  private CodeBlock getMapperFor(final TypeMirror mirror) {
    final Class<?> handler;
    final String method;
    final List<CodeBlock> args;

    switch (mirror.getKind()) {
      // Primitives.
      case BOOLEAN:
        handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
        method = "$boolean";
        args = List.of();
        break;
      case BYTE:
        handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
        method = "$byte";
        args = List.of();
        break;
      case SHORT:
        handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
        method = "$short";
        args = List.of();
        break;
      case INT:
        handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
        method = "$int";
        args = List.of();
        break;
      case LONG:
        handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
        method = "$long";
        args = List.of();
        break;
      case CHAR:
        handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
        method = "$char";
        args = List.of();
        break;
      case FLOAT:
        handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
        method = "$float";
        args = List.of();
        break;
      case DOUBLE:
        handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
        method = "$double";
        args = List.of();
        break;

      // Arrays require a little lookahead to handle arrays of primitive type.
      case ARRAY: {
        final var t = ((ArrayType) mirror).getComponentType();
        switch (t.getKind()) {
          case BOOLEAN:
            handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
            method = "booleanArray";
            args = List.of();
            break;
          case BYTE:
            handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
            method = "byteArray";
            args = List.of();
            break;
          case SHORT:
            handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
            method = "shortArray";
            args = List.of();
            break;
          case INT:
            handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
            method = "intArray";
            args = List.of();
            break;
          case LONG:
            handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
            method = "longArray";
            args = List.of();
            break;
          case CHAR:
            handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
            method = "charArray";
            args = List.of();
            break;
          case FLOAT:
            handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
            method = "floatArray";
            args = List.of();
            break;
          case DOUBLE:
            handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
            method = "doubleArray";
            args = List.of();
            break;

          // If it's not a primitive, recurse normally.
          default: {
            handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
            method = "array";
            args = List.of(
                (this.typeUtils.isSameType(t, this.typeUtils.erasure(t)))
                    ? CodeBlock.of("$T.class", t)
                    : CodeBlock.of("(Class<$T>) (Object) $T.class", t, this.typeUtils.erasure(t)),
                getMapperFor(t));
          }
          break;
        }
      }
      break;

      // Reference types.
      case DECLARED: {
        final var mirrorErasure = this.typeUtils.erasure(mirror);

        if (this.typeUtils.isSameType(mirrorErasure, erasureOf(Boolean.class))) {
          handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
          method = "$boolean";
          args = List.of();
        } else if (this.typeUtils.isSameType(mirrorErasure, erasureOf(Byte.class))) {
          handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
          method = "$byte";
          args = List.of();
        } else if (this.typeUtils.isSameType(mirrorErasure, erasureOf(Short.class))) {
          handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
          method = "$short";
          args = List.of();
        } else if (this.typeUtils.isSameType(mirrorErasure, erasureOf(Integer.class))) {
          handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
          method = "$int";
          args = List.of();
        } else if (this.typeUtils.isSameType(mirrorErasure, erasureOf(Long.class))) {
          handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
          method = "$long";
          args = List.of();
        } else if (this.typeUtils.isSameType(mirrorErasure, erasureOf(Character.class))) {
          handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
          method = "$char";
          args = List.of();
        } else if (this.typeUtils.isSameType(mirrorErasure, erasureOf(Float.class))) {
          handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
          method = "$float";
          args = List.of();
        } else if (this.typeUtils.isSameType(mirrorErasure, erasureOf(Double.class))) {
          handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
          method = "$double";
          args = List.of();
        } else if (this.typeUtils.isSameType(mirrorErasure, erasureOf(String.class))) {
          handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
          method = "string";
          args = List.of();
        } else if (this.typeUtils.isSameType(mirrorErasure, erasureOf(gov.nasa.jpl.aerie.time.Duration.class))) {
          handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
          method = "duration";
          args = List.of();
        } else if (this.typeUtils.isSubtype(mirrorErasure, erasureOf(Enum.class))) {
          handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
          method = "$enum";
          args = List.of(
              CodeBlock.of("$T.class", mirrorErasure));
        } else if (this.typeUtils.isSameType(mirrorErasure, erasureOf(List.class))) {
          final var typeArguments = ((DeclaredType) mirror).getTypeArguments();

          handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
          method = "list";
          args = List.of(
              getMapperFor(typeArguments.get(0)));
        } else if (this.typeUtils.isSameType(mirrorErasure, erasureOf(java.util.Map.class))) {
          final var typeArguments = ((DeclaredType) mirror).getTypeArguments();

          handler = gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.class;
          method = "map";
          args = List.of(
              getMapperFor(typeArguments.get(0)),
              getMapperFor(typeArguments.get(1)));
        } else {
          throw new Error("Cannot construct a mapper for type " + mirror);
        }
      }
      break;

      default:
        throw new Error("Cannot construct a mapper for type " + mirror);
    }

    final var builder = CodeBlock.builder();
    builder.add("$T.$L(", handler, method);
    if (args.size() > 0) {
      final var iter = args.iterator();
      builder.add("\n$>$>$L$<$<", iter.next());
      while (iter.hasNext()) {
        builder.add(",\n$>$>$L$<$<", iter.next());
      }
    }
    builder.add(")");

    return builder.build();
  }

  private TypeMirror erasureOf(final Class<?> x) {
    return this.typeUtils.erasure(
        this.elementUtils
            .getTypeElement(x.getCanonicalName())
            .asType());
  }
}
