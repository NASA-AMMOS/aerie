package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.UUID;

import static gov.nasa.jpl.aerie.banananation.generated.ActivityActions.call;
import static gov.nasa.jpl.aerie.banananation.generated.ActivityActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

@ActivityType("BananaPhotographer")
public class BananaPhotographer {

  @EffectModel
  public void run(Mission mission) {
    final var filename = call(new TakePicture());
    delay(Duration.of(2, Duration.HOURS));
    spawn(new MakeGif()
              .withFilename(filename));
  }


  @ActivityType("TakePicture")
  public static class TakePicture {

    @EffectModel
    public String run(Mission mission) {
      return UUID.randomUUID() + ".jpg";
    }
  }

  @ActivityType("MakeGif")
  public static class MakeGif {

    @Parameter
    public String filename = "";

    public MakeGif() {
    }

    MakeGif withFilename(String filename) {
      this.filename = filename;
      return this;
    }

    @EffectModel
    public boolean run(Mission mission) {
      // closefile;
      boolean success = true;
      return success;
    }
  }
}
