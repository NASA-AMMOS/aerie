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
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.NullableParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ShortParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.StringParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.EnumParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveDoubleArrayParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveFloatArrayParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveByteArrayParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveShortArrayParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveIntArrayParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveLongArrayParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveCharArrayParameterMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveBooleanArrayParameterMapper;


import javax.lang.model.type.TypeMirror;
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

final class PrimitiveArrayType<T> implements ParameterTypeReference {
  private Class<? extends ParameterMapper<T>> primitiveArrayMapper;
  private Class<? extends T> elementType;

  public PrimitiveArrayType(Class<? extends ParameterMapper<T>> primitiveArrayMapper, Class<? extends T> elementType) {
    this.elementType = elementType;
    this.primitiveArrayMapper = primitiveArrayMapper;
  }

  @Override
  public TypeName getTypeName() {
    return TypeName.get(this.elementType);
  }

  @Override
  public TypeName getRawTypeName() {
    return getTypeName();
  }

  @Override
  public CodeBlock getMapper() {
    return CodeBlock.builder()
            .add("new $T()", this.primitiveArrayMapper)
            .build();
  }
}

final class EnumType implements ParameterTypeReference {
  private final TypeMirror enumType;

  public EnumType(final TypeMirror enumType) {
    this.enumType = enumType;
  }

  @Override
  public TypeName getTypeName() {
    return TypeName.get(this.enumType);
  }

  @Override
  public TypeName getRawTypeName() {
    return this.getTypeName();
  }

  @Override
  public CodeBlock getMapper() {
    return CodeBlock.builder()
            .add("new $T<>($T.class)", EnumParameterMapper.class, this.enumType)
            .build();
  }
}

final class NullableType implements ParameterTypeReference {
  private final ParameterTypeReference valueType;

  public NullableType(final ParameterTypeReference valueType) {
    this.valueType = valueType;
  }

  @Override
  public TypeName getTypeName() {
    return valueType.getTypeName();
  }

  @Override
  public TypeName getRawTypeName() {
    return valueType.getRawTypeName();
  }

  @Override
  public CodeBlock getMapper() {
    return CodeBlock.builder()
        .add("new $T<>($L)", NullableParameterMapper.class, this.valueType.getMapper())
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
  ParameterTypeReference PRIM_DOUBLE_ARRAY = new PrimitiveArrayType<>(PrimitiveDoubleArrayParameterMapper.class, double[].class);
  ParameterTypeReference PRIM_FLOAT_ARRAY = new PrimitiveArrayType<>(PrimitiveFloatArrayParameterMapper.class, float[].class);
  ParameterTypeReference PRIM_BYTE_ARRAY = new PrimitiveArrayType<>(PrimitiveByteArrayParameterMapper.class, byte[].class);
  ParameterTypeReference PRIM_SHORT_ARRAY = new PrimitiveArrayType<>(PrimitiveShortArrayParameterMapper.class, short[].class);
  ParameterTypeReference PRIM_INT_ARRAY = new PrimitiveArrayType<>(PrimitiveIntArrayParameterMapper.class, int[].class);
  ParameterTypeReference PRIM_LONG_ARRAY = new PrimitiveArrayType<>(PrimitiveLongArrayParameterMapper.class, long[].class);
  ParameterTypeReference PRIM_CHAR_ARRAY = new PrimitiveArrayType<>(PrimitiveCharArrayParameterMapper.class, char[].class);
  ParameterTypeReference PRIM_BOOLEAN_ARRAY = new PrimitiveArrayType<>(PrimitiveBooleanArrayParameterMapper.class, boolean[].class);


  static ParameterTypeReference nullable(final ParameterTypeReference valueType) {
    return new NullableType(valueType);
  }

  static ParameterTypeReference ofArray(final ParameterTypeReference elementType) {
    return new ArrayType(elementType);
  }

  static ParameterTypeReference ofList(final ParameterTypeReference elementType) {
    return new ListType(elementType);
  }

  static ParameterTypeReference ofMap(final ParameterTypeReference keyType, final ParameterTypeReference elementType) {
    return new MapType(keyType, elementType);
  }

  static ParameterTypeReference ofEnum(final TypeMirror enumType) {
    return new EnumType(enumType);
  }
}
