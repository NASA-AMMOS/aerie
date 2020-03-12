package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ParameterTestActivityTest {

    private ParameterTestActivity$$ActivityMapper mapper;

    public ParameterTestActivityTest() {
        mapper = new ParameterTestActivity$$ActivityMapper();
    }

    @Test
    public void testSerializedToSerialized() {
        SerializedActivity sourceActivity = createSerializedInstance();
        var maybeDeserializedActivity = mapper.deserializeActivity(sourceActivity);

        if (maybeDeserializedActivity.isEmpty()) {
            fail("Deserialization failed!");
        }

        ParameterTestActivity deserializedActivity = (ParameterTestActivity)maybeDeserializedActivity.get();

        Optional<SerializedActivity> maybeReserializedActivity = mapper.serializeActivity(deserializedActivity);

        if (maybeReserializedActivity.isEmpty()) {
            fail("Serialization failed!");
        }

        SerializedActivity reserializedActivity = maybeReserializedActivity.get();

        assertEquals(sourceActivity, reserializedActivity);
    }

    @Test
    public void testDeserializedToDeserialized() {
        ParameterTestActivity sourceActivity = createParameterTestActivityInstance();
        Optional<SerializedActivity> maybeSerializedActivity = mapper.serializeActivity(sourceActivity);

        if (maybeSerializedActivity.isEmpty()) {
            fail("Serialization failed!");
        }

        SerializedActivity serializedActivity = maybeSerializedActivity.get();

        var maybeDeserializedActivity = mapper.deserializeActivity(serializedActivity);

        if (maybeDeserializedActivity.isEmpty()) {
            fail("Deserialization failed!");
        }

        ParameterTestActivity deserializedActivity = (ParameterTestActivity)maybeDeserializedActivity.get();

        assertEquals(sourceActivity.primitiveDouble, deserializedActivity.primitiveDouble, 0.0);
        assertEquals(sourceActivity.primitiveFloat, deserializedActivity.primitiveFloat, 0.0);
        assertEquals(sourceActivity.primitiveByte, deserializedActivity.primitiveByte);
        assertEquals(sourceActivity.primitiveShort, deserializedActivity.primitiveShort);
        assertEquals(sourceActivity.primitiveInt, deserializedActivity.primitiveInt);
        assertEquals(sourceActivity.primitiveLong, deserializedActivity.primitiveLong);
        assertEquals(sourceActivity.primitiveChar, deserializedActivity.primitiveChar);
        assertEquals(sourceActivity.primitiveBoolean, deserializedActivity.primitiveBoolean);
        assertEquals(sourceActivity.boxedDouble, deserializedActivity.boxedDouble);
        assertEquals(sourceActivity.boxedFloat, deserializedActivity.boxedFloat);
        assertEquals(sourceActivity.boxedByte, deserializedActivity.boxedByte);
        assertEquals(sourceActivity.boxedShort, deserializedActivity.boxedShort);
        assertEquals(sourceActivity.boxedInt, deserializedActivity.boxedInt);
        assertEquals(sourceActivity.boxedLong, deserializedActivity.boxedLong);
        assertEquals(sourceActivity.boxedChar, deserializedActivity.boxedChar);
        assertEquals(sourceActivity.boxedBoolean, deserializedActivity.boxedBoolean);
        assertEquals(sourceActivity.string, deserializedActivity.string);
        assertArrayEquals(sourceActivity.doubleArray, deserializedActivity.doubleArray);
        assertArrayEquals(sourceActivity.floatArray, deserializedActivity.floatArray);
        assertArrayEquals(sourceActivity.byteArray, deserializedActivity.byteArray);
        assertArrayEquals(sourceActivity.shortArray, deserializedActivity.shortArray);
        assertArrayEquals(sourceActivity.intArray, deserializedActivity.intArray);
        assertArrayEquals(sourceActivity.longArray, deserializedActivity.longArray);
        assertArrayEquals(sourceActivity.charArray, deserializedActivity.charArray);
        assertArrayEquals(sourceActivity.booleanArray, deserializedActivity.booleanArray);
        assertArrayEquals(sourceActivity.stringArray, deserializedActivity.stringArray);
        assertEquals(sourceActivity.doubleList, deserializedActivity.doubleList);
        assertEquals(sourceActivity.floatList, deserializedActivity.floatList);
        assertEquals(sourceActivity.byteList, deserializedActivity.byteList);
        assertEquals(sourceActivity.shortList, deserializedActivity.shortList);
        assertEquals(sourceActivity.intList, deserializedActivity.intList);
        assertEquals(sourceActivity.longList, deserializedActivity.longList);
        assertEquals(sourceActivity.charList, deserializedActivity.charList);
        assertEquals(sourceActivity.booleanList, deserializedActivity.booleanList);
        assertEquals(sourceActivity.stringList, deserializedActivity.stringList);
        assertEquals(sourceActivity.doubleMap, deserializedActivity.doubleMap);
        assertEquals(sourceActivity.floatMap, deserializedActivity.floatMap);
        assertEquals(sourceActivity.byteMap, deserializedActivity.byteMap);
        assertEquals(sourceActivity.shortMap, deserializedActivity.shortMap);
        assertEquals(sourceActivity.intMap, deserializedActivity.intMap);
        assertEquals(sourceActivity.longMap, deserializedActivity.longMap);
        assertEquals(sourceActivity.charMap, deserializedActivity.charMap);
        assertEquals(sourceActivity.booleanMap, deserializedActivity.booleanMap);
        assertEquals(sourceActivity.stringMap, deserializedActivity.stringMap);
        assertEquals(sourceActivity.mappyBoi, deserializedActivity.mappyBoi);
        assertArrayEquals(sourceActivity.intListArrayArray, deserializedActivity.intListArrayArray);
        // TODO; Check equality for obnoxious (this is quite complex)
    }

    private SerializedActivity createSerializedInstance() {
        Map<String, SerializedParameter> parameters = new HashMap<>();

        // Primitive parameters
        parameters.put("primitiveDouble", SerializedParameter.of(3.141));
        parameters.put("primitiveFloat", SerializedParameter.of(1.618f));
        parameters.put("primitiveByte", SerializedParameter.of(16));
        parameters.put("primitiveShort", SerializedParameter.of(32));
        parameters.put("primitiveInt", SerializedParameter.of(64));
        parameters.put("primitiveLong", SerializedParameter.of(128));
        parameters.put("primitiveChar", SerializedParameter.of("g"));
        parameters.put("primitiveBoolean", SerializedParameter.of(true));

        // Boxed parameters
        parameters.put("boxedDouble", SerializedParameter.of(6.282));
        parameters.put("boxedFloat", SerializedParameter.of(3.236f));
        parameters.put("boxedByte", SerializedParameter.of(116));
        parameters.put("boxedShort", SerializedParameter.of(132));
        parameters.put("boxedInt", SerializedParameter.of(164));
        parameters.put("boxedLong", SerializedParameter.of(1128L));
        parameters.put("boxedChar", SerializedParameter.of("G"));
        parameters.put("boxedBoolean", SerializedParameter.of(false));
        parameters.put("string", SerializedParameter.of("h"));

        // Array parameters
        parameters.put("doubleArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(1.0),
                        SerializedParameter.of(2.0)
                )
        ));
        parameters.put("floatArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(3.0),
                        SerializedParameter.of(4.0)
                )
        ));
        parameters.put("byteArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(5),
                        SerializedParameter.of(6)
                )
        ));
        parameters.put("shortArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(7),
                        SerializedParameter.of(8)
                )
        ));
        parameters.put("intArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(9),
                        SerializedParameter.of(10)
                )
        ));
        parameters.put("longArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(11),
                        SerializedParameter.of(12)
                )
        ));
        parameters.put("charArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of("a"),
                        SerializedParameter.of("b")
                )
        ));
        parameters.put("booleanArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(true),
                        SerializedParameter.of(false)
                )
        ));
        parameters.put("stringArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of("17"),
                        SerializedParameter.of("18")
                )
        ));

        // List parameters
        parameters.put("doubleList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(19.0),
                        SerializedParameter.of(20.0)
                )
        ));
        parameters.put("floatList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(21.0),
                        SerializedParameter.of(22.0)
                )
        ));
        parameters.put("byteList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(23),
                        SerializedParameter.of(24)
                )
        ));
        parameters.put("shortList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(25),
                        SerializedParameter.of(26)
                )
        ));
        parameters.put("intList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(27),
                        SerializedParameter.of(28)
                )
        ));
        parameters.put("longList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(29),
                        SerializedParameter.of(30)
                )
        ));
        parameters.put("charList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of("c"),
                        SerializedParameter.of("d")
                )
        ));
        parameters.put("booleanList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(false),
                        SerializedParameter.of(true)
                )
        ));
        parameters.put("stringList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of("35"),
                        SerializedParameter.of("36")
                )
        ));


        // Map Parameters
        parameters.put("doubleMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(37.0), SerializedParameter.of(39.0))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(38.0), SerializedParameter.of(40.0)))
                )
        ));
        parameters.put("floatMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(41.0), SerializedParameter.of(43.0))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(42.0), SerializedParameter.of(44.0)))
                )
        ));
        parameters.put("byteMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(45), SerializedParameter.of(47))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(46), SerializedParameter.of(48)))
                )
        ));
        parameters.put("shortMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(49), SerializedParameter.of(51))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(50), SerializedParameter.of(52)))
                )
        ));
        parameters.put("intMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(53), SerializedParameter.of(55))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(54), SerializedParameter.of(56)))
                )
        ));
        parameters.put("longMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(57), SerializedParameter.of(59))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(58), SerializedParameter.of(60)))
                )
        ));
        parameters.put("charMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of("e"), SerializedParameter.of("g"))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of("f"), SerializedParameter.of("h")))
                )
        ));
        parameters.put("booleanMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(false), SerializedParameter.of(true))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(true), SerializedParameter.of(false)))
                )
        ));
        parameters.put("stringMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of("69"), SerializedParameter.of("71"))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of("70"), SerializedParameter.of("72")))
                )
        ));

        // Complex Parameters
        parameters.put("mappyBoi", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(
                                List.of(
                                        SerializedParameter.of(101),
                                        SerializedParameter.of(104)
                                )
                        ),
                        "values", SerializedParameter.of(
                                List.of(
                                        SerializedParameter.of(List.of(
                                                SerializedParameter.of("102"),
                                                SerializedParameter.of("103")
                                        )),
                                        SerializedParameter.of(List.of(
                                                SerializedParameter.of("105"),
                                                SerializedParameter.of("106")
                                        ))
                                )
                        )
                )
        ));

        parameters.put("intListArrayArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(List.of(
                                SerializedParameter.of(List.of(
                                        SerializedParameter.of(110),
                                        SerializedParameter.of(111)
                                )),
                                SerializedParameter.of(List.of(
                                        SerializedParameter.of(112),
                                        SerializedParameter.of(113)
                                ))
                        )),
                        SerializedParameter.of(List.of(
                                SerializedParameter.of(List.of(
                                        SerializedParameter.of(114),
                                        SerializedParameter.of(115)
                                )),
                                SerializedParameter.of(List.of(
                                        SerializedParameter.of(116),
                                        SerializedParameter.of(117)
                                ))
                        ))
                )
        ));

        parameters.put("obnoxious", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(Map.of(
                                "keys", SerializedParameter.of(List.of(
                                        SerializedParameter.of(List.of(
                                                SerializedParameter.of(List.of(
                                                        SerializedParameter.of("200"),
                                                        SerializedParameter.of("201")
                                                )),
                                                SerializedParameter.of(List.of(
                                                        SerializedParameter.of("202"),
                                                        SerializedParameter.of("203")
                                                ))
                                        )),
                                        SerializedParameter.of(List.of(
                                                SerializedParameter.of(List.of(
                                                        SerializedParameter.of("204"),
                                                        SerializedParameter.of("205")
                                                )),
                                                SerializedParameter.of(List.of(
                                                        SerializedParameter.of("206"),
                                                        SerializedParameter.of("207")
                                                ))
                                        ))
                                )),
                                "values", SerializedParameter.of(List.of(
                                        SerializedParameter.of(Map.of(
                                                "keys", SerializedParameter.of(List.of(
                                                        SerializedParameter.of(208),
                                                        SerializedParameter.of(209)
                                                )),
                                                "values", SerializedParameter.of(List.of(
                                                        SerializedParameter.of(List.of(
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(210.0),
                                                                        SerializedParameter.of(211.0)
                                                                )),
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(212.0),
                                                                        SerializedParameter.of(213.0)
                                                                ))
                                                        )),
                                                        SerializedParameter.of(List.of(
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(214.0),
                                                                        SerializedParameter.of(215.0)
                                                                )),
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(216.0),
                                                                        SerializedParameter.of(217.0)
                                                                ))
                                                        ))
                                                ))
                                        )),
                                        SerializedParameter.of(Map.of(
                                                "keys", SerializedParameter.of(List.of(
                                                        SerializedParameter.of(218),
                                                        SerializedParameter.of(219)
                                                )),
                                                "values", SerializedParameter.of(List.of(
                                                        SerializedParameter.of(List.of(
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(220.0),
                                                                        SerializedParameter.of(221.0)
                                                                )),
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(222.0),
                                                                        SerializedParameter.of(223.0)
                                                                ))
                                                        )),
                                                        SerializedParameter.of(List.of(
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(224.0),
                                                                        SerializedParameter.of(225.0)
                                                                )),
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(226.0),
                                                                        SerializedParameter.of(227.0)
                                                                ))
                                                        ))
                                                ))
                                        ))
                                ))
                        ))
                )
        ));

        return new SerializedActivity("ParameterTestActivity", parameters);
    }

    private ParameterTestActivity createParameterTestActivityInstance() {
        ParameterTestActivity activity = new ParameterTestActivity();

        // Primitive parameters
        activity.primitiveDouble = 1.0;
        activity.primitiveFloat = 2.0f;
        activity.primitiveByte = 3;
        activity.primitiveShort = 4;
        activity.primitiveInt = 5;
        activity.primitiveLong = 6;
        activity.primitiveChar = '7';
        activity.primitiveBoolean = true;

        // Boxed parameters
        activity.boxedDouble = 8.0;
        activity.boxedFloat = 9.0f;
        activity.boxedByte = 10;
        activity.boxedShort = 11;
        activity.boxedInt = 12;
        activity.boxedLong = 13L;
        activity.boxedChar = 'n';
        activity.boxedBoolean = false;
        activity.string = "p";

        // Array parameters
        activity.doubleArray = new Double[] {17.0, 18.0};
        activity.floatArray = new Float[] {19.0f, 20.0f};
        activity.byteArray = new Byte[] {21, 22};
        activity.shortArray = new Short[] {23, 24};
        activity.intArray = new Integer[] {25, 26};
        activity.longArray = new Long[] {27L, 28L};
        activity.charArray = new Character[] {'C', 'D'};
        activity.booleanArray = new Boolean[] {true, false};
        activity.stringArray = new String[] {"33", "34"};

        // List parameters
        activity.doubleList = List.of(35.0, 36.0);
        activity.floatList = List.of(37.0f, 38.0f);
        activity.byteList = List.of((byte)39, (byte)40);
        activity.shortList = List.of((short)41, (short)42);
        activity.intList = List.of(43, 44);
        activity.longList = List.of(45L, 46L);
        activity.charList = List.of('U', 'V');
        activity.booleanList = List.of(false, true);
        activity.stringList = List.of("51", "52");

        // Map Parameters
        activity.doubleMap = Map.of(53.0, 54.0, 55.0, 56.0);
        activity.floatMap = Map.of(57.0f, 58.0f, 59.0f, 60.0f);
        activity.byteMap = Map.of((byte)61, (byte)62, (byte)63, (byte)64);
        activity.shortMap = Map.of((short)65, (short)66, (short)67, (short)68);
        activity.intMap = Map.of(69, 70, 71, 72);
        activity.longMap = Map.of(73L, 74L, 75L, 76L);
        activity.charMap = Map.of('a', 'b', 'x', 'y');
        activity.booleanMap = Map.of(false, true, true, false);
        activity.stringMap = Map.of("A", "X", "B", "Y");

        // Complex Parameters
        activity.mappyBoi = Map.of(
                100, List.of("abc", "xyz"),
                200, List.of("def", "uvw")
        );

        @SuppressWarnings("unchecked")
        List<Integer>[][] intListArrArr = new List[][] {
                new List[] {
                        List.of(200, 201),
                        List.of(202, 203),
                },
                new List[] {
                        List.of(204, 205),
                        List.of(206, 207)
                }
        };
        activity.intListArrayArray = intListArrArr;

        //List<Map<String[][], Map<Integer, List<Float>[]>>> obnox =

        @SuppressWarnings("unchecked")
        List<Map<String[][], Map<Integer, List<Float>[]>>> obnox = List.of(
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
        activity.obnoxious = obnox;

        return activity;
    }
}
