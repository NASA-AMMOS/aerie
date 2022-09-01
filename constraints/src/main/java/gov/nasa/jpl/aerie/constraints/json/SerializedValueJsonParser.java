package gov.nasa.jpl.aerie.constraints.json;

import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.SchemaCache;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SerializedValueJsonParser implements JsonParser<SerializedValue> {
  public static final JsonParser<SerializedValue> serializedValueP = new SerializedValueJsonParser();

  @Override
  public JsonObject getSchema(final SchemaCache anchors) {
    return Json.createObjectBuilder().add("type", "any").build();
  }

  @Override
  public JsonParseResult<SerializedValue> parse(final JsonValue json) {
    return JsonParseResult.success(this.parseInfallible(json));
  }

  private SerializedValue parseInfallible(final JsonValue value) {
    return switch (value.getValueType()) {
      case NULL -> SerializedValue.NULL;
      case TRUE -> SerializedValue.of(true);
      case FALSE -> SerializedValue.of(false);
      case STRING -> SerializedValue.of(((JsonString) value).getString());
      case NUMBER -> {
        final var num = (JsonNumber) value;
        yield (num.isIntegral())
            ? SerializedValue.of(num.longValue())
            : SerializedValue.of(num.doubleValue());
      }
      case ARRAY -> {
        final var arr = (JsonArray) value;
        final var list = new ArrayList<SerializedValue>(arr.size());
        for (final var element : arr) list.add(this.parseInfallible(element));
        yield SerializedValue.of(list);
      }
      case OBJECT -> {
        final var obj = (JsonObject) value;
        final var map = new HashMap<String, SerializedValue>(obj.size());
        for (final var entry : obj.entrySet()) map.put(entry.getKey(), this.parseInfallible(entry.getValue()));
        yield SerializedValue.of(map);
      }
    };
  }

  @Override
  public JsonValue unparse(final SerializedValue value) {
    return value.match(new SerializedValue.Visitor<>() {
      @Override
      public JsonValue onNull() {
        return JsonValue.NULL;
      }

      @Override
      public JsonValue onBoolean(final boolean value) {
        return (value) ? JsonValue.TRUE : JsonValue.FALSE;
      }

      @Override
      public JsonValue onReal(final double value) {
        return Json.createValue(value);
      }

      @Override
      public JsonValue onInt(final long value) {
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
}
