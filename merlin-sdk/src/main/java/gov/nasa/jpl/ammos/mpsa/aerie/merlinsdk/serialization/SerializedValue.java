package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.ParameterType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A serializable representation of an adaptation-specific activity parameter domain object.
 *
 * Implementors of the {@link ParameterType} protocol may be constructed from other {@link ParameterType}s.
 * A {@link SerializedValue} is an adaptation-agnostic representation of the data in such an
 * activity parameter, structured as serializable primitives composed using sequences and maps.
 *
 * This class is implemented using the Visitor pattern, following the approach considered at
 * http://blog.higher-order.com/blog/2009/08/21/structural-pattern-matching-in-java/.
 * Because a (de)serialization format (such as JSON) may have a fixed set of primitives types
 * from which data may be composed. SerializedParameter ensures that all data boils down to
 * this fixed set of primitives.
 *
 * Note that, if the disk representation of a {@link SerializedValue} could have multiple parses
 * -- multiple Java objects that it could deserialize to -- then there would an unresolvable
 * ambiguity in how to deserialize that disk representation. If {@link SerializedValue} could be
 * freely subclassed, then such ambiguities would be inevitable (not to mention that deserialization
 * code would need to know about all possible subclasses for deserialization). The Visitor
 * pattern on a class closed to extension allows us to guarantee that no ambiguity occurs.
 */
public abstract class SerializedValue {
  static public SerializedValue NULL = SerializedValue.ofNull();


