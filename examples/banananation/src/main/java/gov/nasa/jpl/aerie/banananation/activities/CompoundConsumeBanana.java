package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Compound;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

@ActivityType("ConsumeBanana")
@Compound
public class CompoundConsumeBanana {
  @Export.Parameter
  public int howMany;
  //parameters can be validated as in regular primitives, that's pretty good

  //TODO: validation that there is no EffectModel so we can put an empty one ourselves
}
