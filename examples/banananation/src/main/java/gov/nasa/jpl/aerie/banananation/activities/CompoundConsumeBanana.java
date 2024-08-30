package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Compound;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

@ActivityType("ConsumeBanana")
@Compound(shouldBeDecomposed = false)
public class CompoundConsumeBanana {
  @Export.Parameter
  public int howMany;
  public int biteSize;
  //parameters can be validated as in regular primitives, that's pretty good

}
