package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Objects;

public class CellApplicator<EffectType, CellType extends Cell<EffectType, CellType>>
    implements Applicator<EffectType, CellType>
{
  private final CellType initialState;

  public CellApplicator(final CellType initialState) {
    this.initialState = Objects.requireNonNull(initialState);
  }

  @Override
  public CellType initial() {
    return this.initialState.duplicate();
  }

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
