package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class DelimitedDynamics<Dynamics> {
  private DelimitedDynamics() {}

  public abstract Dynamics getDynamics();
  public abstract Duration getEndTime();

  public static <Dynamics> DelimitedDynamics<Dynamics> delimited(final Duration endTime, final Dynamics dynamics) {
    Objects.requireNonNull(endTime);
    Objects.requireNonNull(dynamics);

    return new DelimitedDynamics<>() {
      @Override
      public Dynamics getDynamics() {
        return dynamics;
      }

      @Override
      public Duration getEndTime() {
        return endTime;
      }
    };
  }

  public static <Dynamics> DelimitedDynamics<Dynamics> persistent(final Dynamics dynamics) {
    return DelimitedDynamics.delimited(Duration.MAX_VALUE, dynamics);
  }

  public final <Result> DelimitedDynamics<Result> map(final Function<Dynamics, Result> transform) {
    return DelimitedDynamics.delimited(this.getEndTime(), transform.apply(this.getDynamics()));
  }

  public final <Other, Result> DelimitedDynamics<Result> parWith(
      final DelimitedDynamics<Other> other,
      final BiFunction<Dynamics, Other, Result> transform)
  {
    return DelimitedDynamics.delimited(
        Duration.min(this.getEndTime(), other.getEndTime()),
        transform.apply(this.getDynamics(), other.getDynamics()));
  }

  public final <Other> DelimitedDynamics<Pair<Dynamics, Other>> parWith(final DelimitedDynamics<Other> other) {
    return this.parWith(other, Pair::of);
  }

  @Override
  public final String toString() {
    return String.format("(%s, %s)", this.getEndTime(), this.getDynamics());
  }
}
