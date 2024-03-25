package gov.nasa.jpl.aerie.merlin.protocol.types;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A serializable representation of a mission model-specific activity parameter domain object.
 *
 * A {@link SerializedValue} is a mission model-agnostic representation of the data in such an
 * activity parameter, structured as serializable primitives composed using sequences and maps.
 *
 * This class is implemented using the Visitor pattern, following the approach considered at
 * http://blog.higher-order.com/blog/2009/08/21/structural-pattern-matching-in-java/.
 * Because a (de)serialization format (such as JSON) may have a fixed set of primitives types
 * from which data may be composed. SerializedValue ensures that all data boils down to
 * this fixed set of primitives.
 *
 * Note that, if the disk representation of a {@link SerializedValue} could have multiple parses
 * -- multiple Java objects that it could deserialize to -- then there would an unresolvable
 * ambiguity in how to deserialize that disk representation. If {@link SerializedValue} could be
 * freely subclassed, then such ambiguities would be inevitable (not to mention that deserialization
 * code would need to know about all possible subclasses for deserialization). The Visitor
 * pattern on a class closed to extension allows us to guarantee that no ambiguity occurs.
 */
public sealed interface SerializedValue {
  SerializedValue NULL = SerializedValue.ofNull();

  /**
   * Calls the appropriate method of the passed {@link Visitor} depending on the kind of data
   * contained by this object.
   *
   * @param visitor The operation to be performed on the data contained by this object.
   * @param <T> The return type produced by the visiting operation.
   * @return The result of calling {@code visitor.onX()}, where {@code X} depends on the
   *   kind of data contained in this object.
   */
  <T> T match(Visitor<T> visitor);

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
   * @param <T> The return type of the operation represented by this {@link Visitor}.
   */
  interface Visitor<T> {
    T onNull();
    T onNumeric(BigDecimal value);
    T onBoolean(boolean value);
    T onString(String value);
    T onMap(Map<String, SerializedValue> value);
    T onList(List<SerializedValue> value);
  }

  record NullValue() implements SerializedValue {
    @Override
    public <T> T match(final Visitor<T> visitor) {
      return visitor.onNull();
    }
  }

  interface DirectNumericValue {
    NumericValue asNumericValue();
  }

  record NumericValue(BigDecimal value) implements SerializedValue {
    @Override
    public <T> T match(final Visitor<T> visitor) {
      return visitor.onNumeric(value);
    }

    // `BigDecimal#equals` is too strict -- values differing only in representation need to be considered the same.
    @Override
    public boolean equals(final Object obj) {
      if (obj instanceof NumericValue other) {
        return (this.value.compareTo(other.value) == 0);
      } else if (obj instanceof DirectNumericValue other) {
        return (this.value.compareTo(other.asNumericValue().value) == 0);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return this.value.stripTrailingZeros().hashCode();
    }
  }

  record IntValue(int value) implements SerializedValue, DirectNumericValue {
    @Override
    public <T> T match(final Visitor<T> visitor) {
      return visitor.onNumeric(new BigDecimal(value));
    }

    @Override
    public NumericValue asNumericValue() {
      return new NumericValue(new BigDecimal(value));
    }

    @Override
    public boolean equals(final Object obj) {
      return asNumericValue().equals(obj);
    }
  }

  record LongValue(long value) implements SerializedValue, DirectNumericValue {
    @Override
    public <T> T match(final Visitor<T> visitor) {
      return visitor.onNumeric(new BigDecimal(value));
    }

    @Override
    public NumericValue asNumericValue() {
      return new NumericValue(new BigDecimal(value));
    }

    @Override
    public boolean equals(final Object obj) {
      return asNumericValue().equals(obj);
    }
  }

  record DoubleValue(double value) implements SerializedValue, DirectNumericValue {
    @Override
    public <T> T match(final Visitor<T> visitor) {
      return visitor.onNumeric(toBigDecimal());
    }

