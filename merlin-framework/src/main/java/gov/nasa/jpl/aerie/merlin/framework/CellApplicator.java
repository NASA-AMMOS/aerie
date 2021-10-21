package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public class CellApplicator<EffectType, CellType extends Cell<EffectType, CellType>>
    implements Applicator<EffectType, CellType>
{
  @Override
  public CellType duplicate(final CellType cell) {
    return cell.duplicate();
  }

  @Override
  public void apply(final CellType cell, final EffectType effect) {
    cell.react(effect);
  }

  @Override
  public void step(final CellType cell, final Duration duration) {
    cell.step(duration);
  }
}
