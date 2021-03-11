package gov.nasa.jpl.aerie.json;

import gov.nasa.jpl.aerie.json.ProductParsers.EmptyProductParser;

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
import java.util.Objects;
import java.util.function.Function;

public abstract class BasicParsers {
  private BasicParsers() {}


  public static final JsonParser<JsonValue> anyP = json ->
      JsonParseResult.success(json);

  public static final JsonParser<Boolean> boolP = json -> {
    if (Objects.equals(json, JsonValue.TRUE)) return JsonParseResult.success(true);
    if (Objects.equals(json, JsonValue.FALSE)) return JsonParseResult.success(false);
    return JsonParseResult.failure("expected boolean");
  };

  public static final JsonParser<String> stringP = json -> {
    if (!(json instanceof JsonString)) return JsonParseResult.failure("expected string");

    return JsonParseResult.success(((JsonString) json).getString());
  };

  public static final JsonParser<Long> longP = json -> {
    if (!(json instanceof JsonNumber)) return JsonParseResult.failure("expected long");
    if (!((JsonNumber) json).isIntegral()) return JsonParseResult.failure("expected integral number");

    return JsonParseResult.success(((JsonNumber) json).longValue());
  };

  public static final JsonParser<Double> doubleP = json -> {
    if (!(json instanceof JsonNumber)) return JsonParseResult.failure("expected double");

    return JsonParseResult.success(((JsonNumber) json).doubleValue());
  };

  public static final JsonParser<Long> nullP = json -> {
    if (!Objects.equals(json, JsonValue.NULL)) return JsonParseResult.failure("expected null");

    return JsonParseResult.success(null);
  };


  public static JsonParser<String> literalP(final String x) {
    return json -> {
      if (!Objects.equals(json, Json.createValue(x))) return JsonParseResult.failure("string literal does not match expected value: \"" + x + "\"");

      return JsonParseResult.success(x);
    };
  }

  public static JsonParser<Long> literalP(final long x) {
    return json -> {
      if (!Objects.equals(json, Json.createValue(x))) return JsonParseResult.failure("long literal does not match expected value: \"" + x + "\"");

      return JsonParseResult.success(x);
    };
  }

  public static JsonParser<Double> literalP(final double x) {
    return json -> {
      if (!Objects.equals(json, Json.createValue(x))) return JsonParseResult.failure("double literal does not match expected value: \"" + x + "\"");

      return JsonParseResult.success(x);
    };
  }

  public static JsonParser<Boolean> literalP(final boolean x) {
    return json -> {
      if (!Objects.equals(json, x ? JsonValue.TRUE : JsonValue.FALSE)) return JsonParseResult.failure("boolean literal does not match expected value: \"" + x + "\"");

      return JsonParseResult.success(x);
    };
  }

  public static <T> JsonParser<List<T>> listP(final JsonParser<T> elementParser) {
    return json -> {
      if (!(json instanceof JsonArray)) return JsonParseResult.failure("expected list");

      final var jsonArray = json.asJsonArray();
      final var list = new ArrayList<T>(jsonArray.size());
      for (int index=0; index < jsonArray.size(); index++) {
        final var element = jsonArray.get(index);
        final var result = elementParser.parse(element);

        if (result.isFailure()) {
          return JsonParseResult.failure(
              result
              .failureReason()
              .prependBreadcrumb(
                  Breadcrumb.ofInteger(index)
              ));
        }
        list.add(result.getSuccessOrThrow());
      }

      return JsonParseResult.success(list);
    };
  }

  public static <S> JsonParser<Map<String, S>> mapP(final JsonParser<S> fieldParser) {
    return json -> {
      if (!(json instanceof JsonObject)) return JsonParseResult.failure("expected object");

      final var map = new HashMap<String, S>(json.asJsonObject().size());
      for (final var field : json.asJsonObject().entrySet()) {
        final var result = fieldParser.parse(field.getValue());

        if (result.isFailure()) {
          return JsonParseResult.failure(
              result
              .failureReason()
              .prependBreadcrumb(
                  Breadcrumb.ofString(field.getKey())
              ));
        }
        map.put(field.getKey(), result.getSuccessOrThrow());
      }

      return JsonParseResult.success(map);
    };
  }

  public static <S> JsonParser<S> recursiveP(final Function<JsonParser<S>, JsonParser<S>> scope) {
    final var proxy = new JsonParser<S>() {
      JsonParser<S> target = null;

      @Override
      public JsonParseResult<S> parse(final JsonValue json) {
        return this.target.parse(json);
      }
    };

    proxy.target = scope.apply(proxy);
    return proxy.target;
  }

  @SafeVarargs
  public static <T> JsonParser<T> chooseP(final JsonParser<? extends T>... options) {
    return json -> {
      for (final var option : options) {
        final var result = option.parse(json);
        if (result.isFailure()) continue;
        return result.mapSuccess(x -> (T) x);
      }

      return JsonParseResult.failure("not parsable into acceptable type");
    };
  }

  public static <T> SumParsers.VariantJsonParser<T> sumP() {
    return SumParsers.sumP();
  }

  public static EmptyProductParser productP = ProductParsers.productP;
}
