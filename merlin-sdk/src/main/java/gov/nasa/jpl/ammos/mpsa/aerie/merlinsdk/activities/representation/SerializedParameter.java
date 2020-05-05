package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ParameterType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * A serializable representation of an adaptation-specific activity parameter domain object.
 *
 * Implementors of the {@link ParameterType} protocol may be constructed from other {@link ParameterType}s.
 * A {@link SerializedParameter} is an adaptation-agnostic representation of the data in such an
 * activity parameter, structured as serializable primitives composed using sequences and maps.
 *
 * This class is implemented using the Visitor pattern, following the approach considered at
 * http://blog.higher-order.com/blog/2009/08/21/structural-pattern-matching-in-java/.
 * Because a (de)serialization format (such as JSON) may have a fixed set of primitives types
 * from which data may be composed. SerializedParameter ensures that all data boils down to
 * this fixed set of primitives.
 *
 * Note that, if the disk representation of a {@link SerializedParameter} could have multiple parses
 * -- multiple Java objects that it could deserialize to -- then there would an unresolvable
 * ambiguity in how to deserialize that disk representation. If {@link SerializedParameter} could be
 * freely subclassed, then such ambiguities would be inevitable (not to mention that deserialization
 * code would need to know about all possible subclasses for deserialization). The Visitor
 * pattern on a class closed to extension allows us to guarantee that no ambiguity occurs.
 */
public abstract class SerializedParameter {
  static public SerializedParameter NULL = SerializedParameter.ofNull();


  // Closed type family -- the only legal subclasses are those defined within the body of
  // this class definition.
  private SerializedParameter() {}

  /**
   * Calls the appropriate method of the passed {@link Visitor} depending on the kind of data
   * contained by this object.
   *
   * @param visitor The operation to be performed on the data contained by this object.
   * @param <T> The return type produced by the visiting operation.
   * @return The result of calling {@code visitor.onX()}, where {@code X} depends on the
   *   kind of data contained in this object.
   */
  public abstract <T> T match(Visitor<T> visitor);

  /**
   * An operation to be performed on the data contained in a {@link SerializedParameter}.
   *
   * A method must be defined for each kind of data that a {@link SerializedParameter} may contain.
   * This may be likened to the pattern-matching capability built into languages such as Rust
   * or Haskell.
   *
   * Most clients will prefer to inherit from {@link DefaultVisitor}, which returns `Optional.empty()`
   * for any unimplemented methods.
   *
   * @param <T> The return type of the operation represented by this {@link Visitor }.
   */
  public interface Visitor<T> {
    T onNull();
    T onReal(double value);
    T onInt(long value);
    T onBoolean(boolean value);
    T onString(String value);
    T onMap(Map<String, SerializedParameter> value);
    T onList(List<SerializedParameter> value);
  }

