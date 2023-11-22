package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.set;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentData;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

@ActivityType("ChangeApproximationInput")
public class ChangeApproximationInput {
  @Export.Parameter
  public double[] coefficients;

  @Export.Parameter
  public boolean doFirstDummyUpdate = false;

  @Export.Parameter
  public boolean doSecondDummyUpdate = false;


  @Export.Parameter
  public boolean doDelay = false;
  @Export.Parameter
  public boolean doSecondDelay = false;

  @ActivityType.EffectModel
  public void run(Mission mission) {
    // Emit a "do-nothing" effect, just to try to confuse the machinery behind the scenes
    if (doFirstDummyUpdate) set(mission.approximationModel.polynomial, currentData(mission.approximationModel.polynomial));
    if (doDelay) delay(ZERO);
    if (doSecondDummyUpdate) set(mission.approximationModel.polynomial, currentData(mission.approximationModel.polynomial));
    if (doSecondDelay) delay(ZERO);
    set(mission.approximationModel.polynomial, polynomial(coefficients));
  }
}
