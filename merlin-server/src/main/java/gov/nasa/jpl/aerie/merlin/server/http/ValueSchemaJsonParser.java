package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class ValueSchemaJsonParser implements JsonParser<ValueSchema> {
  public static final JsonParser<ValueSchema> valueSchemaP = new ValueSchemaJsonParser();

  @Override
  public JsonObject getSchema(final Map<Object, String> anchors) {
    // TODO: Figure out what this should be
    return Json.createObjectBuilder().add("type", "any").build();
  }

  @Override
  public JsonParseResult<ValueSchema> parse(final JsonValue json) {
    if (!json.getValueType().equals(JsonValue.ValueType.OBJECT)) return JsonParseResult.failure("Expected object");
    final var obj = json.asJsonObject();
    if (!obj.containsKey("type")) return JsonParseResult.failure("Expected field \"type\"");
    final var type = obj.get("type");
    if (!type.getValueType().equals(JsonValue.ValueType.STRING)) return JsonParseResult.failure("\"type\" field must be a string");

    return switch (obj.getString("type")) {
      case "real" -> JsonParseResult.success(ValueSchema.REAL);
      case "int" -> JsonParseResult.success(ValueSchema.INT);
      case "boolean" -> JsonParseResult.success(ValueSchema.BOOLEAN);
      case "string" -> JsonParseResult.success(ValueSchema.STRING);
      case "duration" -> JsonParseResult.success(ValueSchema.DURATION);
      case "path" -> JsonParseResult.success(ValueSchema.PATH);
      case "series" -> parseSeries(obj);
      case "struct" -> parseStruct(obj);
      case "variant" -> parseVariant(obj);
      default -> JsonParseResult.failure("Unrecognized value schema type");
    };
  }

  private JsonParseResult<ValueSchema> parseSeries(final JsonObject obj) {
    if (!obj.containsKey("items")) return JsonParseResult.failure("\"series\" value schema requires field \"items\"");
    return parse(obj.get("items")).mapSuccess(ValueSchema::ofSeries);
  }

  private JsonParseResult<ValueSchema> parseStruct(final JsonObject obj) {
    if (!obj.containsKey("items")) return JsonParseResult.failure("\"struct\" value schema requires field \"items\"");
    final var items = obj.get("items");
    if (!items.getValueType().equals(JsonValue.ValueType.OBJECT)) return JsonParseResult.failure("\"items\" field of \"struct\" must be an object");

    final var itemSchemas = new HashMap<String, ValueSchema>();
    for (final var entry : items.asJsonObject().entrySet()) {
      final var schema$ = parse(entry.getValue());
      if (schema$.isFailure()) return schema$;
      itemSchemas.put(entry.getKey(), schema$.getSuccessOrThrow());
    }

    return JsonParseResult.success(ValueSchema.ofStruct(itemSchemas));
  }

  private JsonParseResult<ValueSchema> parseVariant(final JsonObject obj) {
    if (!obj.containsKey("variants")) return JsonParseResult.failure("\"variant\" value schema requires field \"variants\"");
    final var variants = obj.get("variants");
    if (!variants.getValueType().equals(JsonValue.ValueType.ARRAY)) return JsonParseResult.failure("\"variants\" field of \"variant\" must be an array");

    final var options = new ArrayList<ValueSchema.Variant>();
    for (final var variant : variants.asJsonArray()) {
      if (!variant.getValueType().equals(JsonValue.ValueType.OBJECT)) return JsonParseResult.failure("Each option of a \"variant\" value schema must be an object");
      final var variantObj = variant.asJsonObject();

      if (!variantObj.containsKey("key")) return JsonParseResult.failure("Each option of a \"variant\" value schema must contain a \"key\" field");
      if (!variantObj.containsKey("label")) return JsonParseResult.failure("Each option of a \"variant\" value schema must contain a \"label\" field");
      final var key = variantObj.get("key");
      final var label = variantObj.get("label");

      if (!key.getValueType().equals(JsonValue.ValueType.STRING)) return JsonParseResult.failure("The \"key\" field of each option of a \"variant\" must be a string");
      if (!label.getValueType().equals(JsonValue.ValueType.STRING)) return JsonParseResult.failure("The \"label\" field of each option of a \"variant\" must be a string");
      options.add(new ValueSchema.Variant(key.toString(), label.toString()));
    }

    return JsonParseResult.success(ValueSchema.ofVariant(options));
  }

  @Override
  public JsonValue unparse(final ValueSchema value) {
    return ResponseSerializers.serializeValueSchema(value);
  }
}
