package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits;

import java.util.Objects;

public abstract class SettableEffect<Accumulator, Effect> {
  private SettableEffect() {}

  public abstract <Result> Result visit(Visitor<Accumulator, Effect, Result> visitor);

  public interface Visitor<Accumulator, Effect, Result> {
    Result conflict();
    Result setTo(Accumulator base);
    Result add(Effect delta);
  }

  public interface VoidVisitor<Accumulator, Effect> {
    void conflict();
    void setTo(Accumulator base);
    void add(Effect delta);
  }

  public static <Accumulator, Effect> SettableEffect<Accumulator, Effect> conflict() {
    return new SettableEffect<>() {
      @Override
      public <Result> Result visit(final Visitor<Accumulator, Effect, Result> visitor) {
        return visitor.conflict();
      }
    };
  }

  public static <Accumulator, Effect> SettableEffect<Accumulator, Effect> setTo(final Accumulator base) {
    Objects.requireNonNull(base);

    return new SettableEffect<>() {
      @Override
      public <Result> Result visit(final Visitor<Accumulator, Effect, Result> visitor) {
        return visitor.setTo(base);
      }
    };
  }

  public static <Accumulator, Effect> SettableEffect<Accumulator, Effect> add(final Effect delta) {
    Objects.requireNonNull(delta);

    return new SettableEffect<>() {
      @Override
      public <Result> Result visit(final Visitor<Accumulator, Effect, Result> visitor) {
        return visitor.add(delta);
      }
    };
  }

  // Convenience method
  public final void visit(final VoidVisitor<Accumulator, Effect> visitor) {
    this.visit(new Visitor<>() {
      @Override
      public Object conflict() {
        visitor.conflict();
        return null;
      }

      @Override
      public Object setTo(final Accumulator base) {
        visitor.setTo(base);
        return null;
      }

      @Override
      public Object add(final Effect delta) {
        visitor.add(delta);
        return null;
      }
    });
  }

  @Override
  public final String toString() {
    return this.visit(new Visitor<>() {
      @Override
      public String conflict() {
        return "conflict()";
      }

      @Override
      public String setTo(final Accumulator base) {
        return "setTo(" + base + ")";
      }

      @Override
      public String add(final Effect delta) {
        return "add(" + delta + ")";
      }
    });
  }
}
