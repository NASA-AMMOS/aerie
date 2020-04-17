package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ParameterType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A serializable description of the structure of an activity parameter.
 *
 * Implementors of the {@link ActivityType} protocol may be constructed from parameters
 * (which are themselves implementors of the {@link ParameterType} protocol). A {@link ParameterSchema}
 * is an adaptation-agnostic representation of the serialized structure of a parameter.
 *
 * For instance, if an activity accepts two parameters, each of which is a 3D point in space,
 * then the schema for each point will be a sequence of three real number primitives. The schema
 * for the activity itself will be a sequence of two instances of the schema for points.
 *
 * This is useful for providing information to mission-agnostic front-end applications, which
 * may (for instance) present a specialized UI for each primitive type, or a specialized means
 * of presenting trees of parameters.
 *
 * This class is implemented using the Visitor pattern, following the approach considered at
 * http://blog.higher-order.com/blog/2009/08/21/structural-pattern-matching-in-java/.
 */
// TODO: We will likely want to extend ParameterSchema to support common semantic types
//   such as DateTime objects (which might otherwise be serialized as strings).
public abstract class ParameterSchema {
  // Closed type family -- the only legal subclasses are those defined within the body of
  // this class definition.
  private ParameterSchema() {}

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
   * An operation to be performed over the schema described by a ParameterSchema.
   *
   * A method must be defined for each kind of data that a ParameterSchema may describe.
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
    T onList(ParameterSchema value);
    T onMap(Map<String, ParameterSchema> value);
    T onEnum(Class<? extends Enum<?>> enumeration);
  }

  /**
   * Creates a {@link ParameterSchema} representing a real number parameter type.
   *
   * @return A new {@link ParameterSchema} representing a real number parameter type.
   */
  public static ParameterSchema ofReal() {
    return new ParameterSchema() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onReal();
      }
      public String toString() {
        return "ParameterSchema.REAL";
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof ParameterSchema) && ((ParameterSchema)other).asReal().isPresent());
      }
    };
  }

  @Deprecated(forRemoval = true)
  public static ParameterSchema ofDouble() { return ofReal(); }

  /**
   * Creates a {@link ParameterSchema} representing an integral number parameter type.
   *
   * @return A new {@link ParameterSchema} representing an integral number parameter type.
   */
  public static ParameterSchema ofInt() {
    return new ParameterSchema() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onInt();
      }
      public String toString() {
        return "ParameterSchema.INT";
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof ParameterSchema) && ((ParameterSchema)other).asInt().isPresent());
      }
    };
  }

  /**
   * Creates a {@link ParameterSchema} representing a {@link boolean} parameter type.
   *
   * @return A new {@link ParameterSchema} representing a {@link boolean} parameter type.
   */
  public static ParameterSchema ofBoolean() {
    return new ParameterSchema() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onBoolean();
      }
      public String toString() {
        return "ParameterSchema.BOOLEAN";
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof ParameterSchema) && ((ParameterSchema)other).asBoolean().isPresent());
      }
    };
  }

  /**
   * Creates a {@link ParameterSchema} representing a {@link String} parameter type.
   *
   * @return A new {@link ParameterSchema} representing a {@link String} parameter type.
   */
  public static ParameterSchema ofString() {
    return new ParameterSchema() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onString();
      }
      public String toString() {
        return "ParameterSchema.STRING";
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof ParameterSchema) && ((ParameterSchema)other).asString().isPresent());
      }
    };
  }

  /**
   * Creates a {@link ParameterSchema} representing a homogeneous list of elements matching
   *   the provided {@link ParameterSchema}s.
   *
   * @param value A {@link ParameterSchema}.
   * @return A new {@link ParameterSchema} representing a homogeneous list of elements.
   */
  public static ParameterSchema ofList(final ParameterSchema value) {
    Objects.requireNonNull(value);
    return new ParameterSchema() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onList(value);
      }
      public String toString() {
        return "[" + value + "]";
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof ParameterSchema) && ((ParameterSchema)other).asList().equals(Optional.of(value)));
      }
    };
  }

  /**
   * Creates a {@link ParameterSchema} representing a heterogeneous set of named elements matching
   *   the associated {@link ParameterSchema}s.
   *
   * @param map Any set of named {@link ParameterSchema}s.
   * @return A new {@link ParameterSchema} representing a heterogeneous set of named {@link ParameterSchema}s.
   */
  public static ParameterSchema ofMap(final Map<String, ParameterSchema> map) {
    for (final var v : Objects.requireNonNull(map).values()) Objects.requireNonNull(v);
    final var value = Map.copyOf(map);
    return new ParameterSchema() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onMap(value);
      }
      public String toString() {
        return String.valueOf(value);
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof ParameterSchema) && ((ParameterSchema)other).asMap().equals(Optional.of(value)));
      }
    };
  }

  /**
   * Creates a {@link ParameterSchema} representing an {@link Enum} parameter type.
   *
   * @return A new {@link ParameterSchema} representing an {@link Enum} parameter type.
   */
  public static ParameterSchema ofEnum(final Class<? extends Enum<?>> enumeration) {
    Objects.requireNonNull(enumeration);
    return new ParameterSchema() {
      public <T> T match(Visitor<T> visitor) {
        return visitor.onEnum(enumeration);
      }
      public String toString() {
        return String.format("ParameterSchema.ENUM(%s)", enumeration.getName());
      }

      @Override
      public boolean equals(final Object other) {
        if (!(other instanceof ParameterSchema)) return false;
        return ((ParameterSchema)other).asEnum()
                .map(x -> Objects.equals(x, enumeration))
                .orElse(false);
      }
    };
  }

  public static final ParameterSchema REAL = ofReal();
  public static final ParameterSchema INT = ofInt();
  public static final ParameterSchema BOOLEAN = ofBoolean();
  public static final ParameterSchema STRING = ofString();

  @Deprecated(forRemoval = true)
  public static final ParameterSchema DOUBLE = REAL;

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
    public T onString() {
      return this.onDefault();
    }

    @Override
    public T onList(final ParameterSchema value) {
      return this.onDefault();
    }

    @Override
    public T onMap(final Map<String, ParameterSchema> value) {
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
    public Optional<T> onEnum(Class<? extends Enum<?>> enumeration) {
      return onDefault();
    }
  }

  public enum Unit { UNIT }

  /**
   * Asserts that this object represents a double parameter type.
   *
   * @return A non-empty {@link Optional} if this object represents a double parameter type.
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

  @Deprecated(forRemoval = true)
  public Optional<Unit> asDouble() {
    return asReal();
  }

  /**
   * Asserts that this object represents an int parameter type.
   *
   * @return A non-empty {@link Optional} if this object represents an int parameter type.
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
   * Asserts that this object represents a boolean parameter type.
   *
   * @return A non-empty {@link Optional} if this object represents an boolean parameter type.
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
   * Asserts that this object represents a String parameter type.
   *
   * @return A non-empty {@link Optional} if this object represents an String parameter type.
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
   * Attempts to access this schema as a list of {@code ParameterSchema}s.
   *
   * @return An {@link Optional} containing a schema for elements of a homogeneous list if this
   *   object represents a list parameter type. Otherwise, returns an empty {@link Optional}.
   */
  public Optional<ParameterSchema> asList() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<ParameterSchema> onList(final ParameterSchema value) {
        return Optional.of(value);
      }
    });
  }

  /**
   * Attempts to access this schema as a map of named {@code ParameterSchema}s.
   *
   * @return An {@link Optional} containing a map if this object represents a map parameter type.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Map<String, ParameterSchema>> asMap() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<Map<String, ParameterSchema>> onMap(final Map<String, ParameterSchema> value) {
        return Optional.of(value);
      }
    });
  }

  /**
   * Asserts that this object represents an enum parameter type.
   *
   * @return An {@link Optional} containing an enum if this object represents an Enum parameter type.
   *   Otherwise, returns an empty {@link Optional}
   */
  public Optional<Class<? extends Enum<?>>> asEnum() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<Class<? extends Enum<?>>> onEnum(final Class<? extends Enum<?>> enumeration) {
        return Optional.of(enumeration);
      }
    });
  }
}
