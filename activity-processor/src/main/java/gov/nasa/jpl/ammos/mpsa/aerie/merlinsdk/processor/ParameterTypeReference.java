package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.BooleanValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ByteValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.CharacterValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.FloatValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.IntegerValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ListValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.LongValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.MapValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.NullableValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveBooleanArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveByteArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveCharArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveFloatArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveIntArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ShortValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.StringValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.EnumValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveDoubleArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveShortArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveLongArrayValueMapper;


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
        .add("new $T<>($L, $L)", MapValueMapper.class, this.keyType.getMapper(), this.elementType.getMapper())
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
        .add("new $T<>($L)", ListValueMapper.class, this.elementType.getMapper())
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
        .add("new $T<>($L, $T.class)", ArrayValueMapper.class, this.elementType.getMapper(), this.elementType.getRawTypeName())
        .build();
  }
}

final class PrimitiveArrayType<T> implements ParameterTypeReference {
  private Class<? extends ValueMapper<T>> primitiveArrayMapper;
  private Class<? extends T> elementType;

  public PrimitiveArrayType(Class<? extends ValueMapper<T>> primitiveArrayMapper, Class<? extends T> elementType) {
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
            .add("new $T<>($T.class)", EnumValueMapper.class, this.enumType)
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
        .add("new $T<>($L)", NullableValueMapper.class, this.valueType.getMapper())
        .build();
  }
}

final class NullaryType<ParameterType> implements ParameterTypeReference {
  private final Class<? extends ValueMapper<ParameterType>> mapperClass;
  private final Class<? extends ParameterType> parameterClass;

  public NullaryType(
      final Class<? extends ValueMapper<ParameterType>> mapperClass,
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

  ParameterTypeReference BYTE = new NullaryType<>(ByteValueMapper.class, Byte.class);
  ParameterTypeReference SHORT = new NullaryType<>(ShortValueMapper.class, Short.class);
  ParameterTypeReference INT = new NullaryType<>(IntegerValueMapper.class, Integer.class);
  ParameterTypeReference LONG = new NullaryType<>(LongValueMapper.class, Long.class);
  ParameterTypeReference FLOAT = new NullaryType<>(FloatValueMapper.class, Float.class);
  ParameterTypeReference DOUBLE = new NullaryType<>(DoubleValueMapper.class, Double.class);
  ParameterTypeReference CHAR = new NullaryType<>(CharacterValueMapper.class, Character.class);
  ParameterTypeReference STRING = new NullaryType<>(StringValueMapper.class, String.class);
  ParameterTypeReference BOOLEAN = new NullaryType<>(BooleanValueMapper.class, Boolean.class);
  ParameterTypeReference PRIM_DOUBLE_ARRAY = new PrimitiveArrayType<>(PrimitiveDoubleArrayValueMapper.class, double[].class);
  ParameterTypeReference PRIM_FLOAT_ARRAY = new PrimitiveArrayType<>(PrimitiveFloatArrayValueMapper.class, float[].class);
  ParameterTypeReference PRIM_BYTE_ARRAY = new PrimitiveArrayType<>(PrimitiveByteArrayValueMapper.class, byte[].class);
  ParameterTypeReference PRIM_SHORT_ARRAY = new PrimitiveArrayType<>(PrimitiveShortArrayValueMapper.class, short[].class);
  ParameterTypeReference PRIM_INT_ARRAY = new PrimitiveArrayType<>(PrimitiveIntArrayValueMapper.class, int[].class);
  ParameterTypeReference PRIM_LONG_ARRAY = new PrimitiveArrayType<>(PrimitiveLongArrayValueMapper.class, long[].class);
  ParameterTypeReference PRIM_CHAR_ARRAY = new PrimitiveArrayType<>(PrimitiveCharArrayValueMapper.class, char[].class);
  ParameterTypeReference PRIM_BOOLEAN_ARRAY = new PrimitiveArrayType<>(PrimitiveBooleanArrayValueMapper.class, boolean[].class);


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
