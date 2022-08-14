package gov.nasa.jpl.aerie.json;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Objects;

public interface JsonParser<T> {
  JsonObject getSchema(SchemaCache anchors);
  JsonParseResult<T> parse(JsonValue json);
  JsonValue unparse(T value);

  default JsonObject getSchema() {
    return Json
        .createObjectBuilder()
        .add("$schema", Json.createValue("https://json-schema.org/draft/2020-12/schema"))
        .addAll(Json.createObjectBuilder(this.getSchema(new SchemaCache())))
        .build();
  }

  default <S> JsonParser<S> map(final Iso<T, S> transform) {
    Objects.requireNonNull(transform);

    final var self = this;

    return new JsonParser<>() {
      @Override
      public JsonObject getSchema(final SchemaCache anchors) {
        return self.getSchema(anchors);
      }

      @Override
      public JsonParseResult<S> parse(final JsonValue json) {
        return self.parse(json).mapSuccess(transform::from);
      }

      @Override
      public JsonValue unparse(final S value) {
        return self.unparse(transform.to(value));
      }
    };
  }
}
