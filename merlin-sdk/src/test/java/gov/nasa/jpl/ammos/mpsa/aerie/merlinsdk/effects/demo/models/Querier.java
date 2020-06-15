package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModelApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;

public final class Querier<T> {
  private final Query<T, Event, DataModel> dataQuery;

  public Querier(final SimulationTimeline<T, Event> timeline) {
    this.dataQuery = timeline.register(new DataEffectEvaluator(), new DataModelApplicator());
  }

  public DataModel getDataModel(final Time<T, Event> time) {
    return this.dataQuery.getAt(time);
  }
}
