package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModelApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;

import java.util.function.Supplier;

public final class Querier<T> {
  private final Query<T, Event, DataModel> dataQuery;

  public Querier(final SimulationTimeline<T, Event> timeline) {
    this.dataQuery = timeline.register(new DataEffectEvaluator(), new DataModelApplicator());
  }

  public InnerQuerier<?> at(final Supplier<Time<T, Event>> currentTime) {
    return new InnerQuerier<>(this, currentTime);
  }

  public static final class InnerQuerier<T> {
    private final Querier<T> querier;
    private final Supplier<Time<T, Event>> currentTime;

    private InnerQuerier(final Querier<T> querier, final Supplier<Time<T, Event>> currentTime) {
      this.querier = querier;
      this.currentTime = currentTime;
    }

    public DataModel getDataModel() {
      return this.querier.dataQuery.getAt(currentTime.get());
    }
  }
}
