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

  // CONTRACT: T must be of the form Pair<...Pair<Pair<T1, T2>, T3>..., Tn>,
  //   where `n == fields.size()`.
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

      int acceptedFields = 0;
      JsonParseResult<?> accumulator;
      {
        // SAFETY: This iterator is not empty. If it was, we'd be in EmptyProductParser.parse().
        final var field = iter.next();

        final var result = parseField(field, obj);

        acceptedFields += result.getLeft();
        accumulator = result.getRight().prependBreadcrumb(Breadcrumb.ofString(field.name));
      }

      // PRECONDITION: accumulated type is T1
      // INVARIANT: accumulated type is Pair<...Pair<T1, T2>..., Ti>
      // where `i` is the number of fields iterated through.
      // POSTCONDITION: accumulated type is T = Pair<...Pair<Pair<T1, T2>, T3>..., Tn>.
      while (iter.hasNext()) {
        final var field = iter.next();

        final var result = parseField(field, obj);

        acceptedFields += result.getLeft();
        accumulator = accumulator.parWith(result.getRight().prependBreadcrumb(Breadcrumb.ofString(field.name)));
      }

      // Check that it contains only the fields we want.
      // TODO: We should take note of what extra fields are present and report them.
      if (acceptedFields != obj.keySet().size()) {
        accumulator = accumulator.parWith(JsonParseResult.failure("unexpected extra fields present"), (acc, $) -> acc);
      }

      // SAFETY: established by loop invariant.
      @SuppressWarnings("unchecked")
      final var typedAccumulator$ = (JsonParseResult<T>) accumulator;

      return typedAccumulator$;
    }

    // Returns:
    //   (1, success) if the field was present and could be parsed,
    //   (1, failure) if the field was present and could not be parsed,
    //   (1, success) if the field was not present but is optional,
    //   (0, failure) if the field was not present and was not optional.
    // Note that (0, success) is not a possible result.
    private Pair<Integer, JsonParseResult<?>> parseField(final FieldSpec<?> field, final JsonObject obj) {
      if (obj.containsKey(field.name)) {
        final var parser = (field.isOptional)
            ? field.valueParser.map(Optional::of)
            : field.valueParser;
        return Pair.of(1, parser.parse(obj.get(field.name)));
      } else {
        if (field.isOptional) {
          return Pair.of(1, JsonParseResult.success(Optional.empty()));
        } else {
          return Pair.of(0, JsonParseResult.failure("required field not present"));
        }
      }
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
