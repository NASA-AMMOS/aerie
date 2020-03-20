package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
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
    public void testDeserialization() {
        final SerializedActivity sourceActivity = createSerializedInstance();
        final ParameterTestActivity testValues = ParameterTestActivity.createTestActivity();
        final var maybeDeserializedActivity = mapper.deserializeActivity(sourceActivity);

        if (maybeDeserializedActivity.isEmpty()) {
            fail("Deserialization failed!");
        }

        final ParameterTestActivity deserializedActivity = (ParameterTestActivity)maybeDeserializedActivity.get();

        // Verify the deserialized activity contains the expected values
        assertEquals(deserializedActivity.primitiveDouble, testValues.primitiveDouble, 0.0);
        assertEquals(deserializedActivity.primitiveFloat, testValues.primitiveFloat, 0.0);
        assertEquals(deserializedActivity.primitiveByte, testValues.primitiveByte);
        assertEquals(deserializedActivity.primitiveShort, testValues.primitiveShort);
        assertEquals(deserializedActivity.primitiveInt, testValues.primitiveInt);
        assertEquals(deserializedActivity.primitiveLong, testValues.primitiveLong);
        assertEquals(deserializedActivity.primitiveChar, testValues.primitiveChar);
        assertEquals(deserializedActivity.primitiveBoolean, testValues.primitiveBoolean);
        assertEquals(deserializedActivity.boxedDouble, testValues.boxedDouble);
        assertEquals(deserializedActivity.boxedFloat, testValues.boxedFloat);
        assertEquals(deserializedActivity.boxedByte, testValues.boxedByte);
        assertEquals(deserializedActivity.boxedShort, testValues.boxedShort);
        assertEquals(deserializedActivity.boxedInt, testValues.boxedInt);
        assertEquals(deserializedActivity.boxedLong, testValues.boxedLong);
        assertEquals(deserializedActivity.boxedChar, testValues.boxedChar);
        assertEquals(deserializedActivity.boxedBoolean, testValues.boxedBoolean);
        assertEquals(deserializedActivity.string, testValues.string);
        assertArrayEquals(deserializedActivity.doubleArray, testValues.doubleArray);
        assertArrayEquals(deserializedActivity.floatArray, testValues.floatArray);
        assertArrayEquals(deserializedActivity.byteArray, testValues.byteArray);
        assertArrayEquals(deserializedActivity.shortArray, testValues.shortArray);
        assertArrayEquals(deserializedActivity.intArray, testValues.intArray);
        assertArrayEquals(deserializedActivity.longArray, testValues.longArray);
        assertArrayEquals(deserializedActivity.charArray, testValues.charArray);
        assertArrayEquals(deserializedActivity.booleanArray, testValues.booleanArray);
        assertArrayEquals(deserializedActivity.stringArray, testValues.stringArray);
        assertEquals(deserializedActivity.doubleList, testValues.doubleList);
        assertEquals(deserializedActivity.floatList, testValues.floatList);
        assertEquals(deserializedActivity.byteList, testValues.byteList);
        assertEquals(deserializedActivity.shortList, testValues.shortList);
        assertEquals(deserializedActivity.intList, testValues.intList);
        assertEquals(deserializedActivity.longList, testValues.longList);
        assertEquals(deserializedActivity.charList, testValues.charList);
        assertEquals(deserializedActivity.booleanList, testValues.booleanList);
        assertEquals(deserializedActivity.stringList, testValues.stringList);
        assertEquals(deserializedActivity.doubleMap, testValues.doubleMap);
        assertEquals(deserializedActivity.floatMap, testValues.floatMap);
        assertEquals(deserializedActivity.byteMap, testValues.byteMap);
        assertEquals(deserializedActivity.shortMap, testValues.shortMap);
        assertEquals(deserializedActivity.intMap, testValues.intMap);
        assertEquals(deserializedActivity.longMap, testValues.longMap);
        assertEquals(deserializedActivity.charMap, testValues.charMap);
        assertEquals(deserializedActivity.booleanMap, testValues.booleanMap);
        assertEquals(deserializedActivity.stringMap, testValues.stringMap);
        assertEquals(deserializedActivity.mappyBoi, testValues.mappyBoi);
        assertArrayEquals(deserializedActivity.intListArrayArray, testValues.intListArrayArray);
        // TODO; Check equality for obnoxious (this is quite complex)
    }

    @Test
    public void testSerialization() {
        final ParameterTestActivity sourceActivity = createParameterTestActivityInstance();
        final Optional<SerializedActivity> maybeSerializedActivity = mapper.serializeActivity(sourceActivity);

        if (maybeSerializedActivity.isEmpty()) {
            fail("Serialization failed!");
        }

        final SerializedActivity serializedActivity = maybeSerializedActivity.get();

        final var maybeDeserializedActivity = mapper.deserializeActivity(serializedActivity);

        if (maybeDeserializedActivity.isEmpty()) {
            fail("Deserialization failed!");
        }

        final ParameterTestActivity deserializedActivity = (ParameterTestActivity)maybeDeserializedActivity.get();

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
        final ParameterTestActivity testValues = ParameterTestActivity.createTestActivity();
        final Map<String, SerializedParameter> parameters = new HashMap<>();

        // Primitive parameters
        parameters.put("primitiveDouble", SerializedParameter.of(testValues.primitiveDouble));
        parameters.put("primitiveFloat", SerializedParameter.of(testValues.primitiveFloat));
        parameters.put("primitiveByte", SerializedParameter.of(testValues.primitiveByte));
        parameters.put("primitiveShort", SerializedParameter.of(testValues.primitiveShort));
        parameters.put("primitiveInt", SerializedParameter.of(testValues.primitiveInt));
        parameters.put("primitiveLong", SerializedParameter.of(testValues.primitiveLong));
        parameters.put("primitiveChar", SerializedParameter.of("" + testValues.primitiveChar));
        parameters.put("primitiveBoolean", SerializedParameter.of(testValues.primitiveBoolean));

        // Boxed parameters
        parameters.put("boxedDouble", SerializedParameter.of(testValues.boxedDouble));
        parameters.put("boxedFloat", SerializedParameter.of(testValues.boxedFloat));
        parameters.put("boxedByte", SerializedParameter.of(testValues.boxedByte));
        parameters.put("boxedShort", SerializedParameter.of(testValues.boxedShort));
        parameters.put("boxedInt", SerializedParameter.of(testValues.boxedInt));
        parameters.put("boxedLong", SerializedParameter.of(testValues.boxedLong));
        parameters.put("boxedChar", SerializedParameter.of("" + testValues.boxedChar));
        parameters.put("boxedBoolean", SerializedParameter.of(testValues.boxedBoolean));
        parameters.put("string", SerializedParameter.of(testValues.string));

        // Array parameters
        parameters.put("doubleArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.doubleArray[0]),
                        SerializedParameter.of(testValues.doubleArray[1])
                )
        ));
        parameters.put("floatArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.floatArray[0]),
                        SerializedParameter.of(testValues.floatArray[1])
                )
        ));
        parameters.put("byteArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.byteArray[0]),
                        SerializedParameter.of(testValues.byteArray[1])
                )
        ));
        parameters.put("shortArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.shortArray[0]),
                        SerializedParameter.of(testValues.shortArray[1])
                )
        ));
        parameters.put("intArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.intArray[0]),
                        SerializedParameter.of(testValues.intArray[1])
                )
        ));
        parameters.put("longArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.longArray[0]),
                        SerializedParameter.of(testValues.longArray[1])
                )
        ));
        parameters.put("charArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of("" + testValues.charArray[0]),
                        SerializedParameter.of("" + testValues.charArray[1])
                )
        ));
        parameters.put("booleanArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.booleanArray[0]),
                        SerializedParameter.of(testValues.booleanArray[1])
                )
        ));
        parameters.put("stringArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.stringArray[0]),
                        SerializedParameter.of(testValues.stringArray[1])
                )
        ));

        // List parameters
        parameters.put("doubleList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.doubleList.get(0)),
                        SerializedParameter.of(testValues.doubleList.get(1))
                )
        ));
        parameters.put("floatList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.floatList.get(0)),
                        SerializedParameter.of(testValues.floatList.get(1))
                )
        ));
        parameters.put("byteList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.byteList.get(0)),
                        SerializedParameter.of(testValues.byteList.get(1))
                )
        ));
        parameters.put("shortList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.shortList.get(0)),
                        SerializedParameter.of(testValues.shortList.get(1))
                )
        ));
        parameters.put("intList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.intList.get(0)),
                        SerializedParameter.of(testValues.intList.get(1))
                )
        ));
        parameters.put("longList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.longList.get(0)),
                        SerializedParameter.of(testValues.longList.get(1))
                )
        ));
        parameters.put("charList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of("" + testValues.charList.get(0)),
                        SerializedParameter.of("" + testValues.charList.get(1))
                )
        ));
        parameters.put("booleanList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.booleanList.get(0)),
                        SerializedParameter.of(testValues.booleanList.get(1))
                )
        ));
        parameters.put("stringList", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(testValues.stringList.get(0)),
                        SerializedParameter.of(testValues.stringList.get(1))
                )
        ));


        // Map Parameters
        Map.Entry<Double, Double>[] doubleMapEntries = testValues.doubleMap.entrySet().toArray(new Map.Entry[testValues.doubleMap.size()]);
        parameters.put("doubleMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(doubleMapEntries[0].getKey()), SerializedParameter.of(doubleMapEntries[1].getKey()))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(doubleMapEntries[0].getValue()), SerializedParameter.of(doubleMapEntries[1].getValue())))
                )
        ));
        Map.Entry<Float, Float>[] floatMapEntries = testValues.floatMap.entrySet().toArray(new Map.Entry[testValues.floatMap.size()]);
        parameters.put("floatMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(floatMapEntries[0].getKey()), SerializedParameter.of(floatMapEntries[1].getKey()))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(floatMapEntries[0].getValue()), SerializedParameter.of(floatMapEntries[1].getValue())))
                )
        ));
        Map.Entry<Byte, Byte>[] byteMapEntries = testValues.byteMap.entrySet().toArray(new Map.Entry[testValues.byteMap.size()]);
        parameters.put("byteMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(byteMapEntries[0].getKey()), SerializedParameter.of(byteMapEntries[1].getKey()))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(byteMapEntries[0].getValue()), SerializedParameter.of(byteMapEntries[1].getValue())))
                )
        ));
        Map.Entry<Short, Short>[] shortMapEntries = testValues.shortMap.entrySet().toArray(new Map.Entry[testValues.shortMap.size()]);
        parameters.put("shortMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(shortMapEntries[0].getKey()), SerializedParameter.of(shortMapEntries[1].getKey()))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(shortMapEntries[0].getValue()), SerializedParameter.of(shortMapEntries[1].getValue())))
                )
        ));
        Map.Entry<Integer, Integer>[] intMapEntries = testValues.intMap.entrySet().toArray(new Map.Entry[testValues.intMap.size()]);
        parameters.put("intMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(intMapEntries[0].getKey()), SerializedParameter.of(intMapEntries[1].getKey()))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(intMapEntries[0].getValue()), SerializedParameter.of(intMapEntries[1].getValue())))
                )
        ));
        Map.Entry<Long, Long>[] longMapEntries = testValues.longMap.entrySet().toArray(new Map.Entry[testValues.longMap.size()]);
        parameters.put("longMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(longMapEntries[0].getKey()), SerializedParameter.of(longMapEntries[1].getKey()))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(longMapEntries[0].getValue()), SerializedParameter.of(longMapEntries[1].getValue())))
                )
        ));
        Map.Entry<Character, Character>[] charMapEntries = testValues.charMap.entrySet().toArray(new Map.Entry[testValues.charMap.size()]);
        parameters.put("charMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of("" + charMapEntries[0].getKey()), SerializedParameter.of("" + charMapEntries[1].getKey()))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of("" + charMapEntries[0].getValue()), SerializedParameter.of("" + charMapEntries[1].getValue())))
                )
        ));
        Map.Entry<Boolean, Boolean>[] booleanMapEntries = testValues.booleanMap.entrySet().toArray(new Map.Entry[testValues.booleanMap.size()]);
        parameters.put("booleanMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(false), SerializedParameter.of(true))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(true), SerializedParameter.of(false)))
                )
        ));
        Map.Entry<String, String>[] stringMapEntries = testValues.stringMap.entrySet().toArray(new Map.Entry[testValues.stringMap.size()]);
        parameters.put("stringMap", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(List.of(SerializedParameter.of(stringMapEntries[0].getKey()), SerializedParameter.of(stringMapEntries[1].getKey()))),
                        "values", SerializedParameter.of(List.of(SerializedParameter.of(stringMapEntries[0].getValue()), SerializedParameter.of(stringMapEntries[1].getValue())))
                )
        ));

        // Complex Parameters

        Map.Entry<Integer, List<String>>[] mappyBoiEntries = testValues.mappyBoi.entrySet().toArray(new Map.Entry[testValues.mappyBoi.size()]);
        parameters.put("mappyBoi", SerializedParameter.of(
                Map.of(
                        "keys", SerializedParameter.of(
                                List.of(
                                        SerializedParameter.of(mappyBoiEntries[0].getKey()),
                                        SerializedParameter.of(mappyBoiEntries[1].getKey())
                                )
                        ),
                        "values", SerializedParameter.of(
                                List.of(
                                        SerializedParameter.of(List.of(
                                                SerializedParameter.of(mappyBoiEntries[0].getValue().get(0)),
                                                SerializedParameter.of(mappyBoiEntries[0].getValue().get(1))
                                        )),
                                        SerializedParameter.of(List.of(
                                                SerializedParameter.of(mappyBoiEntries[1].getValue().get(0)),
                                                SerializedParameter.of(mappyBoiEntries[1].getValue().get(1))
                                        ))
                                )
                        )
                )
        ));

        parameters.put("intListArrayArray", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(List.of(
                                SerializedParameter.of(List.of(
                                        SerializedParameter.of(testValues.intListArrayArray[0][0].get(0)),
                                        SerializedParameter.of(testValues.intListArrayArray[0][0].get(1))
                                )),
                                SerializedParameter.of(List.of(
                                        SerializedParameter.of(testValues.intListArrayArray[0][1].get(0)),
                                        SerializedParameter.of(testValues.intListArrayArray[0][1].get(1))
                                ))
                        )),
                        SerializedParameter.of(List.of(
                                SerializedParameter.of(List.of(
                                        SerializedParameter.of(testValues.intListArrayArray[1][0].get(0)),
                                        SerializedParameter.of(testValues.intListArrayArray[1][0].get(1))
                                )),
                                SerializedParameter.of(List.of(
                                        SerializedParameter.of(testValues.intListArrayArray[1][1].get(0)),
                                        SerializedParameter.of(testValues.intListArrayArray[1][1].get(1))
                                ))
                        ))
                )
        ));

        // Because obnixous is so obnoxious, we'll just hardcode it twice :(
        parameters.put("obnoxious", SerializedParameter.of(
                List.of(
                        SerializedParameter.of(Map.of(
                                "keys", SerializedParameter.of(List.of(
                                        SerializedParameter.of(List.of(
                                                SerializedParameter.of(List.of(
                                                        SerializedParameter.of("300"),
                                                        SerializedParameter.of("301")
                                                )),
                                                SerializedParameter.of(List.of(
                                                        SerializedParameter.of("302"),
                                                        SerializedParameter.of("303")
                                                ))
                                        )),
                                        SerializedParameter.of(List.of(
                                                SerializedParameter.of(List.of(
                                                        SerializedParameter.of("304"),
                                                        SerializedParameter.of("305")
                                                )),
                                                SerializedParameter.of(List.of(
                                                        SerializedParameter.of("306"),
                                                        SerializedParameter.of("307")
                                                ))
                                        )),
                                        SerializedParameter.of(List.of(
                                                SerializedParameter.of(List.of(
                                                        SerializedParameter.of("308"),
                                                        SerializedParameter.of("309")
                                                )),
                                                SerializedParameter.of(List.of(
                                                        SerializedParameter.of("310"),
                                                        SerializedParameter.of("311")
                                                ))
                                        )),
                                        SerializedParameter.of(List.of(
                                                SerializedParameter.of(List.of(
                                                        SerializedParameter.of("312"),
                                                        SerializedParameter.of("313")
                                                )),
                                                SerializedParameter.of(List.of(
                                                        SerializedParameter.of("314"),
                                                        SerializedParameter.of("315")
                                                ))
                                        ))
                                )),
                                "values", SerializedParameter.of(List.of(
                                        SerializedParameter.of(Map.of(
                                                "keys", SerializedParameter.of(List.of(
                                                        SerializedParameter.of(500),
                                                        SerializedParameter.of(501)
                                                )),
                                                "values", SerializedParameter.of(List.of(
                                                        SerializedParameter.of(List.of(
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(400.0f),
                                                                        SerializedParameter.of(401.0f)
                                                                )),
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(402.0f),
                                                                        SerializedParameter.of(403.0f)
                                                                ))
                                                        )),
                                                        SerializedParameter.of(List.of(
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(404.0f),
                                                                        SerializedParameter.of(405.0f)
                                                                )),
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(406.0f),
                                                                        SerializedParameter.of(407.0f)
                                                                ))
                                                        ))
                                                ))
                                        )),
                                        SerializedParameter.of(Map.of(
                                                "keys", SerializedParameter.of(List.of(
                                                        SerializedParameter.of(502),
                                                        SerializedParameter.of(503)
                                                )),
                                                "values", SerializedParameter.of(List.of(
                                                        SerializedParameter.of(List.of(
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(408.0f),
                                                                        SerializedParameter.of(409.0f)
                                                                )),
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(410.0f),
                                                                        SerializedParameter.of(411.0f)
                                                                ))
                                                        )),
                                                        SerializedParameter.of(List.of(
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(412.0f),
                                                                        SerializedParameter.of(413.0f)
                                                                )),
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(414.0f),
                                                                        SerializedParameter.of(415.0f)
                                                                ))
                                                        ))
                                                ))
                                        )),
                                        SerializedParameter.of(Map.of(
                                                "keys", SerializedParameter.of(List.of(
                                                        SerializedParameter.of(504),
                                                        SerializedParameter.of(505)
                                                )),
                                                "values", SerializedParameter.of(List.of(
                                                        SerializedParameter.of(List.of(
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(416.0f),
                                                                        SerializedParameter.of(417.0f)
                                                                )),
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(418.0f),
                                                                        SerializedParameter.of(419.0f)
                                                                ))
                                                        )),
                                                        SerializedParameter.of(List.of(
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(420.0f),
                                                                        SerializedParameter.of(421.0f)
                                                                )),
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(422.0f),
                                                                        SerializedParameter.of(423.0f)
                                                                ))
                                                        ))
                                                ))
                                        )),
                                        SerializedParameter.of(Map.of(
                                                "keys", SerializedParameter.of(List.of(
                                                        SerializedParameter.of(506),
                                                        SerializedParameter.of(507)
                                                )),
                                                "values", SerializedParameter.of(List.of(
                                                        SerializedParameter.of(List.of(
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(424.0f),
                                                                        SerializedParameter.of(425.0f)
                                                                )),
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(426.0f),
                                                                        SerializedParameter.of(427.0f)
                                                                ))
                                                        )),
                                                        SerializedParameter.of(List.of(
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(428.0f),
                                                                        SerializedParameter.of(429.0f)
                                                                )),
                                                                SerializedParameter.of(List.of(
                                                                        SerializedParameter.of(430.0f),
                                                                        SerializedParameter.of(431.0f)
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
        return ParameterTestActivity.createTestActivity();
    }
}
