package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.DynamicReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModelApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModelQuerier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DynamicDataModelQuerier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell;

import java.util.function.Supplier;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell.setDynamic;

public final class Querier<T> {
  private static final DynamicCell<ReactionContext<?, Activity, Event>> reactionContext = DynamicCell.create();
  private static final DynamicCell<InnerQuerier<?>> queryContext = DynamicCell.create();

  public static final ReactionContext<?, Activity, Event> ctx = new DynamicReactionContext<>(reactionContext::get);
  public static final DataModelQuerier dataQuerier = new DynamicDataModelQuerier(() -> queryContext.get().getDataQuerier());

  private final Query<T, Event, DataModel> dataQuery;

  public Querier(final SimulationTimeline<T, Event> timeline) {
    this.dataQuery = timeline.register(new DataEffectEvaluator(), new DataModelApplicator());
  }

  public void runActivity(final ReactionContext<T, Activity, Event> ctx, final Activity activity) {
    setDynamic(queryContext, new InnerQuerier<>(this, ctx::now), () ->
        setDynamic(reactionContext, ctx, () ->
            activity.modelEffects()));
  }

  public static final class InnerQuerier<T> {
    private final Querier<T> querier;
    private final Supplier<History<T, Event>> currentTime;

    private InnerQuerier(final Querier<T> querier, final Supplier<History<T, Event>> currentTime) {
      this.querier = querier;
      this.currentTime = currentTime;
    }

    public DataModelQuerier getDataQuerier() {
      return this.querier.dataQuery.getAt(this.currentTime.get());
    }
  }
}