    @Override
    public NumericValue asNumericValue() {
      return new NumericValue(toBigDecimal());
    }

    @Override
    public boolean equals(final Object obj) {
      return asNumericValue().equals(obj);
    }

    private BigDecimal toBigDecimal() {
      //without MathContext.DECIMAL64 then a double assigned from a string (or code literal) "3.14"
      //converts to a BigDecimal=3.140000000000000124344978758017532527446746826171875
      //but since a double can only represent up to 15 decimal digits when going from string -> double -> string
      //the nonzero values in the smaller decimal places are just an artifact of the representation
      //and there are unit tests that assume that string -> double -> string will be an identity op for e.g. 3.14
      //with MathContext.DECIMAL64 "3.14" converts to a BigDecimal=3.140000000000000
      var bd = new BigDecimal(value, MathContext.DECIMAL64);
      if (bd.scale() == 0) { //if the underlying value was actually an integer
        //we want to always serialize as a real number, i.e. "1.0" not "1" in JSON
        bd = new BigDecimal(bd.unscaledValue().multiply(BigInteger.valueOf(10)), 1); //yes scale=1 not -1
      }
      return bd;
    }
  }

  record BooleanValue(boolean value) implements SerializedValue {
    @Override
    public <T> T match(final Visitor<T> visitor) {
      return visitor.onBoolean(value);
    }
  }

  record StringValue(String value) implements SerializedValue {
    @Override
    public <T> T match(final Visitor<T> visitor) {
      return visitor.onString(value);
    }
  }

  record MapValue(Map<String, SerializedValue> map) implements SerializedValue {
    @Override
    public <T> T match(final Visitor<T> visitor) {
      return visitor.onMap(map);
    }
  }

  record ListValue(List<SerializedValue> list) implements SerializedValue {
    @Override
    public <T> T match(final Visitor<T> visitor) {
      return visitor.onList(list);
    }
  }

  /**
   * Creates a {@link SerializedValue} containing a null value.
   *
   * @return A new {@link SerializedValue} containing a null value.
   */
  private static SerializedValue ofNull() {
    return new NullValue();
  }

  /**
   * Creates a {@link SerializedValue} containing an arbitrary-precision number.
   *
   * @param value Any {@link BigDecimal} value.
   * @return A new {@link SerializedValue} containing an arbitrary-precision number.
   */
  static SerializedValue of(final BigDecimal value) {
    return new NumericValue(value);
  }

  /**
   * Creates a {@link SerializedValue} containing a real number.
   *
   * @param value Any double value.
   * @return A new {@link SerializedValue} containing a real number.
   */
  static SerializedValue of(final double value) {
    return new DoubleValue(value);
  }

  /**
   * Creates a {@link SerializedValue} containing an integral number.
   *
   * @param value Any long value.
   * @return A new {@link SerializedValue} containing an integral number.
   */
  static SerializedValue of(final long value) {
    return new LongValue(value);
  }

  /**
   * Creates a {@link SerializedValue} containing an integral number.
   *
   * @param value Any integer value.
   * @return A new {@link SerializedValue} containing an integral number.
   */
  static SerializedValue of(final int value) {
    return new IntValue(value);
  }

  /**
   * Creates a {@link SerializedValue} containing a boolean.
   *
   * @param value Any boolean value.
   * @return A new {@link SerializedValue} containing a boolean.
   */
  static SerializedValue of(final boolean value) {
    return new BooleanValue(value);
  }

  /**
   * Creates a {@link SerializedValue} containing a {@link String}.
   *
   * @param value Any {@link String} value.
   * @return A new {@link SerializedValue} containing a {@link String}.
   */
  static SerializedValue of(final String value) {
    Objects.requireNonNull(value);
    return new StringValue(value);
  }

