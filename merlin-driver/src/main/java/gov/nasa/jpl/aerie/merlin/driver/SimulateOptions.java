package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SimulateOptions<Model> {
  private final MissionModel<Model> missionModel;
  private final Instant planStartTime;
  private Optional<Map<ActivityDirectiveId, ActivityDirective>> schedule = Optional.empty();
  private final Duration planDuration;
  private Optional<Instant> simulationStartTime = Optional.empty();
  private Optional<Duration> simulationDuration = Optional.empty();
  private Optional<Supplier<Boolean>> simulationCanceled = Optional.empty();
  private Optional<Consumer<Duration>> simulationExtentConsumer = Optional.empty();

  private SimulateOptions(MissionModel<Model> missionModel, Instant planStartTime, Duration planDuration) {
    this.missionModel = missionModel;
    this.planStartTime = planStartTime;
    this.planDuration = planDuration;
  }

  public static <Model> SimulateOptions<Model> of(MissionModel<Model> missionModel, Instant planStartTime, Duration planDuration) {
    return new SimulateOptions<>(missionModel, planStartTime, planDuration);
  }

  public SimulateOptions<Model> schedule(Map<ActivityDirectiveId, ActivityDirective> schedule) {
    this.schedule = Optional.of(schedule);
    return this;
  }

  public SimulateOptions<Model> simulationStartTime(Instant simulationStartTime) {
    this.simulationStartTime = Optional.of(simulationStartTime);
    return this;
  }

  public SimulateOptions<Model> simulationDuration(Duration simulationDuration) {
    this.simulationDuration = Optional.of(simulationDuration);
    return this;
  }

  public SimulateOptions<Model> simulationCanceled(Supplier<Boolean> simulationCanceled) {
    this.simulationCanceled = Optional.of(simulationCanceled);
    return this;
  }

  public SimulateOptions<Model> simulationExtentConsumer(Consumer<Duration> simulationExtentConsumer) {
    this.simulationExtentConsumer = Optional.of(simulationExtentConsumer);
    return this;
  }

  MissionModel<Model> missionModel() {
    return this.missionModel;
  }

  Map<ActivityDirectiveId, ActivityDirective> schedule() {
    return this.schedule.orElse(Map.of());
  }

  Instant planStartTime() {
    return this.planStartTime;
  }

  Duration planDuration() {
    return this.planDuration;
  }

  Instant simulationStartTime() {
    return this.simulationStartTime.orElse(planStartTime);
  }

  Duration simulationDuration() {
    // default to (plan duration - (simulation start - plan start))
    return this.simulationDuration.orElse(planDuration.minus(Duration.of(this.planStartTime().until(this.simulationStartTime(), ChronoUnit.MICROS), Duration.MICROSECONDS)));
  }

  Supplier<Boolean> simulationCanceled() {
    return this.simulationCanceled.orElse(() -> false);
  }

  Consumer<Duration> simulationExtentConsumer() {
    return this.simulationExtentConsumer.orElse($ -> {});
  }
}
