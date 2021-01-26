package gov.nasa.jpl.aerie.fooadaptation.activities;

import gov.nasa.jpl.aerie.fooadaptation.Mission;
import gov.nasa.jpl.aerie.fooadaptation.generated.Task;
import gov.nasa.jpl.aerie.fooadaptation.models.ComplexData;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.Parameter;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.Validation;

import static gov.nasa.jpl.aerie.time.Duration.SECOND;

@ActivityType("foo")
public final class FooActivity {
  @Parameter
  public int x = 0;

  @Parameter
  public String y = "test";

  @Validation("x cannot be exactly 99")
  public boolean validateX() {
    return (x != 99);
  }

  @Validation("y cannot be 'bad'")
  public boolean validateY() {
    return !y.equals("bad");
  }

  public final class EffectModel extends Task {
    public void run(final Mission mission) {
      final var data = mission.data;
      final var complexData = mission.complexData;

      complexData.imagerHardwareState.set(ComplexData.ImagerHardwareState.ON);
      complexData.imagerResMode.set(ComplexData.ImagerResMode.HI_RES);
      complexData.imagerFrameRate.set(60.0);
      complexData.imagingInProgress.set(true);

      if (y.equals("test")) {
        data.rate.add(x);
      } else if (y.equals("spawn")) {
        call(new FooActivity());
      }

      data.rate.add(1.0);
      delay(1, SECOND);

      mission.simpleData.downlinkData();

      waitUntil(data.volume.isBetween(5.0, 10.0));
      data.rate.add(2.0);
      data.rate.add(data.rate.get());
      delay(10, SECOND);

      complexData.imagingInProgress.set(false);
      complexData.imagerHardwareState.set(ComplexData.ImagerHardwareState.OFF);

      mission.simpleData.toggleInstrumentA(false);
      mission.simpleData.toggleInstrumentB(false);
      delay(1, SECOND);

      mission.activitiesExecuted.add(1);
    }
  }
}
