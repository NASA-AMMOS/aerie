package gov.nasa.jpl.aerie.merlin.protocol;

import java.util.Objects;

public abstract class CompoundCondition<$Schema> {
  private CompoundCondition() {}

  public abstract <Result> Result interpret(Visitor<$Schema, Result> visitor);

  public interface Visitor<$Schema, Result> {
    <R, D, C>
    Result atom(ResourceSolver<$Schema, R, D, C> resourceType, R resource, C condition);

    Result not(Result x);
    Result and(Result x, Result y);
    Result or(Result x, Result y);
  }

  public static <$Schema, R, C>
  CompoundCondition<$Schema> atom(ResourceSolver<$Schema, R, ?, C> resourceType, R resource, C condition) {
    Objects.requireNonNull(resourceType);
    Objects.requireNonNull(resource);
    Objects.requireNonNull(condition);

    return new CompoundCondition<>() {
      @Override
      public <Result> Result interpret(final Visitor<$Schema, Result> visitor) {
        return visitor.atom(resourceType, resource, condition);
      }
    };
  }

  public static <$Schema>
  CompoundCondition<$Schema> not(final CompoundCondition<$Schema> x) {
    Objects.requireNonNull(x);

    return new CompoundCondition<>() {
      @Override
      public <Result> Result interpret(final Visitor<$Schema, Result> visitor) {
        return visitor.not(x.interpret(visitor));
      }
    };
  }

  public static <$Schema>
  CompoundCondition<$Schema> and(final CompoundCondition<$Schema> x, final CompoundCondition<$Schema> y) {
    Objects.requireNonNull(x);
    Objects.requireNonNull(y);

    return new CompoundCondition<>() {
      @Override
      public <Result> Result interpret(final Visitor<$Schema, Result> visitor) {
        return visitor.and(x.interpret(visitor), y.interpret(visitor));
      }
    };
  }

  public static <$Schema>
  CompoundCondition<$Schema> or(final CompoundCondition<$Schema> x, final CompoundCondition<$Schema> y) {
    Objects.requireNonNull(x);
    Objects.requireNonNull(y);

    return new CompoundCondition<>() {
      @Override
      public <Result> Result interpret(final Visitor<$Schema, Result> visitor) {
        return visitor.or(x.interpret(visitor), y.interpret(visitor));
      }
    };
  }


  public final CompoundCondition<$Schema> and(final CompoundCondition<$Schema> other) {
    return CompoundCondition.and(this, other);
  }

  public final CompoundCondition<$Schema> or(final CompoundCondition<$Schema> other) {
    return CompoundCondition.or(this, other);
  }

  public final CompoundCondition<$Schema> negate() {
    return CompoundCondition.not(this);
  }
}
