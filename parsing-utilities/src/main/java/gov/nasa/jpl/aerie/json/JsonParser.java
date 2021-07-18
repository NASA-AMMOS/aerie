package gov.nasa.jpl.aerie.json;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public interface JsonParser<T> {
  JsonParseResult<T> parse(JsonValue json);
  JsonObject getSchema(Map<Object, String> anchors);

  default JsonObject getSchema() {
    return Json
        .createObjectBuilder()
        .add("$schema", Json.createValue("https://json-schema.org/draft/2020-12/schema"))
        .addAll(Json.createObjectBuilder(this.getSchema(new HashMap<>())))
        .build();
  }

  default <S> JsonParser<S> map(final Function<T, S> transform) {
    Objects.requireNonNull(transform);

    final var that = this;

    return new JsonParser<>() {
      @Override
      public JsonParseResult<S> parse(final JsonValue json) {
        return that.parse(json).mapSuccess(transform);
      }

      @Override
      public JsonObject getSchema(final Map<Object, String> anchors) {
        return that.getSchema(anchors);
      }
    };
  }
}
