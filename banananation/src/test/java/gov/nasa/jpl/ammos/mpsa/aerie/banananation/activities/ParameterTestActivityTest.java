package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ParameterTestActivityTest {
    private final ParameterTestActivity$$ActivityMapper mapper;

    public ParameterTestActivityTest() {
        mapper = new ParameterTestActivity$$ActivityMapper();
    }

    @Test
    public void testDefaultSerializationDoesNotThrow() {
      this.mapper.serializeActivity(new ParameterTestActivity()).orElseThrow();
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
        assertArrayEquals(deserializedActivity.primDoubleArray, testValues.primDoubleArray, 0);
        assertArrayEquals(deserializedActivity.primFloatArray, testValues.primFloatArray, 0);
        assertArrayEquals(deserializedActivity.primByteArray, testValues.primByteArray);
        assertArrayEquals(deserializedActivity.primShortArray, testValues.primShortArray);
        assertArrayEquals(deserializedActivity.primIntArray, testValues.primIntArray);
        assertArrayEquals(deserializedActivity.primLongArray, testValues.primLongArray);
        assertArrayEquals(deserializedActivity.primCharArray, testValues.primCharArray);
        assertArrayEquals(deserializedActivity.primBooleanArray, testValues.primBooleanArray);
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
        assertEquals(deserializedActivity.testEnum, testValues.testEnum);
        assertEquals(deserializedActivity.mappyBoi, testValues.mappyBoi);
        assertArrayEquals(deserializedActivity.doublePrimIntArray, testValues.doublePrimIntArray);
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
        assertArrayEquals(sourceActivity.primDoubleArray, deserializedActivity.primDoubleArray, 0);
        assertArrayEquals(sourceActivity.primFloatArray, deserializedActivity.primFloatArray, 0);
        assertArrayEquals(sourceActivity.primByteArray, deserializedActivity.primByteArray);
        assertArrayEquals(sourceActivity.primShortArray, deserializedActivity.primShortArray);
        assertArrayEquals(sourceActivity.primIntArray, deserializedActivity.primIntArray);
        assertArrayEquals(sourceActivity.primLongArray, deserializedActivity.primLongArray);
        assertArrayEquals(sourceActivity.primCharArray, deserializedActivity.primCharArray);
        assertArrayEquals(sourceActivity.primBooleanArray, deserializedActivity.primBooleanArray);
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
        assertEquals(sourceActivity.testEnum, deserializedActivity.testEnum);
        assertEquals(sourceActivity.mappyBoi, deserializedActivity.mappyBoi);
        assertArrayEquals(sourceActivity.doublePrimIntArray, deserializedActivity.doublePrimIntArray);
        assertArrayEquals(sourceActivity.intListArrayArray, deserializedActivity.intListArrayArray);
        // TODO; Check equality for obnoxious (this is quite complex)
    }

    private SerializedActivity createSerializedInstance() {
        final ParameterTestActivity testValues = ParameterTestActivity.createTestActivity();
        final Map<String, SerializedValue> parameters = new HashMap<>();

        // Primitive parameters
        parameters.put("primitiveDouble", SerializedValue.of(testValues.primitiveDouble));
        parameters.put("primitiveFloat", SerializedValue.of(testValues.primitiveFloat));
        parameters.put("primitiveByte", SerializedValue.of(testValues.primitiveByte));
        parameters.put("primitiveShort", SerializedValue.of(testValues.primitiveShort));
        parameters.put("primitiveInt", SerializedValue.of(testValues.primitiveInt));
        parameters.put("primitiveLong", SerializedValue.of(testValues.primitiveLong));
        parameters.put("primitiveChar", SerializedValue.of("" + testValues.primitiveChar));
        parameters.put("primitiveBoolean", SerializedValue.of(testValues.primitiveBoolean));

        // Boxed parameters
        parameters.put("boxedDouble", SerializedValue.of(testValues.boxedDouble));
        parameters.put("boxedFloat", SerializedValue.of(testValues.boxedFloat));
        parameters.put("boxedByte", SerializedValue.of(testValues.boxedByte));
        parameters.put("boxedShort", SerializedValue.of(testValues.boxedShort));
        parameters.put("boxedInt", SerializedValue.of(testValues.boxedInt));
        parameters.put("boxedLong", SerializedValue.of(testValues.boxedLong));
        parameters.put("boxedChar", SerializedValue.of("" + testValues.boxedChar));
        parameters.put("boxedBoolean", SerializedValue.of(testValues.boxedBoolean));
        parameters.put("string", SerializedValue.of(testValues.string));

        // Array parameters
        parameters.put("doubleArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.doubleArray[0]),
                        SerializedValue.of(testValues.doubleArray[1])
                )
        ));
        parameters.put("floatArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.floatArray[0]),
                        SerializedValue.of(testValues.floatArray[1])
                )
        ));
        parameters.put("byteArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.byteArray[0]),
                        SerializedValue.of(testValues.byteArray[1])
                )
        ));
        parameters.put("shortArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.shortArray[0]),
                        SerializedValue.of(testValues.shortArray[1])
                )
        ));
        parameters.put("intArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.intArray[0]),
                        SerializedValue.of(testValues.intArray[1])
                )
        ));
        parameters.put("longArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.longArray[0]),
                        SerializedValue.of(testValues.longArray[1])
                )
        ));
        parameters.put("charArray", SerializedValue.of(
                List.of(
                        SerializedValue.of("" + testValues.charArray[0]),
                        SerializedValue.of("" + testValues.charArray[1])
                )
        ));
        parameters.put("booleanArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.booleanArray[0]),
                        SerializedValue.of(testValues.booleanArray[1])
                )
        ));
        parameters.put("stringArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.stringArray[0]),
                        SerializedValue.of(testValues.stringArray[1])
                )
        ));

        // Primitive array parameters
        parameters.put("primDoubleArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.primDoubleArray[0]),
                        SerializedValue.of(testValues.primDoubleArray[1])
                )
        ));
        parameters.put("primFloatArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.primFloatArray[0]),
                        SerializedValue.of(testValues.primFloatArray[1])
                )
        ));
        parameters.put("primByteArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.primByteArray[0]),
                        SerializedValue.of(testValues.primByteArray[1])
                )
        ));
        parameters.put("primShortArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.primShortArray[0]),
                        SerializedValue.of(testValues.primShortArray[1])
                )
        ));
        parameters.put("primIntArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.primIntArray[0]),
                        SerializedValue.of(testValues.primIntArray[1])
                )
        ));
        parameters.put("primLongArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.primLongArray[0]),
                        SerializedValue.of(testValues.primLongArray[1])
                )
        ));
        parameters.put("primCharArray", SerializedValue.of(
                List.of(
                        SerializedValue.of("" + testValues.primCharArray[0]),
                        SerializedValue.of("" + testValues.primCharArray[1])
                )
        ));
        parameters.put("primBooleanArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.primBooleanArray[0]),
                        SerializedValue.of(testValues.primBooleanArray[1])
                )
        ));

        // List parameters
        parameters.put("doubleList", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.doubleList.get(0)),
                        SerializedValue.of(testValues.doubleList.get(1))
                )
        ));
        parameters.put("floatList", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.floatList.get(0)),
                        SerializedValue.of(testValues.floatList.get(1))
                )
        ));
        parameters.put("byteList", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.byteList.get(0)),
                        SerializedValue.of(testValues.byteList.get(1))
                )
        ));
        parameters.put("shortList", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.shortList.get(0)),
                        SerializedValue.of(testValues.shortList.get(1))
                )
        ));
        parameters.put("intList", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.intList.get(0)),
                        SerializedValue.of(testValues.intList.get(1))
                )
        ));
        parameters.put("longList", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.longList.get(0)),
                        SerializedValue.of(testValues.longList.get(1))
                )
        ));
        parameters.put("charList", SerializedValue.of(
                List.of(
                        SerializedValue.of("" + testValues.charList.get(0)),
                        SerializedValue.of("" + testValues.charList.get(1))
                )
        ));
        parameters.put("booleanList", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.booleanList.get(0)),
                        SerializedValue.of(testValues.booleanList.get(1))
                )
        ));
        parameters.put("stringList", SerializedValue.of(
                List.of(
                        SerializedValue.of(testValues.stringList.get(0)),
                        SerializedValue.of(testValues.stringList.get(1))
                )
        ));


        // Map Parameters
        final var doubleMapEntries = entryArray(testValues.doubleMap);
        parameters.put("doubleMap", SerializedValue.of(
                Map.of(
                        "keys", SerializedValue.of(List.of(SerializedValue.of(doubleMapEntries[0].getKey()), SerializedValue.of(doubleMapEntries[1].getKey()))),
                        "values", SerializedValue.of(List.of(SerializedValue.of(doubleMapEntries[0].getValue()), SerializedValue.of(doubleMapEntries[1].getValue())))
                )
        ));
        final var floatMapEntries = entryArray(testValues.floatMap);
        parameters.put("floatMap", SerializedValue.of(
                Map.of(
                        "keys", SerializedValue.of(List.of(SerializedValue.of(floatMapEntries[0].getKey()), SerializedValue.of(floatMapEntries[1].getKey()))),
                        "values", SerializedValue.of(List.of(SerializedValue.of(floatMapEntries[0].getValue()), SerializedValue.of(floatMapEntries[1].getValue())))
                )
        ));
        final var byteMapEntries = entryArray(testValues.byteMap);
        parameters.put("byteMap", SerializedValue.of(
                Map.of(
                        "keys", SerializedValue.of(List.of(SerializedValue.of(byteMapEntries[0].getKey()), SerializedValue.of(byteMapEntries[1].getKey()))),
                        "values", SerializedValue.of(List.of(SerializedValue.of(byteMapEntries[0].getValue()), SerializedValue.of(byteMapEntries[1].getValue())))
                )
        ));
        final var shortMapEntries = entryArray(testValues.shortMap);
        parameters.put("shortMap", SerializedValue.of(
                Map.of(
                        "keys", SerializedValue.of(List.of(SerializedValue.of(shortMapEntries[0].getKey()), SerializedValue.of(shortMapEntries[1].getKey()))),
                        "values", SerializedValue.of(List.of(SerializedValue.of(shortMapEntries[0].getValue()), SerializedValue.of(shortMapEntries[1].getValue())))
                )
        ));
        final var intMapEntries = entryArray(testValues.intMap);
        parameters.put("intMap", SerializedValue.of(
                Map.of(
                        "keys", SerializedValue.of(List.of(SerializedValue.of(intMapEntries[0].getKey()), SerializedValue.of(intMapEntries[1].getKey()))),
                        "values", SerializedValue.of(List.of(SerializedValue.of(intMapEntries[0].getValue()), SerializedValue.of(intMapEntries[1].getValue())))
                )
        ));
        final var longMapEntries = entryArray(testValues.longMap);
        parameters.put("longMap", SerializedValue.of(
                Map.of(
                        "keys", SerializedValue.of(List.of(SerializedValue.of(longMapEntries[0].getKey()), SerializedValue.of(longMapEntries[1].getKey()))),
                        "values", SerializedValue.of(List.of(SerializedValue.of(longMapEntries[0].getValue()), SerializedValue.of(longMapEntries[1].getValue())))
                )
        ));
        final var charMapEntries = entryArray(testValues.charMap);
        parameters.put("charMap", SerializedValue.of(
                Map.of(
                        "keys", SerializedValue.of(List.of(SerializedValue.of("" + charMapEntries[0].getKey()), SerializedValue.of("" + charMapEntries[1].getKey()))),
                        "values", SerializedValue.of(List.of(SerializedValue.of("" + charMapEntries[0].getValue()), SerializedValue.of("" + charMapEntries[1].getValue())))
                )
        ));
        final var booleanMapEntries = entryArray(testValues.booleanMap);
        parameters.put("booleanMap", SerializedValue.of(
                Map.of(
                        "keys", SerializedValue.of(List.of(SerializedValue.of(booleanMapEntries[0].getKey()), SerializedValue.of(booleanMapEntries[1].getKey()))),
                        "values", SerializedValue.of(List.of(SerializedValue.of(booleanMapEntries[0].getValue()), SerializedValue.of(booleanMapEntries[1].getValue())))
                )
        ));
        final var stringMapEntries = entryArray(testValues.stringMap);
        parameters.put("stringMap", SerializedValue.of(
                Map.of(
                        "keys", SerializedValue.of(List.of(SerializedValue.of(stringMapEntries[0].getKey()), SerializedValue.of(stringMapEntries[1].getKey()))),
                        "values", SerializedValue.of(List.of(SerializedValue.of(stringMapEntries[0].getValue()), SerializedValue.of(stringMapEntries[1].getValue())))
                )
        ));

        // Enum Parameter
        parameters.put("testEnum", SerializedValue.of(testValues.testEnum.name()));

        // Complex Parameters

        final var mappyBoiEntries = entryArray(testValues.mappyBoi);
        parameters.put("mappyBoi", SerializedValue.of(
                Map.of(
                        "keys", SerializedValue.of(
                                List.of(
                                        SerializedValue.of(mappyBoiEntries[0].getKey()),
                                        SerializedValue.of(mappyBoiEntries[1].getKey())
                                )
                        ),
                        "values", SerializedValue.of(
                                List.of(
                                        SerializedValue.of(List.of(
                                                SerializedValue.of(mappyBoiEntries[0].getValue().get(0)),
                                                SerializedValue.of(mappyBoiEntries[0].getValue().get(1))
                                        )),
                                        SerializedValue.of(List.of(
                                                SerializedValue.of(mappyBoiEntries[1].getValue().get(0)),
                                                SerializedValue.of(mappyBoiEntries[1].getValue().get(1))
                                        ))
                                )
                        )
                )
        ));

        parameters.put("doublePrimIntArray", SerializedValue.of(
           List.of(
                   SerializedValue.of(List.of(
                           SerializedValue.of(testValues.doublePrimIntArray[0][0]),
                           SerializedValue.of(testValues.doublePrimIntArray[0][1])
                   )),
                   SerializedValue.of(List.of(
                           SerializedValue.of(testValues.doublePrimIntArray[1][0]),
                           SerializedValue.of(testValues.doublePrimIntArray[1][1])
                   ))
           )
        ));

        parameters.put("intListArrayArray", SerializedValue.of(
                List.of(
                        SerializedValue.of(List.of(
                                SerializedValue.of(List.of(
                                        SerializedValue.of(testValues.intListArrayArray[0][0].get(0)),
                                        SerializedValue.of(testValues.intListArrayArray[0][0].get(1))
                                )),
                                SerializedValue.of(List.of(
                                        SerializedValue.of(testValues.intListArrayArray[0][1].get(0)),
                                        SerializedValue.of(testValues.intListArrayArray[0][1].get(1))
                                ))
                        )),
                        SerializedValue.of(List.of(
                                SerializedValue.of(List.of(
                                        SerializedValue.of(testValues.intListArrayArray[1][0].get(0)),
                                        SerializedValue.of(testValues.intListArrayArray[1][0].get(1))
                                )),
                                SerializedValue.of(List.of(
                                        SerializedValue.of(testValues.intListArrayArray[1][1].get(0)),
                                        SerializedValue.of(testValues.intListArrayArray[1][1].get(1))
                                ))
                        ))
                )
        ));

        // Because obnixous is so obnoxious, we'll just hardcode it twice :(
        parameters.put("obnoxious", SerializedValue.of(
                List.of(
                        SerializedValue.of(Map.of(
                                "keys", SerializedValue.of(List.of(
                                        SerializedValue.of(List.of(
                                                SerializedValue.of(List.of(
                                                        SerializedValue.of("300"),
                                                        SerializedValue.of("301")
                                                )),
                                                SerializedValue.of(List.of(
                                                        SerializedValue.of("302"),
                                                        SerializedValue.of("303")
                                                ))
                                        )),
                                        SerializedValue.of(List.of(
                                                SerializedValue.of(List.of(
                                                        SerializedValue.of("304"),
                                                        SerializedValue.of("305")
                                                )),
                                                SerializedValue.of(List.of(
                                                        SerializedValue.of("306"),
                                                        SerializedValue.of("307")
                                                ))
                                        )),
                                        SerializedValue.of(List.of(
                                                SerializedValue.of(List.of(
                                                        SerializedValue.of("308"),
                                                        SerializedValue.of("309")
                                                )),
                                                SerializedValue.of(List.of(
                                                        SerializedValue.of("310"),
                                                        SerializedValue.of("311")
                                                ))
                                        )),
                                        SerializedValue.of(List.of(
                                                SerializedValue.of(List.of(
                                                        SerializedValue.of("312"),
                                                        SerializedValue.of("313")
                                                )),
                                                SerializedValue.of(List.of(
                                                        SerializedValue.of("314"),
                                                        SerializedValue.of("315")
                                                ))
                                        ))
                                )),
                                "values", SerializedValue.of(List.of(
                                        SerializedValue.of(Map.of(
                                                "keys", SerializedValue.of(List.of(
                                                        SerializedValue.of(500),
                                                        SerializedValue.of(501)
                                                )),
                                                "values", SerializedValue.of(List.of(
                                                        SerializedValue.of(List.of(
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(400.0f),
                                                                        SerializedValue.of(401.0f)
                                                                )),
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(402.0f),
                                                                        SerializedValue.of(403.0f)
                                                                ))
                                                        )),
                                                        SerializedValue.of(List.of(
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(404.0f),
                                                                        SerializedValue.of(405.0f)
                                                                )),
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(406.0f),
                                                                        SerializedValue.of(407.0f)
                                                                ))
                                                        ))
                                                ))
                                        )),
                                        SerializedValue.of(Map.of(
                                                "keys", SerializedValue.of(List.of(
                                                        SerializedValue.of(502),
                                                        SerializedValue.of(503)
                                                )),
                                                "values", SerializedValue.of(List.of(
                                                        SerializedValue.of(List.of(
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(408.0f),
                                                                        SerializedValue.of(409.0f)
                                                                )),
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(410.0f),
                                                                        SerializedValue.of(411.0f)
                                                                ))
                                                        )),
                                                        SerializedValue.of(List.of(
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(412.0f),
                                                                        SerializedValue.of(413.0f)
                                                                )),
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(414.0f),
                                                                        SerializedValue.of(415.0f)
                                                                ))
                                                        ))
                                                ))
                                        )),
                                        SerializedValue.of(Map.of(
                                                "keys", SerializedValue.of(List.of(
                                                        SerializedValue.of(504),
                                                        SerializedValue.of(505)
                                                )),
                                                "values", SerializedValue.of(List.of(
                                                        SerializedValue.of(List.of(
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(416.0f),
                                                                        SerializedValue.of(417.0f)
                                                                )),
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(418.0f),
                                                                        SerializedValue.of(419.0f)
                                                                ))
                                                        )),
                                                        SerializedValue.of(List.of(
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(420.0f),
                                                                        SerializedValue.of(421.0f)
                                                                )),
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(422.0f),
                                                                        SerializedValue.of(423.0f)
                                                                ))
                                                        ))
                                                ))
                                        )),
                                        SerializedValue.of(Map.of(
                                                "keys", SerializedValue.of(List.of(
                                                        SerializedValue.of(506),
                                                        SerializedValue.of(507)
                                                )),
                                                "values", SerializedValue.of(List.of(
                                                        SerializedValue.of(List.of(
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(424.0f),
                                                                        SerializedValue.of(425.0f)
                                                                )),
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(426.0f),
                                                                        SerializedValue.of(427.0f)
                                                                ))
                                                        )),
                                                        SerializedValue.of(List.of(
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(428.0f),
                                                                        SerializedValue.of(429.0f)
                                                                )),
                                                                SerializedValue.of(List.of(
                                                                        SerializedValue.of(430.0f),
                                                                        SerializedValue.of(431.0f)
                                                                ))
                                                        ))
                                                ))
                                        ))
                                ))
                        ))
                )
        ));

        return new SerializedActivity("ParameterTest", parameters);
    }

    private ParameterTestActivity createParameterTestActivityInstance() {
        return ParameterTestActivity.createTestActivity();
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map.Entry<K, V>[] entryArray(final Map<K, V> map) {
      return map.entrySet().toArray(new Map.Entry[0]);
    }
}
