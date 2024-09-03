package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Flag;
import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.contrib.metadata.Unit;
import gov.nasa.jpl.aerie.contrib.models.ValidationResult;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.AutoValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Validation;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

/**
 * Await a change in producer, then toggle flag
 *
 * The purpose of this activity is to demonstrate waitUntil
 */
@ActivityType("ToggleFlagWhenProducerChanges")
public final class ToggleFlagWhenProducerChanges {
  @EffectModel
  public void run(final Mission mission) {
    final var currentProducer = mission.producer.get();
    waitUntil(mission.producer.is(currentProducer).not());
    final var currentFlag = mission.flag.get();
    mission.flag.set(
        switch (currentFlag) {
          case A -> Flag.B;
          case B -> Flag.A;
        }
    );
  }
}