  /**
   * Creates a {@link SerializedParameter} containing a null value.
   *
   * @return A new {@link SerializedParameter} containing a null value.
   */
  private static SerializedParameter ofNull() {
    return new SerializedParameter() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onNull();
      }
      public String toString() {
        return "null";
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof SerializedParameter) && ((SerializedParameter)other).isNull());
      }
    };
  }

  /**
   * Creates a {@link SerializedParameter} containing a real number.
   *
   * @param value Any {@link double} value.
   * @return A new {@link SerializedParameter} containing a real number.
   */
  public static SerializedParameter of(final double value) {
    return new SerializedParameter() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onReal(value);
      }
      public String toString() {
        return String.valueOf(value);
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof SerializedParameter) && Objects.equals(((SerializedParameter)other).asReal(), Optional.of(value)));
      }
    };
  }

  /**
   * Creates a {@link SerializedParameter} containing an integral number.
   *
   * @param value Any {@link long} value.
   * @return A new {@link SerializedParameter} containing an integral number.
   */
  public static SerializedParameter of(final long value) {
    return new SerializedParameter() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onInt(value);
      }
      public String toString() {
        return String.valueOf(value);
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof SerializedParameter) && Objects.equals(((SerializedParameter)other).asInt(), Optional.of(value)));
      }
    };
  }

  /**
   * Creates a {@link SerializedParameter} containing a {@link boolean}.
   *
   * @param value Any {@link boolean} value.
   * @return A new {@link SerializedParameter} containing a {@link boolean}.
   */
  public static SerializedParameter of(final boolean value) {
    return new SerializedParameter() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onBoolean(value);
      }
      public String toString() {
        return String.valueOf(value);
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof SerializedParameter) && Objects.equals(((SerializedParameter)other).asBoolean(), Optional.of(value)));
      }
    };
  }

  /**
   * Creates a {@link SerializedParameter} containing a {@link String}.
   *
   * @param value Any {@link String} value.
   * @return A new {@link SerializedParameter} containing a {@link String}.
   */
  public static SerializedParameter of(final String value) {
    Objects.requireNonNull(value);
    return new SerializedParameter() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onString(value);
      }
      public String toString() {
        return value;
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof SerializedParameter) && Objects.equals(((SerializedParameter)other).asString(), Optional.of(value)));
      }
    };
  }

  /**
   * Creates a {@link SerializedParameter} containing a set of named {@link SerializedParameter}s.
   *
   * @param map Any set of named {@link SerializedParameter}s.
   * @return A new {@link SerializedParameter} containing a set of named {@link SerializedParameter}s.
   */
  public static SerializedParameter of(final Map<String, SerializedParameter> map) {
    for (final var v : Objects.requireNonNull(map).values()) Objects.requireNonNull(v);
    final var value = Map.copyOf(map);
    return new SerializedParameter() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onMap(value);
      }
      public String toString() {
        return String.valueOf(value);
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof SerializedParameter) && Objects.equals(((SerializedParameter)other).asMap(), Optional.of(value)));
      }
    };
  }

  /**
   * Creates a {@link SerializedParameter} containing a list of {@link SerializedParameter}s.
   *
   * @param list Any list of {@link SerializedParameter}s.
   * @return A new SerializedParameter containing a list of {@link SerializedParameter}s.
   */
  public static SerializedParameter of(final List<SerializedParameter> list) {
    for (final var v : Objects.requireNonNull(list)) Objects.requireNonNull(v);
    final var value = List.copyOf(list);
    return new SerializedParameter() {
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onList(value);
      }
      public String toString() {
        return String.valueOf(value);
      }

      @Override
      public boolean equals(final Object other) {
        return ((other instanceof SerializedParameter) && Objects.equals(((SerializedParameter)other).asList(), Optional.of(value)));
      }
    };
  }


  /**
   * A helper base class implementing {@code Visitor<Optional<T>>} for any result type {@code T}.
   *
   * This class allows you to write a {@link Visitor} that operates only on a subset of the possible
   * kinds of value contained by a {@link SerializedParameter}. All others are sent to {@code Optional.empty()}
   * by default. This default behavior may be changed by overriding {@code onDefault}.
   *
   * @param <T> The return type of the operation represented by this {@link Visitor}.
   */
  public static abstract class DefaultVisitor<T> implements Visitor<Optional<T>> {
    protected Optional<T> onDefault() {
      return Optional.empty();
    }

    @Override
    public Optional<T> onNull() {
      return onDefault();
    }

    @Override
    public Optional<T> onReal(final double value) {
      return onDefault();
    }

    @Override
    public Optional<T> onInt(final long value) {
      return onDefault();
    }

    @Override
    public Optional<T> onBoolean(final boolean value) {
      return onDefault();
    }

    @Override
    public Optional<T> onString(final String value) {
      return onDefault();
    }

    @Override
    public Optional<T> onMap(final Map<String, SerializedParameter> value) {
      return onDefault();
    }

    @Override
    public Optional<T> onList(final List<SerializedParameter> value) {
      return onDefault();
    }
  }

  /**
   * Determines if this object represents a null value.
   *
   * @return True if this object represents a null value, and false otherwise.
   */
  public boolean isNull() {
    return this
        .match(new DefaultVisitor<Boolean>() {
          @Override
          public Optional<Boolean> onNull() {
            return Optional.of(true);
          }
        })
        .orElse(false);
  }

  /**
   * Attempts to access the data in this object as a real number.
   *
   * @return An {@link Optional} containing a {@link double} if this object contains a real number.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Double> asReal() {
    return this.match(new DefaultVisitor<>() {
      @Override
      public Optional<Double> onReal(final double value) {
        return Optional.of(value);
      }

      @Override
      public Optional<Double> onInt(final long value) {
        return Optional.of((double)value);
      }
    });
  }

  /**
   * Attempts to access the data in this object as an integral number.
   *
   * @return An {@link Optional} containing a {@link long} if this object contains an integral number.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Long> asInt() {
    return this.match(new DefaultVisitor<>() {
      @Override
      public Optional<Long> onInt(final long value) {
        return Optional.of(value);
      }

      @Override
      public Optional<Long> onReal(final double value) {
        if (!Double.isFinite(value)) return Optional.empty();
        if (Math.floor(value) != value) return Optional.empty();
        return Optional.of((long)value);
      }
    });
  }

  /**
   * Attempts to access the data in this object as a {@link boolean}.
   *
   * @return An {@link Optional} containing a {@link boolean} if this object contains a {@link boolean}.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Boolean> asBoolean() {
    return this.match(new DefaultVisitor<>() {
      @Override
      public Optional<Boolean> onBoolean(final boolean value) {
        return Optional.of(value);
      }
    });
  }

  /**
   * Attempts to access the data in this object as a {@link String}.
   *
   * @return An {@link Optional} containing a {@link String} if this object contains a {@link String}.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<String> asString() {
    return this.match(new DefaultVisitor<>() {
      @Override
      public Optional<String> onString(final String value) {
        return Optional.of(value);
      }
    });
  }

  /**
   * Attempts to access the data in this object as a map of named {@code SerializedParameter}s.
   *
   * @return An {@link Optional} containing a map if this object contains a map.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Map<String, SerializedParameter>> asMap() {
    return this.match(new DefaultVisitor<>() {
      @Override
      public Optional<Map<String, SerializedParameter>> onMap(final Map<String, SerializedParameter> value) {
        return Optional.of(value);
      }
    });
  }

  /**
   * Attempts to access the data in this object as a list of {@code SerializedParameter}s.
   *
   * @return An {@link Optional} containing a list if this object contains a list.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<List<SerializedParameter>> asList() {
    return this.match(new DefaultVisitor<>() {
      @Override
      public Optional<List<SerializedParameter>> onList(final List<SerializedParameter> value) {
        return Optional.of(value);
      }
    });
  }
}
