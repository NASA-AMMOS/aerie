package gov.nasa.jpl.aerie.merlin.protocol.types;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A serializable description of the structure of a value such as an activity parameter.
 *
 * For instance, if an activity accepts two parameters, each of which is a 3D point in space,
 * then the schema for each point will be a series of three real number primitives. The schema
 * for the activity itself will be a series of two instances of the schema for points.
 *
 * This is useful for providing information to mission-agnostic front-end applications, which
 * may (for instance) present a specialized UI for each primitive type, or a specialized means
 * of presenting trees of parameters.
 *
 * This class is implemented using the Visitor pattern, following the approach considered at
 * http://blog.higher-order.com/blog/2009/08/21/structural-pattern-matching-in-java/.
 */
// TODO: We will likely want to extend ValueSchema to support common semantic types
//   such as DateTime objects (which might otherwise be serialized as strings).
public abstract class ValueSchema {
  // Closed type family -- the only legal subclasses are those defined within the body of
  // this class definition.
  private ValueSchema() {}

  /**
   * Calls the appropriate method of the passed Visitor depending on the kind of data
   * contained by this object.
   *
   * @param visitor The operation to be performed on the data contained by this object.
   * @param <T> The return type produced by the visiting operation.
   * @return The result of calling `visitor.onX()`, where `X` depends on the kind of data
   *   contained in this object.
   */
  public abstract <T> T match(Visitor<T> visitor);

  /**
   * An operation to be performed over the schema described by a ValueSchema.
   *
   * A method must be defined for each kind of data that a ValueSchema may describe.
   * This may be likened to the pattern-matching capability built into languages such as Rust
   * or Haskell.
   *
   * Most clients will prefer to inherit from {@link DefaultVisitor}, which returns `Optional.empty()`
   * for any unimplemented methods.
   *
   * @param <T> The return type of the operation represented by this Visitor.
   */
  public interface Visitor<T> {
    T onReal();
    T onInt();
    T onBoolean();
    T onString();
    T onDuration();
    T onPath();
    T onSeries(ValueSchema value);
    T onStruct(Map<String, ValueSchema> value);
    T onVariant(List<Variant> variants);
  }

