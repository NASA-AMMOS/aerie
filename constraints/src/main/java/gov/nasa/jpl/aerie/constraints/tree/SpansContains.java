package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Spans;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;


public record SpansContains(Expression<Spans> parents, Expression<Spans> children, Requirement requirement) implements Expression<Windows> {
  public record Requirement(
      Optional<Integer> minCount,
      Optional<Integer> maxCount,
      Optional<Expression<Duration>> minDur,
      Optional<Expression<Duration>> maxDur
  ) {
    public static SpansContains.Requirement newDefault() {
      return new Requirement(
          Optional.of(1),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
      );
    }
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    final var parents = this.parents.evaluate(results, bounds, environment);
    final var children = this.children.evaluate(results, bounds, environment);
    var falseIntervals = new ArrayList<Interval>();


    if (this.requirement.minCount.isPresent() || this.requirement.maxCount.isPresent()) {
      final var sortedParents = StreamSupport.stream(parents.spliterator(), true).sorted((l, r) -> l
          .interval()
          .compareStarts(r.interval())).toList();
      final var sortedChildren = StreamSupport.stream(children.spliterator(), true).sorted((l, r) -> l
          .interval()
          .compareStarts(r.interval())).toList();
      var childIndex = 0;

      for (final var parent: sortedParents) {
        final var parentInterval = parent.interval();
        var childInterval = sortedChildren.get(childIndex).interval();
        while (childInterval.compareStarts(parentInterval) == -1) {
          childIndex++;
          childInterval = sortedChildren.get(childIndex).interval();
        }

        var instanceCount = new AtomicInteger(0);
        var transientChildIndex = childIndex;
        while (parentInterval.contains(childInterval)) {
          instanceCount.getAndIncrement();
          transientChildIndex++;
          childInterval = sortedChildren.get(transientChildIndex).interval();
        }

        if (this.requirement.minCount.map(c -> instanceCount.get() < c).orElse(false)) {
          falseIntervals.add(parentInterval);
        } else if (this.requirement.maxCount.map(c -> instanceCount.get() > c).orElse(false)) {
          falseIntervals.add(parentInterval);
        }
      }
    }
    if (this.requirement.minDur().isPresent() || this.requirement.maxDur.isPresent()) {
      final var accumulatedDuration = children.accumulatedDuration(Duration.MICROSECOND);
      final Optional<Long> minDur;
      final Optional<Long> maxDur;
      if (this.requirement.minDur().isPresent()) {
        minDur = Optional.of(this.requirement.minDur().get().evaluate(results, bounds, environment).dividedBy(Duration.MICROSECOND));
      } else {
        minDur = Optional.empty();
      }
      if (this.requirement.maxDur().isPresent()) {
        maxDur = Optional.of(this.requirement.maxDur().get().evaluate(results, bounds, environment).dividedBy(Duration.MICROSECOND));
      } else {
        maxDur = Optional.empty();
      }
      for (final var parent: parents) {
        final var parentInterval = parent.interval();
        final var startAcc = accumulatedDuration.valueAt(parentInterval.start).get().asReal().get();
        final var endAcc = accumulatedDuration.valueAt(parentInterval.end).get().asReal().get();

        if (minDur.isPresent() && endAcc - startAcc < minDur.get()) {
          falseIntervals.add(parentInterval);
        } else if (maxDur.isPresent() && endAcc - startAcc > maxDur.get()) {
          falseIntervals.add(parentInterval);
        }
      }
    }

    return (new Windows(bounds, true)).set(falseIntervals, false);
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.parents.extractResources(names);
    this.children.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(spans-contains require %s of %s in %s)",
        prefix,
        this.requirement.toString(),
        this.children.prettyPrint(),
        this.parents.prettyPrint()
    );
  }
}