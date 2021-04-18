package gov.nasa.jpl.aerie.json;

import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static gov.nasa.jpl.aerie.json.BasicParsers.getCachedSchema;

public abstract class ProductParsers {
  private ProductParsers() {}

  public static final EmptyProductParser productP = new EmptyProductParser();


  public static final class EmptyProductParser implements JsonParser<Unit> {
    private EmptyProductParser() {}

    @Override
    public JsonParseResult<Unit> parse(final JsonValue json) {
      if (!(json instanceof JsonObject)) return JsonParseResult.failure("expected object");
      if (!json.asJsonObject().isEmpty()) return JsonParseResult.failure("expected empty object");

      return JsonParseResult.success(Unit.UNIT);
    }

    @Override
    public JsonObject getSchema(final Map<Object, String> anchors) {
      return Json
          .createObjectBuilder()
          .add("type", "object")
          .add("additionalProperties", JsonValue.FALSE)
          .build();
    }

    public <S> VariadicProductParser<S> field(final String key, final JsonParser<S> valueParser) {
      return new VariadicProductParser<>(new FieldSpec<>(key, valueParser, false));
    }

    public <S> VariadicProductParser<Optional<S>> optionalField(final String key, final JsonParser<S> valueParser) {
      return new VariadicProductParser<>(new FieldSpec<>(key, valueParser, true));
    }
  }

  // CONTRACT: T must be of the form Pair<...Pair<Pair<T1, T2>, T3>..., Tn>.
  public static final class VariadicProductParser<T> implements JsonParser<T> {
    private final ArrayList<FieldSpec<?>> fields;

    private VariadicProductParser(final FieldSpec<?> fieldSpec) {
      this.fields = new ArrayList<>();
      this.fields.add(fieldSpec);
    }

    private VariadicProductParser(final ArrayList<FieldSpec<?>> fields, final FieldSpec<?> fieldSpec) {
      for (final var field : fields) {
        if (Objects.equals(field.name, fieldSpec.name)) {
          throw new RuntimeException("Parser already defined for key `" + fieldSpec.name + "`");
        }
      }

      // It's pretty inefficient to copy the set of field specs every time.
      // We don't expect our sets to grow very large, so this probably isn't a problem.
      // In the future, we can use a persistent collections library to efficiently append.
      this.fields = new ArrayList<>(fields);
      this.fields.add(fieldSpec);
    }

    @Override
    public JsonParseResult<T> parse(final JsonValue json) {
      if (!(json instanceof JsonObject)) return JsonParseResult.failure("expected object");
      final var obj = (JsonObject) json;

      // Detect unexpected fields in the json
      // TODO: We should return all unexpected fields, but currently
      //       we can only return one failure reason, with one set of
      //       breadcrumbs. When we allow multiple failure reasons
      //       this should be updated to build a failure reason for
      //       each unexpected parameter provided
      for (final var param : obj.entrySet()) {
        final var name = param.getKey();

        if (getFieldSpec(name).isEmpty()) {
          return JsonParseResult
              .<T>failure("Unexpected field present")
              .prependBreadcrumb(
                  Breadcrumb.ofString(name)
              );
        }
      }

      // Parse the fields
      //
      // PRECONDITION: accumulated result is of type T1
      // INVARIANT: accumulated result is of type Pair<...Pair<T1, T2>..., Ti>
      //   where `i` is the number of fields iterated through.
      // POSTCONDITION: accumulated result is of type T = Pair<...Pair<Pair<T1, T2>, T3>..., Tn>.
      final var iter = this.fields.iterator();
      var accumulator = parseField(iter.next(), obj);
      while (iter.hasNext()) {
        accumulator = accumulator.parWith(parseField(iter.next(), obj)).mapSuccess(x -> x);
      }

      return accumulator.mapSuccess(result -> {
        // SAFETY: established by loop invariant.
        @SuppressWarnings("unchecked")
        final var tmp = (T) result;
        return tmp;
      });
    }

    @Override
    public JsonObject getSchema(final Map<Object, String> anchors) {
      final var fieldSchemas = Json.createObjectBuilder();
      for (final var field : this.fields) {
        fieldSchemas.add(field.name, getCachedSchema(anchors, field.valueParser));
      }

      final var requiredFields = Json.createArrayBuilder();
      for (final var field : this.fields) {
        if (!field.isOptional) requiredFields.add(field.name);
      }

      return Json
          .createObjectBuilder()
          // an object containing all and only the listed properties
          .add("type", "object")
          .add("properties", fieldSchemas)
          .add("required", requiredFields)  // all
          .add("additionalProperties", JsonValue.FALSE)  // only
          .build();
    }

    private Optional<FieldSpec<?>> getFieldSpec(final String name) {
      for (final var field : this.fields) {
        if (field.name.equals(name)) return Optional.of(field);
      }
      return Optional.empty();
    }

    private static JsonParseResult<?> parseField(final FieldSpec<?> field, final JsonObject obj) {
      final JsonParseResult<?> result;
      if (field.isOptional) {
        if (!obj.containsKey(field.name)) {
          result = JsonParseResult.success(Optional.empty());
        } else {
          result = field.valueParser.map(Optional::of).parse(obj.get(field.name));
        }
      } else {
        if (!obj.containsKey(field.name)) {
          result = JsonParseResult.failure("required field not present");
        } else {
          result = field.valueParser.parse(obj.get(field.name));
        }
      }

      return result.prependBreadcrumb(Breadcrumb.ofString(field.name));
    }

    public <S> VariadicProductParser<Pair<T, S>> field(final String key, final JsonParser<S> valueParser) {
      return new VariadicProductParser<>(this.fields, new FieldSpec<>(key, valueParser, false));
    }

    public <S> VariadicProductParser<Pair<T, Optional<S>>> optionalField(
        final String key,
        final JsonParser<S> valueParser)
    {
      return new VariadicProductParser<>(this.fields, new FieldSpec<>(key, valueParser, true));
    }
  }

  private static final class FieldSpec<S> {
    public final String name;
    public final JsonParser<S> valueParser;
    public final boolean isOptional;

    public FieldSpec(final String name, final JsonParser<S> valueParser, final boolean isOptional) {
      this.name = name;
      this.valueParser = valueParser;
      this.isOptional = isOptional;
    }
  }
}
