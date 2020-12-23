package gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.mappers;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.ParameterTestActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.BooleanValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ByteValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.CharacterValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.EnumValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.FloatValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.IntegerValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ListValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.LongValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.MapValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.NullableValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveBooleanArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveByteArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveCharArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveDoubleArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveFloatArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveIntArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveLongArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveShortArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ShortValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.StringValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParameterTestActivityMapper implements ActivityMapper<ParameterTestActivity> {
  private final ValueMapper<Double> mapper_primitiveDouble = new NullableValueMapper<>(new DoubleValueMapper());
  private final ValueMapper<Float> mapper_primitiveFloat = new NullableValueMapper<>(new FloatValueMapper());
  private final ValueMapper<Byte> mapper_primitiveByte = new NullableValueMapper<>(new ByteValueMapper());
  private final ValueMapper<Short> mapper_primitiveShort = new NullableValueMapper<>(new ShortValueMapper());
  private final ValueMapper<Integer> mapper_primitiveInt = new NullableValueMapper<>(new IntegerValueMapper());
  private final ValueMapper<Long> mapper_primitiveLong = new NullableValueMapper<>(new LongValueMapper());
  private final ValueMapper<Character> mapper_primitiveChar =
      new NullableValueMapper<>(new CharacterValueMapper());
  private final ValueMapper<Boolean> mapper_primitiveBoolean =
      new NullableValueMapper<>(new BooleanValueMapper());
  private final ValueMapper<Double> mapper_boxedDouble = new NullableValueMapper<>(new DoubleValueMapper());
  private final ValueMapper<Float> mapper_boxedFloat = new NullableValueMapper<>(new FloatValueMapper());
  private final ValueMapper<Byte> mapper_boxedByte = new NullableValueMapper<>(new ByteValueMapper());
  private final ValueMapper<Short> mapper_boxedShort = new NullableValueMapper<>(new ShortValueMapper());
  private final ValueMapper<Integer> mapper_boxedInt = new NullableValueMapper<>(new IntegerValueMapper());
  private final ValueMapper<Long> mapper_boxedLong = new NullableValueMapper<>(new LongValueMapper());
  private final ValueMapper<Character> mapper_boxedChar = new NullableValueMapper<>(new CharacterValueMapper());
  private final ValueMapper<Boolean> mapper_boxedBoolean = new NullableValueMapper<>(new BooleanValueMapper());
  private final ValueMapper<String> mapper_string = new NullableValueMapper<>(new StringValueMapper());
  private final ValueMapper<Double[]> mapper_doubleArray =
      new NullableValueMapper<>(new ArrayValueMapper<>(new DoubleValueMapper(), Double.class));
  private final ValueMapper<Float[]> mapper_floatArray =
      new NullableValueMapper<>(new ArrayValueMapper<>(new FloatValueMapper(), Float.class));
  private final ValueMapper<Byte[]> mapper_byteArray =
      new NullableValueMapper<>(new ArrayValueMapper<>(new ByteValueMapper(), Byte.class));
  private final ValueMapper<Short[]> mapper_shortArray =
      new NullableValueMapper<>(new ArrayValueMapper<>(new ShortValueMapper(), Short.class));
  private final ValueMapper<Integer[]> mapper_intArray =
      new NullableValueMapper<>(new ArrayValueMapper<>(new IntegerValueMapper(), Integer.class));
  private final ValueMapper<Long[]> mapper_longArray =
      new NullableValueMapper<>(new ArrayValueMapper<>(new LongValueMapper(), Long.class));
  private final ValueMapper<Character[]> mapper_charArray =
      new NullableValueMapper<>(new ArrayValueMapper<>(new CharacterValueMapper(), Character.class));
  private final ValueMapper<Boolean[]> mapper_booleanArray =
      new NullableValueMapper<>(new ArrayValueMapper<>(new BooleanValueMapper(), Boolean.class));
  private final ValueMapper<String[]> mapper_stringArray =
      new NullableValueMapper<>(new ArrayValueMapper<>(new StringValueMapper(), String.class));
  private final ValueMapper<double[]> mapper_primDoubleArray =
      new NullableValueMapper<>(new PrimitiveDoubleArrayValueMapper());
  private final ValueMapper<float[]> mapper_primFloatArray =
      new NullableValueMapper<>(new PrimitiveFloatArrayValueMapper());
  private final ValueMapper<byte[]> mapper_primByteArray =
      new NullableValueMapper<>(new PrimitiveByteArrayValueMapper());
  private final ValueMapper<short[]> mapper_primShortArray =
      new NullableValueMapper<>(new PrimitiveShortArrayValueMapper());
  private final ValueMapper<int[]> mapper_primIntArray =
      new NullableValueMapper<>(new PrimitiveIntArrayValueMapper());
  private final ValueMapper<long[]> mapper_primLongArray =
      new NullableValueMapper<>(new PrimitiveLongArrayValueMapper());
  private final ValueMapper<char[]> mapper_primCharArray =
      new NullableValueMapper<>(new PrimitiveCharArrayValueMapper());
  private final ValueMapper<boolean[]> mapper_primBooleanArray =
      new NullableValueMapper<>(new PrimitiveBooleanArrayValueMapper());
  private final ValueMapper<List<Double>> mapper_doubleList =
      new NullableValueMapper<>(new ListValueMapper<>(new DoubleValueMapper()));
  private final ValueMapper<List<Float>> mapper_floatList =
      new NullableValueMapper<>(new ListValueMapper<>(new FloatValueMapper()));
  private final ValueMapper<List<Byte>> mapper_byteList =
      new NullableValueMapper<>(new ListValueMapper<>(new ByteValueMapper()));
  private final ValueMapper<List<Short>> mapper_shortList =
      new NullableValueMapper<>(new ListValueMapper<>(new ShortValueMapper()));
  private final ValueMapper<List<Integer>> mapper_intList =
      new NullableValueMapper<>(new ListValueMapper<>(new IntegerValueMapper()));
  private final ValueMapper<List<Long>> mapper_longList =
      new NullableValueMapper<>(new ListValueMapper<>(new LongValueMapper()));
  private final ValueMapper<List<Character>> mapper_charList = new NullableValueMapper<>(new ListValueMapper<>(
      new CharacterValueMapper()));
  private final ValueMapper<List<Boolean>> mapper_booleanList = new NullableValueMapper<>(new ListValueMapper<>(
      new BooleanValueMapper()));
  private final ValueMapper<List<String>> mapper_stringList =
      new NullableValueMapper<>(new ListValueMapper<>(new StringValueMapper()));
  private final ValueMapper<Map<Double, Double>> mapper_doubleMap =
      new NullableValueMapper<>(new MapValueMapper<>(new DoubleValueMapper(), new DoubleValueMapper()));
  private final ValueMapper<Map<Float, Float>> mapper_floatMap = new NullableValueMapper<>(new MapValueMapper<>(
      new FloatValueMapper(),
      new FloatValueMapper()));
  private final ValueMapper<Map<Byte, Byte>> mapper_byteMap =
      new NullableValueMapper<>(new MapValueMapper<>(new ByteValueMapper(), new ByteValueMapper()));
  private final ValueMapper<Map<Short, Short>> mapper_shortMap = new NullableValueMapper<>(new MapValueMapper<>(
      new ShortValueMapper(),
      new ShortValueMapper()));
  private final ValueMapper<Map<Integer, Integer>> mapper_intMap =
      new NullableValueMapper<>(new MapValueMapper<>(new IntegerValueMapper(), new IntegerValueMapper()));
  private final ValueMapper<Map<Long, Long>> mapper_longMap =
      new NullableValueMapper<>(new MapValueMapper<>(new LongValueMapper(), new LongValueMapper()));
  private final ValueMapper<Map<Character, Character>> mapper_charMap =
      new NullableValueMapper<>(new MapValueMapper<>(new CharacterValueMapper(), new CharacterValueMapper()));
  private final ValueMapper<Map<Boolean, Boolean>> mapper_booleanMap =
      new NullableValueMapper<>(new MapValueMapper<>(new BooleanValueMapper(), new BooleanValueMapper()));
  private final ValueMapper<Map<String, String>> mapper_stringMap =
      new NullableValueMapper<>(new MapValueMapper<>(new StringValueMapper(), new StringValueMapper()));
  private final ValueMapper<ParameterTestActivity.Tenum> mapper_testEnum =
      new NullableValueMapper<>(new EnumValueMapper<>(ParameterTestActivity.Tenum.class));

  private final ValueMapper<Map<Integer, List<String>>> mapper_mappyBoi =
      new NullableValueMapper<>(new MapValueMapper<>(
          new IntegerValueMapper(),
          new ListValueMapper<>(new StringValueMapper())));

  private final ValueMapper<int[][]> mapper_doublePrimIntArray =
      new NullableValueMapper<>(new ArrayValueMapper<>(new PrimitiveIntArrayValueMapper(), int[].class));

  private final ValueMapper<List<Integer>[][]> mapper_intListArrayArray =
      new NullableValueMapper<>(new ArrayValueMapper<>(new ArrayValueMapper<>(
          new ListValueMapper<>(new IntegerValueMapper()),
          List.class), List[].class));

  private final ValueMapper<List<Map<String[][], Map<Integer, List<Float>[]>>>> mapper_obnoxious =
      new NullableValueMapper<>(new ListValueMapper<>(new MapValueMapper<>(
          new ArrayValueMapper<>(new ArrayValueMapper<>(
              new StringValueMapper(),
              String.class), String[].class),
          new MapValueMapper<>(
              new IntegerValueMapper(),
              new ArrayValueMapper<>(
                  new ListValueMapper<>(new FloatValueMapper()),
                  List.class)))));

  public ParameterTestActivityMapper() { }

  @Override
  public String getName() {
    return "ParameterTest";
  }

  @Override
  public Map<String, ValueSchema> getParameters() {
    final var parameters = new HashMap<String, ValueSchema>();
    parameters.put("primitiveDouble", this.mapper_primitiveDouble.getValueSchema());
    parameters.put("primitiveFloat", this.mapper_primitiveFloat.getValueSchema());
    parameters.put("primitiveByte", this.mapper_primitiveByte.getValueSchema());
    parameters.put("primitiveShort", this.mapper_primitiveShort.getValueSchema());
    parameters.put("primitiveInt", this.mapper_primitiveInt.getValueSchema());
    parameters.put("primitiveLong", this.mapper_primitiveLong.getValueSchema());
    parameters.put("primitiveChar", this.mapper_primitiveChar.getValueSchema());
    parameters.put("primitiveBoolean", this.mapper_primitiveBoolean.getValueSchema());
    parameters.put("boxedDouble", this.mapper_boxedDouble.getValueSchema());
    parameters.put("boxedFloat", this.mapper_boxedFloat.getValueSchema());
    parameters.put("boxedByte", this.mapper_boxedByte.getValueSchema());
    parameters.put("boxedShort", this.mapper_boxedShort.getValueSchema());
    parameters.put("boxedInt", this.mapper_boxedInt.getValueSchema());
    parameters.put("boxedLong", this.mapper_boxedLong.getValueSchema());
    parameters.put("boxedChar", this.mapper_boxedChar.getValueSchema());
    parameters.put("boxedBoolean", this.mapper_boxedBoolean.getValueSchema());
    parameters.put("string", this.mapper_string.getValueSchema());
    parameters.put("doubleArray", this.mapper_doubleArray.getValueSchema());
    parameters.put("floatArray", this.mapper_floatArray.getValueSchema());
    parameters.put("byteArray", this.mapper_byteArray.getValueSchema());
    parameters.put("shortArray", this.mapper_shortArray.getValueSchema());
    parameters.put("intArray", this.mapper_intArray.getValueSchema());
    parameters.put("longArray", this.mapper_longArray.getValueSchema());
    parameters.put("charArray", this.mapper_charArray.getValueSchema());
    parameters.put("booleanArray", this.mapper_booleanArray.getValueSchema());
    parameters.put("stringArray", this.mapper_stringArray.getValueSchema());
    parameters.put("primDoubleArray", this.mapper_primDoubleArray.getValueSchema());
    parameters.put("primFloatArray", this.mapper_primFloatArray.getValueSchema());
    parameters.put("primByteArray", this.mapper_primByteArray.getValueSchema());
    parameters.put("primShortArray", this.mapper_primShortArray.getValueSchema());
    parameters.put("primIntArray", this.mapper_primIntArray.getValueSchema());
    parameters.put("primLongArray", this.mapper_primLongArray.getValueSchema());
    parameters.put("primCharArray", this.mapper_primCharArray.getValueSchema());
    parameters.put("primBooleanArray", this.mapper_primBooleanArray.getValueSchema());
    parameters.put("doubleList", this.mapper_doubleList.getValueSchema());
    parameters.put("floatList", this.mapper_floatList.getValueSchema());
    parameters.put("byteList", this.mapper_byteList.getValueSchema());
    parameters.put("shortList", this.mapper_shortList.getValueSchema());
    parameters.put("intList", this.mapper_intList.getValueSchema());
    parameters.put("longList", this.mapper_longList.getValueSchema());
    parameters.put("charList", this.mapper_charList.getValueSchema());
    parameters.put("booleanList", this.mapper_booleanList.getValueSchema());
    parameters.put("stringList", this.mapper_stringList.getValueSchema());
    parameters.put("doubleMap", this.mapper_doubleMap.getValueSchema());
    parameters.put("floatMap", this.mapper_floatMap.getValueSchema());
    parameters.put("byteMap", this.mapper_byteMap.getValueSchema());
    parameters.put("shortMap", this.mapper_shortMap.getValueSchema());
    parameters.put("intMap", this.mapper_intMap.getValueSchema());
    parameters.put("longMap", this.mapper_longMap.getValueSchema());
    parameters.put("charMap", this.mapper_charMap.getValueSchema());
    parameters.put("booleanMap", this.mapper_booleanMap.getValueSchema());
    parameters.put("stringMap", this.mapper_stringMap.getValueSchema());
    parameters.put("testEnum", this.mapper_testEnum.getValueSchema());
    parameters.put("mappyBoi", this.mapper_mappyBoi.getValueSchema());
    parameters.put("doublePrimIntArray", this.mapper_doublePrimIntArray.getValueSchema());
    parameters.put("intListArrayArray", this.mapper_intListArrayArray.getValueSchema());
    parameters.put("obnoxious", this.mapper_obnoxious.getValueSchema());
    return parameters;
  }

  @Override
  public ParameterTestActivity instantiateDefault() {
    return new ParameterTestActivity();
  }

  @Override
  public ParameterTestActivity instantiate(final Map<String, SerializedValue> arguments)
  throws TaskSpecType.UnconstructableTaskSpecException
  {
    final var activity = new ParameterTestActivity();
    for (final var entry : arguments.entrySet()) {
      switch (entry.getKey()) {
        case "primitiveDouble":
          activity.primitiveDouble = this.mapper_primitiveDouble
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primitiveFloat":
          activity.primitiveFloat = this.mapper_primitiveFloat
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primitiveByte":
          activity.primitiveByte = this.mapper_primitiveByte
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primitiveShort":
          activity.primitiveShort = this.mapper_primitiveShort
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primitiveInt":
          activity.primitiveInt = this.mapper_primitiveInt
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primitiveLong":
          activity.primitiveLong = this.mapper_primitiveLong
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primitiveChar":
          activity.primitiveChar = this.mapper_primitiveChar
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primitiveBoolean":
          activity.primitiveBoolean = this.mapper_primitiveBoolean.deserializeValue(entry.getValue()).getSuccessOrThrow(
              $ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "boxedDouble":
          activity.boxedDouble = this.mapper_boxedDouble
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "boxedFloat":
          activity.boxedFloat = this.mapper_boxedFloat
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "boxedByte":
          activity.boxedByte = this.mapper_boxedByte
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "boxedShort":
          activity.boxedShort = this.mapper_boxedShort
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "boxedInt":
          activity.boxedInt = this.mapper_boxedInt
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "boxedLong":
          activity.boxedLong = this.mapper_boxedLong
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "boxedChar":
          activity.boxedChar = this.mapper_boxedChar
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "boxedBoolean":
          activity.boxedBoolean = this.mapper_boxedBoolean
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "string":
          activity.string = this.mapper_string
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "doubleArray":
          activity.doubleArray = this.mapper_doubleArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "floatArray":
          activity.floatArray = this.mapper_floatArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "byteArray":
          activity.byteArray = this.mapper_byteArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "shortArray":
          activity.shortArray = this.mapper_shortArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "intArray":
          activity.intArray = this.mapper_intArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "longArray":
          activity.longArray = this.mapper_longArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "charArray":
          activity.charArray = this.mapper_charArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "booleanArray":
          activity.booleanArray = this.mapper_booleanArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "stringArray":
          activity.stringArray = this.mapper_stringArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primDoubleArray":
          activity.primDoubleArray = this.mapper_primDoubleArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primFloatArray":
          activity.primFloatArray = this.mapper_primFloatArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primByteArray":
          activity.primByteArray = this.mapper_primByteArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primShortArray":
          activity.primShortArray = this.mapper_primShortArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primIntArray":
          activity.primIntArray = this.mapper_primIntArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primLongArray":
          activity.primLongArray = this.mapper_primLongArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primCharArray":
          activity.primCharArray = this.mapper_primCharArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "primBooleanArray":
          activity.primBooleanArray = this.mapper_primBooleanArray.deserializeValue(entry.getValue()).getSuccessOrThrow(
              $ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "doubleList":
          activity.doubleList = this.mapper_doubleList
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "floatList":
          activity.floatList = this.mapper_floatList
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "byteList":
          activity.byteList = this.mapper_byteList
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "shortList":
          activity.shortList = this.mapper_shortList
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "intList":
          activity.intList = this.mapper_intList
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "longList":
          activity.longList = this.mapper_longList
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "charList":
          activity.charList = this.mapper_charList
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "booleanList":
          activity.booleanList = this.mapper_booleanList
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "stringList":
          activity.stringList = this.mapper_stringList
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "doubleMap":
          activity.doubleMap = this.mapper_doubleMap
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "floatMap":
          activity.floatMap = this.mapper_floatMap
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "byteMap":
          activity.byteMap = this.mapper_byteMap
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "shortMap":
          activity.shortMap = this.mapper_shortMap
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "intMap":
          activity.intMap = this.mapper_intMap
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "longMap":
          activity.longMap = this.mapper_longMap
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "charMap":
          activity.charMap = this.mapper_charMap
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "booleanMap":
          activity.booleanMap = this.mapper_booleanMap
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "stringMap":
          activity.stringMap = this.mapper_stringMap
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "testEnum":
          activity.testEnum = this.mapper_testEnum
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "mappyBoi":
          activity.mappyBoi = this.mapper_mappyBoi
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "doublePrimIntArray":
          activity.doublePrimIntArray = this.mapper_doublePrimIntArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "intListArrayArray":
          activity.intListArrayArray = this.mapper_intListArrayArray
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        case "obnoxious":
          activity.obnoxious = this.mapper_obnoxious
              .deserializeValue(entry.getValue())
              .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
          break;
        default:
          throw new TaskSpecType.UnconstructableTaskSpecException();
      }
    }
    return activity;
  }

  @Override
  public Map<String, SerializedValue> getArguments(final ParameterTestActivity activity) {
    final var arguments = new HashMap<String, SerializedValue>();
    arguments.put("primitiveDouble", this.mapper_primitiveDouble.serializeValue(activity.primitiveDouble));
    arguments.put("primitiveFloat", this.mapper_primitiveFloat.serializeValue(activity.primitiveFloat));
    arguments.put("primitiveByte", this.mapper_primitiveByte.serializeValue(activity.primitiveByte));
    arguments.put("primitiveShort", this.mapper_primitiveShort.serializeValue(activity.primitiveShort));
    arguments.put("primitiveInt", this.mapper_primitiveInt.serializeValue(activity.primitiveInt));
    arguments.put("primitiveLong", this.mapper_primitiveLong.serializeValue(activity.primitiveLong));
    arguments.put("primitiveChar", this.mapper_primitiveChar.serializeValue(activity.primitiveChar));
    arguments.put("primitiveBoolean", this.mapper_primitiveBoolean.serializeValue(activity.primitiveBoolean));
    arguments.put("boxedDouble", this.mapper_boxedDouble.serializeValue(activity.boxedDouble));
    arguments.put("boxedFloat", this.mapper_boxedFloat.serializeValue(activity.boxedFloat));
    arguments.put("boxedByte", this.mapper_boxedByte.serializeValue(activity.boxedByte));
    arguments.put("boxedShort", this.mapper_boxedShort.serializeValue(activity.boxedShort));
    arguments.put("boxedInt", this.mapper_boxedInt.serializeValue(activity.boxedInt));
    arguments.put("boxedLong", this.mapper_boxedLong.serializeValue(activity.boxedLong));
    arguments.put("boxedChar", this.mapper_boxedChar.serializeValue(activity.boxedChar));
    arguments.put("boxedBoolean", this.mapper_boxedBoolean.serializeValue(activity.boxedBoolean));
    arguments.put("string", this.mapper_string.serializeValue(activity.string));
    arguments.put("doubleArray", this.mapper_doubleArray.serializeValue(activity.doubleArray));
    arguments.put("floatArray", this.mapper_floatArray.serializeValue(activity.floatArray));
    arguments.put("byteArray", this.mapper_byteArray.serializeValue(activity.byteArray));
    arguments.put("shortArray", this.mapper_shortArray.serializeValue(activity.shortArray));
    arguments.put("intArray", this.mapper_intArray.serializeValue(activity.intArray));
    arguments.put("longArray", this.mapper_longArray.serializeValue(activity.longArray));
    arguments.put("charArray", this.mapper_charArray.serializeValue(activity.charArray));
    arguments.put("booleanArray", this.mapper_booleanArray.serializeValue(activity.booleanArray));
    arguments.put("stringArray", this.mapper_stringArray.serializeValue(activity.stringArray));
    arguments.put("primDoubleArray", this.mapper_primDoubleArray.serializeValue(activity.primDoubleArray));
    arguments.put("primFloatArray", this.mapper_primFloatArray.serializeValue(activity.primFloatArray));
    arguments.put("primByteArray", this.mapper_primByteArray.serializeValue(activity.primByteArray));
    arguments.put("primShortArray", this.mapper_primShortArray.serializeValue(activity.primShortArray));
    arguments.put("primIntArray", this.mapper_primIntArray.serializeValue(activity.primIntArray));
    arguments.put("primLongArray", this.mapper_primLongArray.serializeValue(activity.primLongArray));
    arguments.put("primCharArray", this.mapper_primCharArray.serializeValue(activity.primCharArray));
    arguments.put("primBooleanArray", this.mapper_primBooleanArray.serializeValue(activity.primBooleanArray));
    arguments.put("doubleList", this.mapper_doubleList.serializeValue(activity.doubleList));
    arguments.put("floatList", this.mapper_floatList.serializeValue(activity.floatList));
    arguments.put("byteList", this.mapper_byteList.serializeValue(activity.byteList));
    arguments.put("shortList", this.mapper_shortList.serializeValue(activity.shortList));
    arguments.put("intList", this.mapper_intList.serializeValue(activity.intList));
    arguments.put("longList", this.mapper_longList.serializeValue(activity.longList));
    arguments.put("charList", this.mapper_charList.serializeValue(activity.charList));
    arguments.put("booleanList", this.mapper_booleanList.serializeValue(activity.booleanList));
    arguments.put("stringList", this.mapper_stringList.serializeValue(activity.stringList));
    arguments.put("doubleMap", this.mapper_doubleMap.serializeValue(activity.doubleMap));
    arguments.put("floatMap", this.mapper_floatMap.serializeValue(activity.floatMap));
    arguments.put("byteMap", this.mapper_byteMap.serializeValue(activity.byteMap));
    arguments.put("shortMap", this.mapper_shortMap.serializeValue(activity.shortMap));
    arguments.put("intMap", this.mapper_intMap.serializeValue(activity.intMap));
    arguments.put("longMap", this.mapper_longMap.serializeValue(activity.longMap));
    arguments.put("charMap", this.mapper_charMap.serializeValue(activity.charMap));
    arguments.put("booleanMap", this.mapper_booleanMap.serializeValue(activity.booleanMap));
    arguments.put("stringMap", this.mapper_stringMap.serializeValue(activity.stringMap));
    arguments.put("testEnum", this.mapper_testEnum.serializeValue(activity.testEnum));
    arguments.put("mappyBoi", this.mapper_mappyBoi.serializeValue(activity.mappyBoi));
    arguments.put("doublePrimIntArray", this.mapper_doublePrimIntArray.serializeValue(activity.doublePrimIntArray));
    arguments.put("intListArrayArray", this.mapper_intListArrayArray.serializeValue(activity.intListArrayArray));
    arguments.put("obnoxious", this.mapper_obnoxious.serializeValue(activity.obnoxious));
    return arguments;
  }

  @Override
  public List<String> getValidationFailures(final ParameterTestActivity activity) {
    // TODO: Extract validation messages from @Validation annotation at compile time.
    return new ArrayList<>();
  }
}
