package gov.nasa.jpl.aerie.json;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Objects;

public interface JsonObjectParser<T> extends JsonParser<T> {
  @Override
  JsonObject unparse(final T value);

  @Override
  default <S> JsonObjectParser<S> map(final Iso<T, S> transform) {
    Objects.requireNonNull(transform);

    final var self = this;

    return new JsonObjectParser<>() {
      @Override
      public JsonObject getSchema(final SchemaCache anchors) {
        return self.getSchema(anchors);
      }

      @Override
      public JsonParseResult<S> parse(final JsonValue json) {
        return self.parse(json).mapSuccess(transform::from);
      }

      @Override
      public JsonObject unparse(final S value) {
        return self.unparse(transform.to(value));
      }
    };
  }
}
