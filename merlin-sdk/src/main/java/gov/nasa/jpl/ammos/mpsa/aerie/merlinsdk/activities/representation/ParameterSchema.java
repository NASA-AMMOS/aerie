package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ParameterType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.unmodifiableMap;

/**
 * A serializable description of the structure of an activity parameter.
 *
 * Implementors of the {@link ActivityType} protocol may be constructed from parameters
 * (which are themselves implementors of the {@link ParameterType} protocol). A {@link ParameterSchema}
 * is an adaptation-agnostic representation of the serialized structure of a parameter.
 *
 * For instance, if an activity accepts two parameters, each of which is a 3D point in space,
 * then the schema for each point will be a sequence of three {@link double} primitives. The schema
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
    T onDouble();
    T onInt();
    T onBoolean();
    T onString();
    T onList(ParameterSchema value);
    T onMap(Map<String, ParameterSchema> value);
  }

  /**
   * Creates a {@link ParameterSchema} representing a {@link double} parameter type.
   *
   * @return A new {@link ParameterSchema} representing a {@link double} parameter type.
   */
  public static ParameterSchema ofDouble() {
    return new ParameterSchema() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onDouble();
      }
      public String toString() {
        return "ParameterSchema.DOUBLE";
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof ParameterSchema) && ((ParameterSchema)other).asDouble().isPresent());
      }
    };
  }

  /**
   * Creates a {@link ParameterSchema} representing an {@link int} parameter type.
   *
   * @return A new {@link ParameterSchema} representing an {@link int} parameter type.
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
        return "[" + String.valueOf(value) + "]";
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

  public static final ParameterSchema DOUBLE = ofDouble();
  public static final ParameterSchema INT = ofInt();
  public static final ParameterSchema BOOLEAN = ofBoolean();
  public static final ParameterSchema STRING = ofString();

  /**
   * A helper base class implementing {@code Visitor<Optional<T>>} for any result type {@code T}.
   *
   * This class allows you to write a Visitor that operates only on a subset of the possible
   * kinds of value contained by a {@link ParameterSchema}. All others are sent to {@code Optional.empty()}
   * by default. This default default behavior may be changed by overriding {@code onDefault}.
   *
   * @param <T> The return type of the operation represented by this {@link Visitor}.
   */
  public static abstract class DefaultVisitor<T> implements Visitor<Optional<T>> {
    protected Optional<T> onDefault() {
      return Optional.empty();
    }

    @Override
    public Optional<T> onDouble() {
      return onDefault();
    }

    @Override
    public Optional<T> onInt() {
      return onDefault();
    }

    @Override
    public Optional<T> onBoolean() {
      return onDefault();
    }

    @Override
    public Optional<T> onString() {
      return onDefault();
    }

    @Override
    public Optional<T> onList(final ParameterSchema value) {
      return onDefault();
    }

    @Override
    public Optional<T> onMap(final Map<String, ParameterSchema> value) {
      return onDefault();
    }
  }

  public static final class Unit {
    private Unit() {}

    public static Unit UNIT = new Unit();
  }

  /**
   * Asserts that this object represents a double parameter type.
   *
   * @return A non-empty {@link Optional} if this object represents a double parameter type.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Unit> asDouble() {
    return this.match(new DefaultVisitor<>() {
      @Override
      public Optional<Unit> onDouble() {
        return Optional.of(Unit.UNIT);
      }
    });
  }

  /**
   * Asserts that this object represents an int parameter type.
   *
   * @return A non-empty {@link Optional} if this object represents an int parameter type.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Unit> asInt() {
    return this.match(new DefaultVisitor<>() {
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
    return this.match(new DefaultVisitor<>() {
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
    return this.match(new DefaultVisitor<>() {
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
    return this.match(new DefaultVisitor<>() {
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
    return this.match(new DefaultVisitor<>() {
      @Override
      public Optional<Map<String, ParameterSchema>> onMap(final Map<String, ParameterSchema> value) {
        return Optional.of(value);
      }
    });
  }
}
