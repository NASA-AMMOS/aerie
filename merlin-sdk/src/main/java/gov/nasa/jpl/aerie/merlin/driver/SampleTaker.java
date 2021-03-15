package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.DiscreteApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.RealApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.time.Duration;
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
  private final Iterable<Pair<Duration, Dynamics>> profile;

  private SampleTaker(final Iterable<Pair<Duration, Dynamics>> profile) {
    this.profile = Objects.requireNonNull(profile);
  }

  public static <Dynamics>
  List<Pair<Duration, SerializedValue>> sample(
      final Iterable<Pair<Duration, Dynamics>> profile,
      final ResourceSolver<?, ?, Dynamics> solver)
  {
    return solver.approximate(new SampleTaker<>(profile));
  }

  public static <Dynamics>
  List<Pair<Duration, SerializedValue>> sample(
      final ProfileBuilder<?, ?, Dynamics> profile)
  {
    return sample(profile.pieces, profile.solver);
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

    var dynamicsStart = Duration.ZERO;
    final var dynamicsIter = this.profile.iterator();
    while (dynamicsIter.hasNext()) {
      final var entry = dynamicsIter.next();

      final var dynamicsEnd = dynamicsStart.plus(entry.getLeft());
      final var dynamics = entry.getRight();
      final var dynamicsOwnsEndpoint = !dynamicsIter.hasNext();

      final var approximation = approximate.apply(dynamics);

      var partStart = dynamicsStart;
      do {
        final var part = approximation.next();
        final var partEnd = Duration.min(
            (part.isPersistent()) ? Duration.MAX_VALUE : partStart.plus(part.extent),
            dynamicsEnd);

        timeline.add(Pair.of(partStart, takeSample.apply(part.dynamics, Duration.ZERO)));

        if (!partEnd.isEqualTo(partStart)) {
          timeline.add(Pair.of(partEnd, takeSample.apply(part.dynamics, partEnd.minus(partStart))));
        }

        partStart = partEnd;
      } while (approximation.hasNext() && (partStart.shorterThan(dynamicsEnd) || dynamicsOwnsEndpoint));

      dynamicsStart = dynamicsEnd;
    }

    return timeline;
  }
}
