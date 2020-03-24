package gov.nasa.jpl.ammos.mpsa.aerie.json;

import javax.json.JsonValue;
import java.util.function.Function;

public interface JsonParser<T> {
  JsonParseResult<T> parse(JsonValue json);

  default <S> JsonParser<S> map(Function<T, S> transform) {
    return json -> this.parse(json).mapSuccess(transform);
  }
}
