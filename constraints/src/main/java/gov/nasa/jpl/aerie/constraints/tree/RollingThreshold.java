package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ConstraintResult;
import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.LinearEquation;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.constraints.time.Spans;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record RollingThreshold(Expression<Spans> spans, Expression<Duration> width, Expression<Duration> threshold, RollingThresholdAlgorithm algorithm) implements Expression<ConstraintResult> {

  public enum RollingThresholdAlgorithm {
    InputSpans,
    Hull
  }

  @Override
  public ConstraintResult evaluate(SimulationResults results, final Interval bounds, EvaluationEnvironment environment) {
    final var width = this.width.evaluate(results, bounds, environment);
    var spans = this.spans.evaluate(results, bounds, environment);
    final var threshold = this.threshold.evaluate(results, bounds, environment);

    final var accDuration = spans.accumulatedDuration(threshold);
    final var shiftedBack = accDuration.shiftBy(Duration.negate(width));

    final var localAccDuration = shiftedBack.plus(accDuration.times(-1));

    final var leftViolatingBounds = localAccDuration.greaterThan(new LinearProfile(Segment.of(Interval.FOREVER, new LinearEquation(Duration.ZERO, 1, 0))));

    final var violations = new ArrayList<Violation>();
    for (final var leftViolatingBound: leftViolatingBounds.iterateEqualTo(true)) {
      final var expandedInterval = Interval.between(leftViolatingBound.start, leftViolatingBound.startInclusivity, leftViolatingBound.end.plus(width), leftViolatingBound.endInclusivity);
      final var violationIntervals = new ArrayList<Interval>();
      final var violationActivityIds = new ArrayList<Long>();
      for (final var span: spans) {
        if (!Interval.intersect(span.interval(), expandedInterval).isEmpty()) {
          violationIntervals.add(span.interval());
          span.value().ifPresent(m -> violationActivityIds.add(m.activityInstance().id));
        }
      }
      if (this.algorithm == RollingThresholdAlgorithm.Hull) {
        final var hull = Interval.between(
            violationIntervals.get(0).start,
            violationIntervals.get(0).startInclusivity,
            violationIntervals.get(violationIntervals.size()-1).end,
            violationIntervals.get(violationIntervals.size()-1).endInclusivity
        );
        violationIntervals.clear();
        violationIntervals.add(hull);
      }
      final var violation = new Violation(violationIntervals, violationActivityIds);
      violations.add(violation);
    }

    return new ConstraintResult(violations, List.of());
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.spans.extractResources(names);
    this.width.extractResources(names);
    this.threshold.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(rolling-threshold on %s, width %s, threshold %s, algorithm %s)",
        prefix,
        this.spans.prettyPrint(prefix + "  "),
        this.width.prettyPrint(prefix + "  "),
        this.threshold.prettyPrint(prefix + "  "),
        this.algorithm
    );
  }
}
