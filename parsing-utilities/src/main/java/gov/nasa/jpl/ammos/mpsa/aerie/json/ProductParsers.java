package gov.nasa.jpl.ammos.mpsa.aerie.json;

import org.apache.commons.lang3.tuple.Pair;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

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
        if (Objects.equals(field.name, fieldSpec.name)) throw new RuntimeException("Parser already defined for key `" + fieldSpec.name + "`");
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

      // Extract all of the fields we want.
      final var iter = this.fields.iterator();
      Object accumulator = null;
      {
        final var field = iter.next();

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

        if (result.isFailure()) {
          return JsonParseResult.failure(
              result
              .failureReason()
              .prependBreadcrumb(
                  Breadcrumb.ofString(field.name)
              ));
        }
        accumulator = result.getSuccessOrThrow();
      }

      // PRECONDITION: accumulator is of type T1
      // INVARIANT: accumulator is of type Pair<...Pair<T1, T2>..., Ti>
      // where `i` is the number of fields iterated through.
      // POSTCONDITION: accumulator is of type T = Pair<...Pair<Pair<T1, T2>, T3>..., Tn>.
      long defaultedFields = 0;
      while (iter.hasNext()) {
        final var field = iter.next();

        final JsonParseResult<?> result;
        if (field.isOptional) {
          if (!obj.containsKey(field.name)) {
            defaultedFields += 1;
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

        if (result.isFailure()) {
          return JsonParseResult.failure(
              result
              .failureReason()
              .prependBreadcrumb(
                  Breadcrumb.ofString(field.name)
              ));
        }
        accumulator = Pair.of(accumulator, result.getSuccessOrThrow());
      }

      // Check that it contains only the fields we want.
      // TODO: We should take note of what extra fields are present and report them
      //       As a temporary workaround error if more than the total number of fields are present
      if (obj.keySet().size() + defaultedFields > this.fields.size()) return JsonParseResult.failure("unexpected extra fields present");

      // SAFETY: established by loop invariant.
      @SuppressWarnings("unchecked")
      final var result = (T) accumulator;
      return JsonParseResult.success(result);
    }

    public <S> VariadicProductParser<Pair<T, S>> field(final String key, final JsonParser<S> valueParser) {
      return new VariadicProductParser<>(this.fields, new FieldSpec<>(key, valueParser, false));
    }

    public <S> VariadicProductParser<Pair<T, Optional<S>>> optionalField(final String key, final JsonParser<S> valueParser) {
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
