package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Flag;
import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.call;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

/**
 * Spawns multiple anonymous subtasks
 */
@ActivityType("AsyncAnonymous")
public record AsyncAnonymous(Duration duration, String newProducerValue) {
  @EffectModel
  public void run(final Mission mission) {
    final var currentProducer = mission.producer.get();
    if (currentProducer.equals(newProducerValue)) {
      mission.producer.set(newProducerValue + "~");
    }
    spawn(() -> {
      waitUntil(mission.producer.is(newProducerValue));
      final var currentFlag = mission.flag.get();
      mission.flag.set(
          switch (currentFlag) {
            case A -> Flag.B;
            case B -> Flag.A;
          }
      );
    });
    spawn(() -> {
      delay(duration);
      mission.producer.set(newProducerValue);
    });
  }
}
