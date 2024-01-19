package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

import static gov.nasa.jpl.aerie.banananation.generated.ActivityActions.call;

@ActivityType("Matryoshka")
public class MatryoshkaActivity {
  @Export.Parameter
  public BiteBananaActivity biteBananaActivity;

  @ActivityType.EffectModel
  public void run(Mission mission) {
    call(mission, biteBananaActivity);
  }
}