  /**
   * Creates a {@link SerializedValue} containing a set of named {@link SerializedValue}s.
   *
   * @param map Any set of named {@link SerializedValue}s.
   * @return A new {@link SerializedValue} containing a set of named {@link SerializedValue}s.
   */
  static SerializedValue of(final Map<String, SerializedValue> map) {
    for (final var v : Objects.requireNonNull(map).values()) Objects.requireNonNull(v);
    final var value = Map.copyOf(map);
    return new MapValue(value);
  }

  /**
   * Creates a {@link SerializedValue} containing a list of {@link SerializedValue}s.
   *
   * @param list Any list of {@link SerializedValue}s.
   * @return A new SerializedValue containing a list of {@link SerializedValue}s.
   */
  static SerializedValue of(final List<SerializedValue> list) {
    for (final var v : Objects.requireNonNull(list)) Objects.requireNonNull(v);
    final var value = List.copyOf(list);
    return new ListValue(value);
  }


  /**
   * Provides a default case on top of the base Visitor.
   *
   * This interface routes all cases to the `onDefault` implementation by default. Each case may be overridden
   * independently to give distinct behavior.
   *
   * @param <T> The return type of the operation represented by this {@link Visitor}.
   */
  abstract class DefaultVisitor<T> implements Visitor<T> {
    protected abstract T onDefault();

    @Override
    public T onNull() {
      return this.onDefault();
    }

    @Override
    public T onNumeric(final BigDecimal value) {
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
  abstract class OptionalVisitor<T> extends DefaultVisitor<Optional<T>> {
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
  default boolean isNull() {
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
   * Attempts to access the data in this object as an arbitrary-precision number.
   *
   * @return An {@link Optional} containing a BigDecimal if this object contains an arbitrary-precision number.
   *   Otherwise, returns an empty {@link Optional}.
   */
  default Optional<BigDecimal> asNumeric() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<BigDecimal> onNumeric(final BigDecimal value) {
        return Optional.of(value);
      }
    });
  }

  /**
   * Attempts to access the data in this object as a real number.
   *
   * @return An {@link Optional} containing a double if this object contains a real number.
   *   Otherwise, returns an empty {@link Optional}.
   */
  default Optional<Double> asReal() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<Double> onNumeric(final BigDecimal value) {
        return Optional.of(value.doubleValue());
      }
    });
  }

  /**
   * Attempts to access the data in this object as an integral number.
   *
   * @return An {@link Optional} containing a long if this object contains an integral number.
   *   Otherwise, returns an empty {@link Optional}.
   */
  default Optional<Long> asInt() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<Long> onNumeric(final BigDecimal value) {
        try {
          return Optional.of(value.longValueExact());
        } catch (final ArithmeticException ex) {
          return Optional.empty();
        }
      }
    });
  }

  /**
   * Attempts to access the data in this object as a boolean.
   *
   * @return An {@link Optional} containing a boolean if this object contains a boolean.
   *   Otherwise, returns an empty {@link Optional}.
   */
  default Optional<Boolean> asBoolean() {
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
  default Optional<String> asString() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<String> onString(final String value) {
        return Optional.of(value);
      }
    });
  }

  /**
   * Attempts to access the data in this object as a map of named {@code SerializedValue}s.
   *
   * @return An {@link Optional} containing a map if this object contains a map.
   *   Otherwise, returns an empty {@link Optional}.
   */
  default Optional<Map<String, SerializedValue>> asMap() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<Map<String, SerializedValue>> onMap(final Map<String, SerializedValue> value) {
        return Optional.of(value);
      }
    });
  }

  /**
   * Attempts to access the data in this object as a list of {@code SerializedValue}s.
   *
   * @return An {@link Optional} containing a list if this object contains a list.
   *   Otherwise, returns an empty {@link Optional}.
   */
  default Optional<List<SerializedValue>> asList() {
    return this.match(new OptionalVisitor<>() {
      @Override
      public Optional<List<SerializedValue>> onList(final List<SerializedValue> value) {
        return Optional.of(value);
      }
    });
  }
}
