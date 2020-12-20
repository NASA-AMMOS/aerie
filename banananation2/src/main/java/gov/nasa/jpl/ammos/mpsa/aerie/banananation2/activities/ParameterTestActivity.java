package gov.nasa.jpl.ammos.mpsa.aerie.banananation2.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.annotations.Parameter;

import java.util.List;
import java.util.Map;

public final class ParameterTestActivity {
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

  // Primitive Array parameters
  @Parameter public double[] primDoubleArray = null;
  @Parameter public float[] primFloatArray = null;
  @Parameter public byte[] primByteArray = null;
  @Parameter public short[] primShortArray = null;
  @Parameter public int[] primIntArray = null;
  @Parameter public long[] primLongArray = null;
  @Parameter public char[] primCharArray = null;
  @Parameter public boolean[] primBooleanArray = null;

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

  // Enum type
  public enum Tenum { A, B, C }
  @Parameter public Tenum testEnum = null;

  // Complex Parameters
  @Parameter
  public Map<Integer, List<String>> mappyBoi = null;

  @Parameter
  public int[][] doublePrimIntArray = null;

  @Parameter
  public List<Integer>[][] intListArrayArray = null;

  @Parameter
  public List<Map<String[][], Map<Integer, List<Float>[]>>> obnoxious;

  public static ParameterTestActivity createTestActivity() {
    ParameterTestActivity testActivity = new ParameterTestActivity();
    testActivity.primitiveDouble = 3.141;
    testActivity.primitiveFloat = 1.618f;
    testActivity.primitiveByte = 16;
    testActivity.primitiveShort = 32;
    testActivity.primitiveInt = 64;
    testActivity.primitiveLong = 128;
    testActivity.primitiveChar = 'g';
    testActivity.primitiveBoolean = true;
    testActivity.boxedDouble = 6.282;
    testActivity.boxedFloat = 3.236f;
    testActivity.boxedByte = 116;
    testActivity.boxedShort = 132;
    testActivity.boxedInt = 164;
    testActivity.boxedLong = 1128L;
    testActivity.boxedChar = 'G';
    testActivity.boxedBoolean =  false;
    testActivity.string = "h";
    testActivity.doubleArray = List.of(1.0, 2.0).toArray(new Double[2]);
    testActivity.floatArray = List.of(3.0f, 4.0f).toArray(new Float[2]);
    testActivity.byteArray = List.of((byte)5, (byte)6).toArray(new Byte[2]);
    testActivity.shortArray = List.of((short)7, (short)8).toArray(new Short[2]);
    testActivity.intArray = List.of(9, 10).toArray(new Integer[2]);
    testActivity.longArray = List.of((long)11, (long)12).toArray(new Long[2]);
    testActivity.charArray = List.of('a', 'b').toArray(new Character[2]);
    testActivity.booleanArray = List.of(true, false).toArray(new Boolean[2]);
    testActivity.stringArray = List.of("17", "18").toArray(new String[2]);
    testActivity.primDoubleArray = new double[] {102.0, 103.0};
    testActivity.primFloatArray = new float[] {(float)104.0, (float)105.0};
    testActivity.primByteArray = new byte[] {(byte)106, (byte)107};
    testActivity.primShortArray = new short[] {(short)108, (short)109};
    testActivity.primIntArray = new int[] {110, 111};
    testActivity.primLongArray = new long[] {(long)112, (long)113};
    testActivity.primCharArray = new char[] {'c', 'd'};
    testActivity.primBooleanArray = new boolean[] {true, false};
    testActivity.doubleList = List.of(19.0, 20.0);
    testActivity.floatList = List.of(21.0f, 22.0f);
    testActivity.byteList = List.of((byte)23, (byte)24);
    testActivity.shortList = List.of((short)25, (short)26);
    testActivity.intList = List.of(27, 28);
    testActivity.longList = List.of((long)29, (long)30);
    testActivity.charList = List.of('c', 'd');
    testActivity.booleanList = List.of(false, true);
    testActivity.stringList = List.of("35", "36");
    testActivity.doubleMap = Map.of(37.0, 38.0, 39.0, 40.0);
    testActivity.floatMap = Map.of(41.0f, 42.0f, 43.0f, 44.0f);
    testActivity.byteMap = Map.of((byte)45, (byte)46, (byte)47, (byte)48);
    testActivity.shortMap = Map.of((short)49, (short)50, (short)51, (short)52);
    testActivity.intMap = Map.of(53, 54, 55, 56);
    testActivity.longMap = Map.of((long)57, (long)58, (long)59, (long)60);
    testActivity.charMap = Map.of('e', 'f', 'g', 'h');
    testActivity.booleanMap = Map.of(false, true, true, false);
    testActivity.stringMap = Map.of("69", "70", "71", "72");
    testActivity.testEnum = Tenum.A;
    testActivity.mappyBoi = Map.of(
            100, List.of("abc", "xyz"),
            200, List.of("def", "uvw")
    );
    testActivity.doublePrimIntArray = new int[][] {
            new int[] {101, 102},
            new int[] {103, 103}
    };
    @SuppressWarnings("unchecked")
    final List<Integer>[][] intListArrayArray = new List[][] {
            new List[] {
                    List.of(200, 201),
                    List.of(202, 203),
            },
            new List[] {
                    List.of(204, 205),
                    List.of(206, 207)
            }
    };
    testActivity.intListArrayArray = intListArrayArray;
    @SuppressWarnings("unchecked")
    final List<Map<String[][], Map<Integer, List<Float>[]>>> obnoxious = List.of(
            Map.of(
                    new String[][] {
                            new String[] {"300", "301"},
                            new String[] {"302", "303"}
                    }, Map.of(
                            500, new List[] {
                                    List.of(400.0f, 401.0f),
                                    List.of(402.0f, 403.0f)
                            },
                            501, new List[] {
                                    List.of(404.0f, 405.0f),
                                    List.of(406.0f, 407.0f)
                            }
                    ),
                    new String[][] {
                            new String[] {"304", "305"},
                            new String[] {"306", "307"}
                    }, Map.of(
                            502, new List[] {
                                    List.of(408.0f, 409.0f),
                                    List.of(410.0f, 411.0f)
                            },
                            503, new List[] {
                                    List.of(412.0f, 413.0f),
                                    List.of(414.0f, 415.0f)
                            }
                    )
            ),
            Map.of(
                    new String[][] {
                            new String[] {"308", "309"},
                            new String[] {"310", "311"}
                    }, Map.of(
                            504, new List[] {
                                    List.of(416.0f, 417.0f),
                                    List.of(418.0f, 419.0f)
                            },
                            505, new List[] {
                                    List.of(420.0f, 421.0f),
                                    List.of(422.0f, 423.0f)
                            }
                    ),
                    new String[][] {
                            new String[] {"312", "313"},
                            new String[] {"314", "315"}
                    }, Map.of(
                            506, new List[] {
                                    List.of(424.0f, 425.0f),
                                    List.of(426.0f, 427.0f)
                            },
                            507, new List[] {
                                    List.of(428.0f, 429.0f),
                                    List.of(430.0f, 431.0f)
                            }
                    )
            )
    );
    testActivity.obnoxious = obnoxious;

    return testActivity;
  }
}