  // Closed type family -- the only legal subclasses are those defined within the body of
  // this class definition.
  private SerializedValue() {}

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
   * An operation to be performed on the data contained in a {@link SerializedValue}.
   *
   * A method must be defined for each kind of data that a {@link SerializedValue} may contain.
   * This may be likened to the pattern-matching capability built into languages such as Rust
   * or Haskell.
   *
   * Most clients will prefer to inherit from {@link OptionalVisitor}, which returns `Optional.empty()`
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
    T onMap(Map<String, SerializedValue> value);
    T onList(List<SerializedValue> value);
  }

  /**
   * Creates a {@link SerializedValue} containing a null value.
   *
   * @return A new {@link SerializedValue} containing a null value.
   */
  private static SerializedValue ofNull() {
    return new SerializedValue() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onNull();
      }
    };
  }

  /**
   * Creates a {@link SerializedValue} containing a real number.
   *
   * @param value Any {@link double} value.
   * @return A new {@link SerializedValue} containing a real number.
   */
  public static SerializedValue of(final double value) {
    return new SerializedValue() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onReal(value);
      }
    };
  }

  /**
   * Creates a {@link SerializedValue} containing an integral number.
   *
   * @param value Any {@link long} value.
   * @return A new {@link SerializedValue} containing an integral number.
   */
  public static SerializedValue of(final long value) {
    return new SerializedValue() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onInt(value);
      }
    };
  }

  /**
   * Creates a {@link SerializedValue} containing a {@link boolean}.
   *
   * @param value Any {@link boolean} value.
   * @return A new {@link SerializedValue} containing a {@link boolean}.
   */
  public static SerializedValue of(final boolean value) {
    return new SerializedValue() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onBoolean(value);
      }
    };
  }

  /**
   * Creates a {@link SerializedValue} containing a {@link String}.
   *
   * @param value Any {@link String} value.
   * @return A new {@link SerializedValue} containing a {@link String}.
   */
  public static SerializedValue of(final String value) {
    Objects.requireNonNull(value);
    return new SerializedValue() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onString(value);
      }
    };
  }

  /**
   * Creates a {@link SerializedValue} containing a set of named {@link SerializedValue}s.
   *
   * @param map Any set of named {@link SerializedValue}s.
   * @return A new {@link SerializedValue} containing a set of named {@link SerializedValue}s.
   */
  public static SerializedValue of(final Map<String, SerializedValue> map) {
    for (final var v : Objects.requireNonNull(map).values()) Objects.requireNonNull(v);
    final var value = Map.copyOf(map);
    return new SerializedValue() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onMap(value);
      }
    };
  }

  /**
   * Creates a {@link SerializedValue} containing a list of {@link SerializedValue}s.
   *
   * @param list Any list of {@link SerializedValue}s.
   * @return A new SerializedParameter containing a list of {@link SerializedValue}s.
   */
  public static SerializedValue of(final List<SerializedValue> list) {
    for (final var v : Objects.requireNonNull(list)) Objects.requireNonNull(v);
    final var value = List.copyOf(list);
    return new SerializedValue() {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onList(value);
      }
    };
  }


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
    public T onNull() {
      return this.onDefault();
    }

    @Override
    public T onReal(final double value) {
      return this.onDefault();
    }

    @Override
    public T onInt(final long value) {
      return this.onDefault();
    }

    @Override
    public T onBoolean(final boolean value) {
      return this.onDefault();
    }

    @Override
    public T onString(final String value) {
      return this.onDefault();
    }

    @Override
    public T onMap(final Map<String, SerializedValue> value) {
      return this.onDefault();
    }

    @Override
    public T onList(final List<SerializedValue> value) {
      return this.onDefault();
    }
  }

  /**
   * A helper base class implementing {@code Visitor<Optional<T>>} for any result type {@code T}.
   *
   * By default, all variants return {@code Optional.empty}.
   */
  public static abstract class OptionalVisitor<T> extends DefaultVisitor<Optional<T>> {
    @Override
    protected Optional<T> onDefault() {
      return Optional.empty();
    }
  }

  /**
   * Determines if this object represents a null value.
   *
   * @return True if this object represents a null value, and false otherwise.
   */
  public boolean isNull() {
    return this.match(new DefaultVisitor<>() {
      @Override
      public Boolean onNull() {
        return true;
      }

      @Override
      protected Boolean onDefault() {
        return false;
      }
    });
  }

  /**
   * Attempts to access the data in this object as a real number.
   *
   * @return An {@link Optional} containing a {@link double} if this object contains a real number.
   *   Otherwise, returns an empty {@link Optional}.
   */
  public Optional<Double> asReal() {
    return this.match(new OptionalVisitor<>() {
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
    return this.match(new OptionalVisitor<>() {
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
    return this.match(new OptionalVisitor<>() {
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
    return this.match(new OptionalVisitor<>() {
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
  public Optional<Map<String, SerializedValue>> asMap() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<Map<String, SerializedValue>> onMap(final Map<String, SerializedValue> value) {
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
  public Optional<List<SerializedValue>> asList() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<List<SerializedValue>> onList(final List<SerializedValue> value) {
        return Optional.of(value);
      }
    });
  }

  @Override
  public String toString() {
    return this.match(new Visitor<>() {
      @Override
      public String onNull() {
        return "null";
      }

      @Override
      public String onReal(final double value) {
        return String.valueOf(value);
      }

      @Override
      public String onInt(final long value) {
        return String.valueOf(value);
      }

      @Override
      public String onBoolean(final boolean value) {
        return String.valueOf(value);
      }

      @Override
      public String onString(final String value) {
        return value;
      }

      @Override
      public String onMap(final Map<String, SerializedValue> value) {
        return String.valueOf(value);
      }

      @Override
      public String onList(final List<SerializedValue> value) {
        return String.valueOf(value);
      }
    });
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof SerializedValue)) return false;
    final var other = (SerializedValue) o;

    return this.match(new Visitor<>() {
      @Override
      public Boolean onNull() {
        return other.isNull();
      }

      @Override
      public Boolean onReal(final double value) {
        return other.asReal().map(x -> x == value).orElse(false);
      }

      @Override
      public Boolean onInt(final long value) {
        return other.asInt().map(x -> x == value).orElse(false);
      }

      @Override
      public Boolean onBoolean(final boolean value) {
        return other.asBoolean().map(x -> x == value).orElse(false);
      }

      @Override
      public Boolean onString(final String value) {
        return other.asString().map(x -> Objects.equals(x, value)).orElse(false);
      }

      @Override
      public Boolean onMap(final Map<String, SerializedValue> value) {
        return other.asMap().map(x -> Objects.equals(x, value)).orElse(false);
      }

      @Override
      public Boolean onList(final List<SerializedValue> value) {
        return other.asList().map(x -> Objects.equals(x, value)).orElse(false);
      }
    });
  }
}
