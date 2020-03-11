package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

import com.squareup.javapoet.CodeBlock;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.BooleanParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ByteParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.CharacterParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.FloatParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.IntegerParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ListParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.LongParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.MapParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ShortParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.StringParameterMapper;

import java.util.List;
import java.util.Map;

final class MapType implements ParameterTypeReference {
  private final ParameterTypeReference keyType;
  private final ParameterTypeReference elementType;

  public MapType(final ParameterTypeReference keyType, final ParameterTypeReference elementType) {
    this.keyType = keyType;
    this.elementType = elementType;
  }

  @Override
  public CodeBlock getParameterSchema() {
    return CodeBlock.builder()
        .add("$T.ofMap($L, $L)", ParameterSchema.class, this.keyType.getParameterSchema(), this.elementType.getParameterSchema())
        .build();
  }

  @Override
  public CodeBlock getMapper() {
    return CodeBlock.builder()
        .add("new $T<>($L, $L)", MapParameterMapper.class, this.keyType.getMapper(), this.elementType.getMapper())
        .build();
  }

  @Override
  public CodeBlock getType() {
    return CodeBlock.builder()
        .add("$T<$L, $L>", Map.class, this.keyType.getType(), this.elementType.getType())
        .build();
  }
}

final class ListType implements ParameterTypeReference {
  private final ParameterTypeReference elementType;

  public ListType(final ParameterTypeReference elementType) {
    this.elementType = elementType;
  }

  @Override
  public CodeBlock getParameterSchema() {
    return CodeBlock.builder()
        .add("$T.ofList($L)", ParameterSchema.class, this.elementType.getParameterSchema())
        .build();
  }

  @Override
  public CodeBlock getMapper() {
    return CodeBlock.builder()
        .add("new $T<>($L)", ListParameterMapper.class, this.elementType.getMapper())
        .build();
  }

  @Override
  public CodeBlock getType() {
    return CodeBlock.builder()
        .add("$T<$L>", List.class, this.elementType.getType())
        .build();
  }
}

final class NullaryType<ParameterType> implements ParameterTypeReference {
  private final ParameterSchema schema;
  private final Class<? extends ParameterMapper<ParameterType>> mapperClass;
  private final Class<? extends ParameterType> parameterClass;

  public NullaryType(
      final ParameterSchema schema,
      final Class<? extends ParameterMapper<ParameterType>> mapperClass,
      final Class<? extends ParameterType> parameterClass
  ) {
    this.schema = schema;
    this.mapperClass = mapperClass;
    this.parameterClass = parameterClass;
  }

  @Override
  public CodeBlock getParameterSchema() {
    final var literal = this.schema.match(new ParameterSchema.Visitor<>() {
      @Override
      public String onReal() { return "REAL"; }

      @Override
      public String onInt() { return "INT"; }

      @Override
      public String onBoolean() { return "BOOLEAN"; }

      @Override
      public String onString() { return "STRING"; }

      @Override
      public String onList(final ParameterSchema value) {
        throw new Error("Unexpectedly got a List in the NullaryType.");
      }

      @Override
      public String onMap(final Map<String, ParameterSchema> value) {
        throw new Error("Unexpectedly got a Map in the NullaryType");
      }
    });

    return CodeBlock.builder().add("$T.$L", ParameterSchema.class, literal).build();
  }

  @Override
  public CodeBlock getMapper() {
    return CodeBlock.builder().add("new $T()", this.mapperClass).build();
  }

  @Override
  public CodeBlock getType() {
    return CodeBlock.builder().add("$T", this.parameterClass).build();
  }
}

interface ParameterTypeReference {
  CodeBlock getParameterSchema();
  CodeBlock getMapper();
  CodeBlock getType();

  ParameterTypeReference BYTE = new NullaryType<>(ParameterSchema.INT, ByteParameterMapper.class, Byte.class);
  ParameterTypeReference SHORT = new NullaryType<>(ParameterSchema.INT, ShortParameterMapper.class, Short.class);
  ParameterTypeReference INT = new NullaryType<>(ParameterSchema.INT, IntegerParameterMapper.class, Integer.class);
  ParameterTypeReference LONG = new NullaryType<>(ParameterSchema.INT, LongParameterMapper.class, Long.class);
  ParameterTypeReference FLOAT = new NullaryType<>(ParameterSchema.REAL, FloatParameterMapper.class, Float.class);
  ParameterTypeReference DOUBLE = new NullaryType<>(ParameterSchema.REAL, DoubleParameterMapper.class, Double.class);
  ParameterTypeReference CHAR = new NullaryType<>(ParameterSchema.STRING, CharacterParameterMapper.class, Character.class);
  ParameterTypeReference STRING = new NullaryType<>(ParameterSchema.STRING, StringParameterMapper.class, String.class);
  ParameterTypeReference BOOLEAN = new NullaryType<>(ParameterSchema.BOOLEAN, BooleanParameterMapper.class, Boolean.class);

  static ParameterTypeReference ofList(final ParameterTypeReference elementType) {
    return new ListType(elementType);
  }

  static ParameterTypeReference ofMap(final ParameterTypeReference keyType, final ParameterTypeReference elementType) {
    return new MapType(keyType, elementType);
  }
}
