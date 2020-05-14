package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;

// This can be mechanically derived from `EventHandler`.
public abstract class MutatingProjection<Accumulator, Effect>
    extends gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.projections.MutatingProjection<Event, Accumulator, Effect>
{
  public MutatingProjection(final Projection<Event, Effect> projection) {
    super(projection);
  }
}
