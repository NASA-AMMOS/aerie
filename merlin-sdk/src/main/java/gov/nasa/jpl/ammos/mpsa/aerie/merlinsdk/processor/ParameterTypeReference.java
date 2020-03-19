package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ArrayParameterMapper;
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
  public ParameterizedTypeName getTypeName() {
    return ParameterizedTypeName.get(ClassName.get(Map.class), keyType.getTypeName(), elementType.getTypeName());
  }

  @Override
  public TypeName getRawTypeName() {
    return this.getTypeName().rawType;
  }

  @Override
  public CodeBlock getMapper() {
    return CodeBlock.builder()
        .add("new $T<>($L, $L)", MapParameterMapper.class, this.keyType.getMapper(), this.elementType.getMapper())
        .build();
  }
}

final class ListType implements ParameterTypeReference {
  private final ParameterTypeReference elementType;

  public ListType(final ParameterTypeReference elementType) {
    this.elementType = elementType;
  }

  @Override
  public ParameterizedTypeName getTypeName() {
    return ParameterizedTypeName.get(ClassName.get(List.class), elementType.getTypeName());
  }

  @Override
  public TypeName getRawTypeName() {
    return this.getTypeName().rawType;
  }

  @Override
  public CodeBlock getMapper() {
    return CodeBlock.builder()
        .add("new $T<>($L)", ListParameterMapper.class, this.elementType.getMapper())
        .build();
  }
}

final class ArrayType implements ParameterTypeReference {
  private final ParameterTypeReference elementType;

  public ArrayType(final ParameterTypeReference elementType) {
    this.elementType = elementType;
  }

  @Override
  public TypeName getTypeName() {
    return ArrayTypeName.of(elementType.getTypeName());
  }

  @Override
  public TypeName getRawTypeName() {
    return ArrayTypeName.of(elementType.getRawTypeName());
  }

  @Override
  public CodeBlock getMapper() {
    return CodeBlock.builder()
        .add("new $T<>($L, $T.class)", ArrayParameterMapper.class, this.elementType.getMapper(), this.elementType.getRawTypeName())
        .build();
  }
}

final class NullaryType<ParameterType> implements ParameterTypeReference {
  private final Class<? extends ParameterMapper<ParameterType>> mapperClass;
  private final Class<? extends ParameterType> parameterClass;

  public NullaryType(
      final Class<? extends ParameterMapper<ParameterType>> mapperClass,
      final Class<? extends ParameterType> parameterClass
  ) {
    this.mapperClass = mapperClass;
    this.parameterClass = parameterClass;
  }

  @Override
  public TypeName getTypeName() {
    return ClassName.get(this.parameterClass);
  }

  @Override
  public TypeName getRawTypeName() {
    return this.getTypeName();
  }

  @Override
  public CodeBlock getMapper() {
    return CodeBlock.builder().add("new $T()", this.mapperClass).build();
  }
}

interface ParameterTypeReference {
  TypeName getTypeName();
  TypeName getRawTypeName();
  CodeBlock getMapper();

  ParameterTypeReference BYTE = new NullaryType<>(ByteParameterMapper.class, Byte.class);
  ParameterTypeReference SHORT = new NullaryType<>(ShortParameterMapper.class, Short.class);
  ParameterTypeReference INT = new NullaryType<>(IntegerParameterMapper.class, Integer.class);
  ParameterTypeReference LONG = new NullaryType<>(LongParameterMapper.class, Long.class);
  ParameterTypeReference FLOAT = new NullaryType<>(FloatParameterMapper.class, Float.class);
  ParameterTypeReference DOUBLE = new NullaryType<>(DoubleParameterMapper.class, Double.class);
  ParameterTypeReference CHAR = new NullaryType<>(CharacterParameterMapper.class, Character.class);
  ParameterTypeReference STRING = new NullaryType<>(StringParameterMapper.class, String.class);
  ParameterTypeReference BOOLEAN = new NullaryType<>(BooleanParameterMapper.class, Boolean.class);

  static ParameterTypeReference ofArray(final ParameterTypeReference elementType) {
    return new ArrayType(elementType);
  }

  static ParameterTypeReference ofList(final ParameterTypeReference elementType) {
    return new ListType(elementType);
  }

  static ParameterTypeReference ofMap(final ParameterTypeReference keyType, final ParameterTypeReference elementType) {
    return new MapType(keyType, elementType);
  }
}
