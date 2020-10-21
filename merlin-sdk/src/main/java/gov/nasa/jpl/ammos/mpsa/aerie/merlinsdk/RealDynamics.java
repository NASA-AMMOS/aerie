package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

/**
 * A description of a dynamic behavior for real-valued resources that may vary continuously.
 *
 * <p>
 *   In general, a dynamics gives an embedding of an interval of time into a space of values.
 *   We expect these embeddings to be <i>continuous</i>: a closed set of values
 *   should be mapped onto by a closed interval of time.
 * </p>
 *
 * <p>
 *   We also want a class of continuous maps from our space of values into the Sierpinski space of boolean valuations.
 *   We call these maps <i>conditions</i>, and each is uniquely given by a choice of closed set in the space of values.
 *   Due to computability and representability concerns, we restrict ourselves further to <i>compact</i> sets.
 * </p>
 *
 * <p>
 *   For real-valued resources, we want such conditions to be thresholds.
 *   The appropriate space is then the one-dimensional Euclidean space,
 *     where compact sets are finite unions of closed intervals.
 *   A dynamics may be any continuous embedding of a time interval;
 *     we select a few useful classes of dynamics for our use.
 * </p>
 */
public abstract class RealDynamics {
  private RealDynamics() {}

  public abstract <Result> Result match(final Visitor<Result> visitor);

  public interface Visitor<Result> {
    // λt. value
    Result constant(double value);

    // λt. intercept + t * slope
    Result linear(double intercept, double slope);

    // In the future: polynomials, maybe differential systems.
  }

  public static RealDynamics constant(final double value) {
    return new RealDynamics() {
      @Override
      public <Result> Result match(final Visitor<Result> visitor) {
        return visitor.constant(value);
      }
    };
  }

  public static RealDynamics linear(final double intercept, final double slope) {
    return new RealDynamics() {
      @Override
      public <Result> Result match(final Visitor<Result> visitor) {
        return visitor.linear(intercept, slope);
      }
    };
  }

  @Override
  public String toString() {
    return this.match(new Visitor<>() {
      @Override
      public String constant(final double value) {
        return "λt. " + value;
      }

      @Override
      public String linear(final double intercept, final double slope) {
        return "λt. " + intercept + " + t * " + slope;
      }
    });
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof RealDynamics)) return false;
    final var other = (RealDynamics) o;

    // The reader may ask, why not implement at least one level of this dispatch
    //   as equals() methods on each individual subclass?
    // We want to make sure that, whenever we add new dynamic behaviors,
    //   all combinations are accounted for.
    // Since Object already has a default equals method,
    //   relying on dynamic dispatch would make it easy to miss a case.
    // It may be a little ugly in Java,
    //   but a two-level match makes it impossible to miss a case.
    // (We can always add a `DefaultVisitor` to reduce the combinatorial explosion.)
    return this.match(new Visitor<>() {
      @Override
      public Boolean constant(final double value1) {
        return other.match(new Visitor<>() {
          @Override
          public Boolean constant(final double value2) {
            return (value1 == value2);
          }

          @Override
          public Boolean linear(final double intercept2, final double slope2) {
            return false;
          }
        });
      }

      @Override
      public Boolean linear(final double intercept1, final double slope1) {
        return other.match(new Visitor<>() {
          @Override
          public Boolean constant(final double value2) {
            return false;
          }

          @Override
          public Boolean linear(final double intercept2, final double slope2) {
            return (intercept1 == intercept2) && (slope1 == slope2);
          }
        });
      }
    });
  }
}
