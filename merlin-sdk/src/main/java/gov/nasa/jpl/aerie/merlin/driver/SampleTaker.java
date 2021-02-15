package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.DiscreteApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.RealApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class SampleTaker<Dynamics>
    implements ResourceSolver.ApproximatorVisitor<Dynamics, List<Pair<Duration, SerializedValue>>>
{
  private final Iterable<Pair<Window, Dynamics>> profile;
  private final Iterable<Duration> sampleTimes;

  private SampleTaker(final Iterable<Pair<Window, Dynamics>> profile, final Iterable<Duration> sampleTimes) {
    this.profile = Objects.requireNonNull(profile);
    this.sampleTimes = Objects.requireNonNull(sampleTimes);
  }

  public static <Dynamics>
  List<Pair<Duration, SerializedValue>> sample(
      final Iterable<Pair<Window, Dynamics>> profile,
      final ResourceSolver<?, ?, Dynamics, ?> solver,
      final Iterable<Duration> sampleTimes)
  {
    return solver.approximate(new SampleTaker<>(profile, sampleTimes));
  }

  public static <Dynamics>
  List<Pair<Duration, SerializedValue>> sample(
      final SimulationDriver.ProfileBuilder<?, ?, Dynamics, ?> profile,
      final Iterable<Duration> sampleTimes)
  {
    return sample(profile.pieces, profile.solver, sampleTimes);
  }

  @Override
  public List<Pair<Duration, SerializedValue>> real(final RealApproximator<Dynamics> approximator) {
    return takeSamples(
        dynamics -> approximator.approximate(dynamics).iterator(),
        (dynamics, offset) -> SerializedValue.of(
            dynamics.initial +
            dynamics.rate * offset.ratioOver(Duration.SECONDS)));
  }

  @Override
  public List<Pair<Duration, SerializedValue>> discrete(final DiscreteApproximator<Dynamics> approximator) {
    return takeSamples(
        dynamics -> approximator.approximate(dynamics).iterator(),
        (dynamics, offset) -> dynamics);
  }

  private <T>
  List<Pair<Duration, SerializedValue>> takeSamples(
      final Function<Dynamics, Iterator<DelimitedDynamics<T>>> approximate,
      final BiFunction<T, Duration, SerializedValue> takeSample)
  {
    final var timeline = new ArrayList<Pair<Duration, SerializedValue>>();

    final var sampleTimeIter = this.sampleTimes.iterator();
    if (!sampleTimeIter.hasNext()) {
      return timeline;
    }

    var sampleTime = sampleTimeIter.next();
    if (sampleTime.isNegative()) {
      throw new IllegalArgumentException("sample time is before start of profile");
    }

    final var dynamicsIter = this.profile.iterator();
    while (dynamicsIter.hasNext()) {
      final var entry = dynamicsIter.next();

      final var window = entry.getLeft();
      final var dynamics = entry.getRight();
      final var dynamicsOwnsEndpoint = !dynamicsIter.hasNext();

      final var approximation = approximate.apply(dynamics);

      var partStart = window.start;
      do {
        final var part = approximation.next();
        final var partEnd = Duration.min(
            (part.isPersistent()) ? Duration.MAX_VALUE : partStart.plus(part.endTime),
            window.end);
        final var partOwnsEndpoint = !approximation.hasNext() && dynamicsOwnsEndpoint;

        timeline.add(Pair.of(partStart, takeSample.apply(part.dynamics, Duration.ZERO)));

        while (sampleTime.shorterThan(partEnd) || (partOwnsEndpoint && sampleTime.isEqualTo(partEnd))) {
          if (!List.of(partStart, partEnd).contains(sampleTime)) {
            timeline.add(Pair.of(sampleTime, takeSample.apply(part.dynamics, sampleTime.minus(partStart))));
          }

          if (!sampleTimeIter.hasNext()) {
            // No other samples need to be taken.
            if (!partEnd.isEqualTo(partStart)) {
              timeline.add(Pair.of(partEnd, takeSample.apply(part.dynamics, partEnd.minus(partStart))));
            }
            return timeline;
          }

          final var nextSampleTime = sampleTimeIter.next();
          if (sampleTime.isNegative()) {
            throw new IllegalArgumentException("sample time is before start of profile");
          } else if (!sampleTime.shorterThan(nextSampleTime)) {
            throw new IllegalArgumentException("sample times must be strictly increasing");
          }

          sampleTime = nextSampleTime;
        }

        if (!partEnd.isEqualTo(partStart)) {
          timeline.add(Pair.of(partEnd, takeSample.apply(part.dynamics, partEnd.minus(partStart))));
        }

        partStart = partEnd;
      } while (sampleTime.shorterThan(window.end) || (dynamicsOwnsEndpoint && sampleTime.isEqualTo(window.end)));
    }

    throw new IllegalArgumentException("sample time is past end of profile");
  }
}
