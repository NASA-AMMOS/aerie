package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Flag;
import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Validation;
import gov.nasa.jpl.aerie.merlin.protocol.model.DurationSpecification;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

/**
 * Bite a banana.
 *
 * This activity causes a piece of banana to be bitten off and consumed.
 *
 * @subsystem fruit
 * @contact John Doe
 */
@ActivityType("BiteBanana")
public final class BiteBananaActivity {

  DurationSpecification getDurationSpecification() {
    return new DurationSpecification() {
      @Override
      public DurationType getDurationType() {
        return DurationType.Constant;
      }

      @Override
      public DurationBounds getDurationBounds() {
        return new DurationBounds(Duration.ZERO, Duration.ZERO);
      }
    };
  }

  @Parameter
  public double biteSize = 1.0;

  @Validation("bite size must be positive")
  public boolean validateBiteSize() {
    return this.biteSize > 0;
  }

  @EffectModel
  public void run(final Mission mission) {
    mission.flag.set((biteSize > 1.0) ? Flag.B : Flag.A);
    mission.fruit.subtract(biteSize);
  }
}
