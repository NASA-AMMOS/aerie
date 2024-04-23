package gov.nasa.jpl.aerie.scheduler.goals;

import gov.nasa.jpl.aerie.merlin.driver.*;
import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduling.EditablePlan;
import gov.nasa.jpl.aerie.scheduling.plan.Edit;
import gov.nasa.jpl.aerie.scheduling.plan.NewDirective;
import gov.nasa.jpl.aerie.scheduling.simulation.SimulateOptions;
import gov.nasa.jpl.aerie.timeline.Duration;
import gov.nasa.jpl.aerie.timeline.Interval;
import gov.nasa.jpl.aerie.timeline.collections.Directives;
import gov.nasa.jpl.aerie.timeline.collections.Instances;
import gov.nasa.jpl.aerie.timeline.ops.coalesce.CoalesceSegmentsOp;
import gov.nasa.jpl.aerie.timeline.payloads.Segment;
import gov.nasa.jpl.aerie.timeline.payloads.activities.AnyDirective;
import gov.nasa.jpl.aerie.timeline.payloads.activities.AnyInstance;
import gov.nasa.jpl.aerie.timeline.payloads.activities.Instance;
import gov.nasa.jpl.aerie.timeline.plan.SimulationResults;
import kotlin.jvm.functions.Function1;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.scheduler.goals.Procedure.$;

public record EditablePlanImpl(List<NewDirective> uncommitted, List<NewDirective> committed, MissionModel<?> missionModel, Interval planBounds, MutableObject<SimulationResults> latestSimulationResults) implements EditablePlan {
  public static EditablePlanImpl init(final MissionModel<?> missionModel, Interval planBounds, gov.nasa.jpl.aerie.merlin.driver.SimulationResults initialSimulationResults) {
    final var plan = new EditablePlanImpl(new ArrayList<>(), new ArrayList<>(), missionModel, planBounds, new MutableObject<>(null));
    if (initialSimulationResults != null) {
      plan.latestSimulationResults.setValue(adaptSimulationResults(initialSimulationResults, plan::toRelative));
    }
    return plan;
  }

  @Nullable
  @Override
  public SimulationResults latestResults() {
    return this.latestSimulationResults.getValue();
  }

  @Override
  public long create(@NotNull final NewDirective directive) {
    this.uncommitted.add(directive);
    return 0; // TODO generate an id
  }

  @Override
  public void commit() {
    this.committed.addAll(this.uncommitted);
    this.uncommitted.clear();
  }

  @NotNull
  @Override
  public List<Edit> rollback() {
    return null;
  }

  @NotNull
  @Override
  public SimulationResults simulate(@NotNull final SimulateOptions options) {
    this.latestSimulationResults.setValue(adaptSimulationResults(SimulationDriver.simulate(
        missionModel,
        Map.of(),
        this.toAbsolute(planBounds.start),
        $(planBounds.end),
        this.toAbsolute(planBounds.start),
        $(planBounds.end),
        () -> false), this::toRelative));
    return this.latestSimulationResults.getValue();
  }

