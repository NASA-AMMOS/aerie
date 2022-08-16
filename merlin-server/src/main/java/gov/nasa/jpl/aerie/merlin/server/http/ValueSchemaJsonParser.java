package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.HashMap;
import java.util.Map;

import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.literalP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.ProductParsers.productP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;

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
    final JsonParser<ValueSchema.Variant> variantP =
        productP
            .field("key", stringP)
            .field("label", stringP)
            .map(Iso.of(untuple(ValueSchema.Variant::new),
                        $ -> tuple($.key(), $.label())));
    final JsonParser<ValueSchema> variantsP =
        productP
            .field("type", literalP("variant"))
            .field("variants", listP(variantP))
            .map(Iso.of(untuple((type, variants) -> ValueSchema.ofVariant(variants)),
                        $ -> tuple(Unit.UNIT, $.asVariant().get())));

    return variantsP.parse(obj);
  }

  @Override
  public JsonValue unparse(final ValueSchema value) {
    return ResponseSerializers.serializeValueSchema(value);
  }

}
