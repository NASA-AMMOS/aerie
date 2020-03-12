package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ActivityType(name="ParameterTestActivity", states=BananaStates.class)
public class ParameterTestActivity implements Activity<BananaStates> {
  // Primitive parameters
  @Parameter public double primitiveDouble = 3.141;
  @Parameter public float primitiveFloat = 1.618f;
  @Parameter public byte primitiveByte = 16;
  @Parameter public short primitiveShort = 32;
  @Parameter public int primitiveInt = 64;
  @Parameter public long primitiveLong = 128;
  @Parameter public char primitiveChar = 'g';
  @Parameter public boolean primitiveBoolean = true;

  // Boxed parameters
  @Parameter public Double boxedDouble = 6.282;
  @Parameter public Float boxedFloat = 3.236f;
  @Parameter public Byte boxedByte = 116;
  @Parameter public Short boxedShort = 132;
  @Parameter public Integer boxedInt = 164;
  @Parameter public Long boxedLong = 1128L;
  @Parameter public Character boxedChar = 'G';
  @Parameter public Boolean boxedBoolean = false;
  @Parameter public String string = "h";

  // Array parameters
  @Parameter public Double[] doubleArray = null;
  @Parameter public Float[] floatArray = null;
  @Parameter public Byte[] byteArray = null;
  @Parameter public Short[] shortArray = null;
  @Parameter public Integer[] intArray = null;
  @Parameter public Long[] longArray = null;
  @Parameter public Character[] charArray = null;
  @Parameter public Boolean[] booleanArray = null;
  @Parameter public String[] stringArray = null;

  // List parameters
  @Parameter public List<Double> doubleList = null;
  @Parameter public List<Float> floatList = null;
  @Parameter public List<Byte> byteList = null;
  @Parameter public List<Short> shortList = null;
  @Parameter public List<Integer> intList = null;
  @Parameter public List<Long> longList = null;
  @Parameter public List<Character> charList = null;
  @Parameter public List<Boolean> booleanList = null;
  @Parameter public List<String> stringList = null;

  // Map Parameters
  @Parameter public Map<Double, Double> doubleMap = null;
  @Parameter public Map<Float, Float> floatMap = null;
  @Parameter public Map<Byte, Byte> byteMap = null;
  @Parameter public Map<Short, Short> shortMap = null;
  @Parameter public Map<Integer, Integer> intMap = null;
  @Parameter public Map<Long, Long> longMap = null;
  @Parameter public Map<Character, Character> charMap = null;
  @Parameter public Map<Boolean, Boolean> booleanMap = null;
  @Parameter public Map<String, String> stringMap = null;

  // Complex Parameters
  @Parameter
  public Map<Integer, List<String>> mappyBoi = null;

  @Parameter
  public List<Integer>[][] intListArrayArray = null;

  @Parameter
  public List<Map<String[][], Map<Integer, List<Float>[]>>> obnoxious;
}
