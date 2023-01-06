package gov.nasa.jpl.aerie.merlin.driver.json;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JsonEncoding {
  public static JsonValue encode(final SerializedValue parameter) {
    return parameter.match(new SerializedValue.Visitor<>() {
      @Override
      public JsonValue onNull() {
        return JsonValue.NULL;
      }

      @Override
      public JsonValue onBoolean(final boolean value) {
        return (value) ? JsonValue.TRUE : JsonValue.FALSE;
      }

      @Override
      public JsonValue onNumeric(final BigDecimal value) {
        return Json.createValue(value);
      }

      @Override
      public JsonValue onString(final String value) {
        return Json.createValue(value);
      }

      @Override
      public JsonValue onList(final List<SerializedValue> elements) {
        final var builder = Json.createArrayBuilder();
        for (final var element : elements) builder.add(element.match(this));

        return builder.build();
      }

      @Override
      public JsonValue onMap(final Map<String, SerializedValue> fields) {
        final var builder = Json.createObjectBuilder();
        for (final var entry : fields.entrySet()) builder.add(entry.getKey(), entry.getValue().match(this));

        return builder.build();
      }
    });
  }

  public static SerializedValue decode(final JsonValue value) {
    switch (value.getValueType()) {
      case NULL: return SerializedValue.NULL;
      case TRUE: return SerializedValue.of(true);
      case FALSE: return SerializedValue.of(false);
      case NUMBER:
        return SerializedValue.of(((JsonNumber) value).bigDecimalValue());
      case STRING:
        return SerializedValue.of(((JsonString) value).getString());
      case ARRAY: {
        final var elements = new ArrayList<SerializedValue>(((JsonArray) value).size());
        for (final var element : (JsonArray) value) {
          elements.add(decode(element));
        }
        return SerializedValue.of(elements);
      }
      case OBJECT: {
        final var fields = new HashMap<String, SerializedValue>(((JsonObject) value).size());
        for (final var field : ((JsonObject) value).entrySet()) {
          fields.put(field.getKey(), decode(field.getValue()));
        }
        return SerializedValue.of(fields);
      }
      default:
        throw new Error("Unknown type of JSON value: " + value.getValueType());
    }
  }
}