  private static SimulationResults adaptSimulationResults(gov.nasa.jpl.aerie.merlin.driver.SimulationResults results, Function<Instant, Duration> toRelative) {
    return new SimulationResults() {
      @Override
      public boolean isStale() {
        return false;
      }

      @NotNull
      @Override
      public Interval simBounds() {
        return new Interval(Duration.ZERO, $(results.duration));
      }

      @NotNull
      @Override
      public <V, TL extends CoalesceSegmentsOp<V, TL>> TL resource(
          @NotNull final String name,
          @NotNull final Function1<? super List<Segment<SerializedValue>>, ? extends TL> deserializer)
      {
        List<ProfileSegment<SerializedValue>> originalProfile;
        if (results.discreteProfiles.containsKey(name)) originalProfile = results.discreteProfiles.get(name).getRight();
        else if (results.realProfiles.containsKey(name)) originalProfile = results.realProfiles
            .get(name)
            .getRight()
            .stream()
            .map($ -> new ProfileSegment<>(
                $.extent(),
                SerializedValue.of(Map.of(
                    "initial",
                    SerializedValue.of($.dynamics().initial),
                    "rate",
                    SerializedValue.of($.dynamics().rate)))))
            .toList();
        else throw new IllegalArgumentException("No such resource " + name);

        final var profile = new ArrayList<Segment<SerializedValue>>();
        var elapsedTime = gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
        for (final var entry : originalProfile) {
          profile.add(new Segment<>(
              new Interval($(elapsedTime), $(elapsedTime.plus(entry.extent()))),
              entry.dynamics()));
          elapsedTime = elapsedTime.plus(entry.extent());
        }
        return deserializer.invoke(profile);
      }

      @NotNull
      @Override
      public <A> Instances<A> instances(
          @Nullable final String type,
          @NotNull final Function1<? super SerializedValue, ? extends A> deserializer)
      {
        throw new NotImplementedException();
      }

      @NotNull
      @Override
      public Instances<AnyInstance> instances(@NotNull final String type) {
        return instances(Optional.of(type));
      }

      private Instances<AnyInstance> instances(Optional<String> typeFilter) {
        record FinishedActivityAttributes(
            gov.nasa.jpl.aerie.merlin.protocol.types.Duration duration,
            SerializedValue computedAttributes
        ) {}
        record CommonActivity(
            Map<String, SerializedValue> arguments,
            String type,
            Optional<Long> directiveId,
            long spanId,
            Instant startTime,
            Optional<FinishedActivityAttributes> finishedActivityAttributes
        ) {}

        final var activities = new ArrayList<CommonActivity>();

        for (final var entry : results.simulatedActivities.entrySet()) {
          final SimulatedActivity a = entry.getValue();
          activities.add(new CommonActivity(
              a.arguments(),
              a.type(),
              a.directiveId().map(ActivityDirectiveId::id),
              entry.getKey().id(),
              a.start(),
              Optional.of(new FinishedActivityAttributes(a.duration(), a.computedAttributes()))));
        }

        for (final var entry : results.unfinishedActivities.entrySet()) {
          final UnfinishedActivity a = entry.getValue();
          activities.add(new CommonActivity(
              a.arguments(),
              a.type(),
              a.directiveId().map(ActivityDirectiveId::id),
              entry.getKey().id(),
              a.start(),
              Optional.empty()));
        }

        final var instances = new ArrayList<Instance<AnyInstance>>();
        for (final var a : activities) {
          if (typeFilter.isPresent() && !a.type().equals(typeFilter.get())) continue;
          final Duration startTime = toRelative.apply(a.startTime);
          final Duration endTime = a.finishedActivityAttributes
              .map(FinishedActivityAttributes::duration)
              .map(x -> startTime.plus($(x)))
              .orElse(Duration.MAX_VALUE);
          final SerializedValue computedAttributes = a.finishedActivityAttributes
              .map(FinishedActivityAttributes::computedAttributes)
              .orElse(SerializedValue.of(Map.of()));
          instances.add(new Instance<>(
              new AnyInstance(a.arguments, computedAttributes),
              a.type,
              a.spanId,
              a.directiveId.orElse(null),
              new Interval(startTime, endTime)));
        }

        return new Instances<>(instances);
      }

      @NotNull
      @Override
      public Instances<AnyInstance> instances() {
        return instances(Optional.empty());
      }
    };
  }

  @Override
  public int getId() {
    return 0;
  }

  @NotNull
  @Override
  public Interval totalBounds() {
    return null;
  }

  @NotNull
  @Override
  public Duration toRelative(@NotNull final Instant abs) {
    return null;
  }

  @NotNull
  @Override
  public Instant toAbsolute(@NotNull final Duration rel) {
    return null;
  }

  @NotNull
  @Override
  public <A> Directives<A> directives(
      @Nullable final String type,
      @NotNull final Function1<? super SerializedValue, ? extends A> deserializer)
  {
    return null;
  }

  @NotNull
  @Override
  public Directives<AnyDirective> directives(@NotNull final String type) {
    return null;
  }

  @NotNull
  @Override
  public Directives<AnyDirective> directives() {
    return null;
  }
}
