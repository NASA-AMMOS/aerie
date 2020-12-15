package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import java.util.Objects;

public abstract class Condition<$Schema> {
  private Condition() {}

  public abstract <Result> Result interpret(Visitor<$Schema, Result> visitor);

  public interface Visitor<$Schema, Result> {
    <R, C>
    Result atom(ResourceSolver<$Schema, R, ?, C> resourceType, R resource, C condition);

    Result not(Result x);
    Result and(Result x, Result y);
    Result or(Result x, Result y);
  }

  public static <$Schema, R, C>
  Condition<$Schema> atom(ResourceSolver<$Schema, R, ?, C> resourceType, R resource, C condition) {
    Objects.requireNonNull(resourceType);
    Objects.requireNonNull(resource);
    Objects.requireNonNull(condition);

    return new Condition<>() {
      @Override
      public <Result> Result interpret(final Visitor<$Schema, Result> visitor) {
        return visitor.atom(resourceType, resource, condition);
      }
    };
  }

  public static <$Schema>
  Condition<$Schema> not(final Condition<$Schema> x) {
    Objects.requireNonNull(x);

    return new Condition<>() {
      @Override
      public <Result> Result interpret(final Visitor<$Schema, Result> visitor) {
        return visitor.not(x.interpret(visitor));
      }
    };
  }

  public static <$Schema>
  Condition<$Schema> and(final Condition<$Schema> x, final Condition<$Schema> y) {
    Objects.requireNonNull(x);
    Objects.requireNonNull(y);

    return new Condition<>() {
      @Override
      public <Result> Result interpret(final Visitor<$Schema, Result> visitor) {
        return visitor.and(x.interpret(visitor), y.interpret(visitor));
      }
    };
  }

  public static <$Schema>
  Condition<$Schema> or(final Condition<$Schema> x, final Condition<$Schema> y) {
    Objects.requireNonNull(x);
    Objects.requireNonNull(y);

    return new Condition<>() {
      @Override
      public <Result> Result interpret(final Visitor<$Schema, Result> visitor) {
        return visitor.or(x.interpret(visitor), y.interpret(visitor));
      }
    };
  }


  public final Condition<$Schema> and(final Condition<$Schema> other) {
    return Condition.and(this, other);
  }

  public final Condition<$Schema> or(final Condition<$Schema> other) {
    return Condition.or(this, other);
  }

  public final Condition<$Schema> negate() {
    return Condition.not(this);
  }
}
