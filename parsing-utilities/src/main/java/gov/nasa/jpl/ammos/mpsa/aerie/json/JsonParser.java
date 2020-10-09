package gov.nasa.jpl.ammos.mpsa.aerie.json;

import javax.json.JsonValue;
import java.util.function.Function;

public interface JsonParser<T> {
  JsonParseResult<T> parse(JsonValue json);

  default <S> JsonParser<S> map(final Function<T, S> transform) {
    return json -> this.parse(json).mapSuccess(transform);
  }

  /**
   * Process the result of this parser, potentially failing.
   *
   * Note that this {@code andThen} is not the monadic bind, since it returns a {@code JsonParseResult},
   * *not* a {@code JsonParser}. Thus, it doesn't harm the applicative guarantees we want to achieve
   * (namely syntactic transparency of any given JSON parser).
   *
   * (I mean, you could still replace any parser {@code p} with {@code anyP.andThen(p::parse)},
   * so... like, don't do that? That's just sticking an extra no-op stage in front of everything.)
   */
  default <S> JsonParser<S> andThen(final Function<T, JsonParseResult<S>> transform) {
    return json -> this.parse(json).andThen(transform);
  }
}