  /**
   * Creates a {@link ValueSchema} representing the domain of real numbers.
   *
   * @return A new {@link ValueSchema} representing the domain of real numbers.
   */
  private static ValueSchema ofReal() {
    return new ValueSchema() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onReal();
      }
    };
  }

  /**
   * Creates a {@link ValueSchema} representing the domain of integers.
   *
   * @return A new {@link ValueSchema} representing the domain of integers.
   */
  private static ValueSchema ofInt() {
    return new ValueSchema() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onInt();
      }
    };
  }

  /**
   * Creates a {@link ValueSchema} representing a {@link boolean} type.
   *
   * @return A new {@link ValueSchema} representing a {@link boolean} type.
   */
  private static ValueSchema ofBoolean() {
    return new ValueSchema() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onBoolean();
      }
    };
  }

  /**
   * Creates a {@link ValueSchema} representing a {@link String} type.
   *
   * @return A new {@link ValueSchema} representing a {@link String} type.
   */
  private static ValueSchema ofString() {
    return new ValueSchema() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onString();
      }
    };
  }

  /**
   * Creates a {@link ValueSchema} representing a {@link Duration} type.
   *
   * @return A new {@link ValueSchema} representing a {@link Duration} type.
   */
  private static ValueSchema ofDuration() {
    return new ValueSchema() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onDuration();
      }
    };
  }

  /**
   * Creates a {@link ValueSchema} representing a {@link Path} type.
   *
   * @return A new {@link ValueSchema} representing a {@link Path} type.
   */
  private static ValueSchema ofPath() {
    return new ValueSchema() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onPath();
      }
    };
  }

  /**
   * Creates a {@link ValueSchema} representing a homogeneous list of elements matching
   *   the provided {@link ValueSchema}s.
   *
   * @param value A {@link ValueSchema}.
   * @return A new {@link ValueSchema} representing a homogeneous list of elements.
   */
  public static ValueSchema ofSeries(final ValueSchema value) {
    Objects.requireNonNull(value);
    return new ValueSchema() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onSeries(value);
      }
    };
  }

  /**
   * Creates a {@link ValueSchema} representing a heterogeneous set of named elements matching
   *   the associated {@link ValueSchema}s.
   *
   * @param map Any set of named {@link ValueSchema}s.
   * @return A new {@link ValueSchema} representing a heterogeneous set of named {@link ValueSchema}s.
   */
  public static ValueSchema ofStruct(final Map<String, ValueSchema> map) {
    for (final var v : Objects.requireNonNull(map).values()) Objects.requireNonNull(v);
    final var value = Map.copyOf(map);
    return new ValueSchema() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onStruct(value);
      }
    };
  }

  /**
   * Creates a {@link ValueSchema} representing an {@link Enum} type.
   *
   * @return A new {@link ValueSchema} representing an {@link Enum} type.
   */
  public static ValueSchema ofVariant(final List<Variant> variants) {
    Objects.requireNonNull(variants);
    return new ValueSchema() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onVariant(variants);
      }
    };
  }

  public static final ValueSchema REAL = ofReal();
  public static final ValueSchema INT = ofInt();
  public static final ValueSchema BOOLEAN = ofBoolean();
  public static final ValueSchema STRING = ofString();
  public static final ValueSchema DURATION = ofDuration();
  public static final ValueSchema PATH = ofPath();

  /**
   * Provides a default case on top of the base Visitor.
   *
   * This interface routes all cases to the `onDefault` implementation by default. Each case may be overridden
   * independently to give distinct behavior.
   *
   * @param <T> The return type of the operation represented by this {@link Visitor}.
   */
  public static abstract class DefaultVisitor<T> implements Visitor<T> {
    protected abstract T onDefault();

    @Override
    public T onReal() {
      return this.onDefault();
    }

    @Override
    public T onInt() {
      return this.onDefault();
    }

    @Override
    public T onBoolean() {
      return this.onDefault();
    }

    @Override
    public T onDuration() {
      return this.onDefault();
    }

    @Override
    public T onString() {
      return this.onDefault();
    }

    @Override
    public T onPath() {
      return this.onDefault();
    }

    @Override
    public T onSeries(final ValueSchema value) {
      return this.onDefault();
    }

    @Override
    public T onStruct(final Map<String, ValueSchema> value) {
      return this.onDefault();
    }

    @Override
    public T onVariant(final List<Variant> variants) {
      return this.onDefault();
    }
  }

  /**
   * A helper base class implementing {@code Visitor<Optional<T>>} for any result type {@code T}.
   *
   * By default, all variants return {@code Optional.empty}.
   */
  public static class OptionalVisitor<T> extends DefaultVisitor<Optional<T>> {
    @Override
    public final Optional<T> onDefault() {
      return Optional.empty();
    }

    @Override
    public Optional<T> onVariant(final List<Variant> variants) {
      return onDefault();
    }
  }

  /**
   * Asserts that this object represents a real number.
   *
   * @return A non-empty {@link Optional} if this object represents a real number.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Unit> asReal() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<Unit> onReal() {
        return Optional.of(Unit.UNIT);
      }
    });
  }

  /**
   * Asserts that this object represents an integer.
   *
   * @return A non-empty {@link Optional} if this object represents an integer.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Unit> asInt() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<Unit> onInt() {
        return Optional.of(Unit.UNIT);
      }
    });
  }

  /**
   * Asserts that this object represents a boolean.
   *
   * @return A non-empty {@link Optional} if this object represents an boolean.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Unit> asBoolean() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<Unit> onBoolean() {
        return Optional.of(Unit.UNIT);
      }
    });
  }

  /**
   * Asserts that this object represents a String.
   *
   * @return A non-empty {@link Optional} if this object represents a String.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Unit> asString() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<Unit> onString() {
        return Optional.of(Unit.UNIT);
      }
    });
  }

  /**
   * Asserts that this object represents a Duration.
   *
   * @return A non-empty {@link Optional} if this object represents a Duration.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Unit> asDuration() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<Unit> onDuration() {
        return Optional.of(Unit.UNIT);
      }
    });
  }

  /**
   * Asserts that this object represents a Path.
   *
   * @return A non-empty {@link Optional} if this object represents a Path.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Unit> asPath() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<Unit> onPath() {
        return Optional.of(Unit.UNIT);
      }
    });
  }

  /**
   * Attempts to access this schema as a list of {@code ValueSchema}s.
   *
   * @return An {@link Optional} containing a schema for elements of a homogeneous list if this
   *   object represents a series. Otherwise, returns an empty {@link Optional}.
   */
  public Optional<ValueSchema> asSeries() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<ValueSchema> onSeries(final ValueSchema value) {
        return Optional.of(value);
      }
    });
  }

  /**
   * Attempts to access this schema as a map of named {@code ValueSchema}s.
   *
   * @return An {@link Optional} containing a map if this object represents a map.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Map<String, ValueSchema>> asStruct() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<Map<String, ValueSchema>> onStruct(final Map<String, ValueSchema> value) {
        return Optional.of(value);
      }
    });
  }

  /**
   * Asserts that this object represents an enumeration.
   *
   * @return An {@link Optional} containing an enum if this object represents an enumeration.
   *   Otherwise, returns an empty {@link Optional}
   */
  public Optional<List<Variant>> asVariant() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<List<Variant>> onVariant(final List<Variant> variants) {
        return Optional.of(variants);
      }
    });
  }

  @Override
  public String toString() {
    return this.match(new Visitor<>() {
      @Override
      public String onReal() {
        return "ValueSchema.REAL";
      }

      @Override
      public String onInt() {
        return "ValueSchema.INT";
      }

      @Override
      public String onBoolean() {
        return "ValueSchema.BOOLEAN";
      }

      @Override
      public String onString() {
        return "ValueSchema.STRING";
      }

      @Override
      public String onDuration() {
        return "ValueSchema.DURATION";
      }

      @Override
      public String onPath() {
        return "ValueSchema.PATH";
      }

      @Override
      public String onSeries(final ValueSchema value) {
        return "[" + value + "]";
      }

      @Override
      public String onStruct(final Map<String, ValueSchema> value) {
        return String.valueOf(value);
      }

      @Override
      public String onVariant(final List<Variant> variants) {
        return "ValueSchema.VARIANT(" + variants.toString() + ")";
      }
    });
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof ValueSchema)) return false;
    final var other = (ValueSchema) o;

    return this.match(new Visitor<>() {
      @Override
      public Boolean onReal() {
        return other.asReal().isPresent();
      }

      @Override
      public Boolean onInt() {
        return other.asInt().isPresent();
      }

      @Override
      public Boolean onBoolean() {
        return other.asBoolean().isPresent();
      }

      @Override
      public Boolean onString() {
        return other.asString().isPresent();
      }

      @Override
      public Boolean onDuration() {
        return other.asDuration().isPresent();
      }

      @Override
      public Boolean onPath() {
        return other.asPath().isPresent();
      }

      @Override
      public Boolean onSeries(final ValueSchema value) {
        return other.asSeries().map(x -> x.equals(value)).orElse(false);
      }

      @Override
      public Boolean onStruct(final Map<String, ValueSchema> value) {
        return other.asStruct().map(x -> x.equals(value)).orElse(false);
      }

      @Override
      public Boolean onVariant(final List<Variant> variants) {
        return other.asVariant().map(x -> x.equals(variants)).orElse(false);
      }
    });
  }

  /**
   * @param key The unique internal name of this variant
   * @param label The user-facing presentation of this variant
   */
  public record Variant(String key, String label) {}
}
