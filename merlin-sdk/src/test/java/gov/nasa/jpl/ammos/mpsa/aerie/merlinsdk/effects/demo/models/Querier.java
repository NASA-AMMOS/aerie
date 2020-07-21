package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityModelApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityModelQuerier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.DynamicActivityModelQuerier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
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

import java.util.Collections;
import java.util.function.Supplier;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell.setDynamic;

public final class Querier<T> {
  private static final DynamicCell<ReactionContext<?, Activity, Event>> reactionContext = DynamicCell.create();
  private static final DynamicCell<InnerQuerier<?>> queryContext = DynamicCell.create();

  public static final ReactionContext<?, Activity, Event> ctx = new DynamicReactionContext<>(reactionContext::get);
  public static final DataModelQuerier dataQuerier = new DynamicDataModelQuerier(() -> queryContext.get().getDataQuerier());
  public static final ActivityModelQuerier activityQuerier = new DynamicActivityModelQuerier(() -> queryContext.get().getActivityQuerier());

  private final Query<T, Event, DataModel> dataQuery;
  private final Query<T, Event, ActivityModel> activityQuery;

  public Querier(final SimulationTimeline<T, Event> timeline) {
    this.dataQuery = timeline.register(
        new DataEffectEvaluator(),
        new DataModelApplicator());
    this.activityQuery = timeline.register(
        new ActivityEffectEvaluator().filterContramap(Event::asActivity),
        new ActivityModelApplicator());
  }

  public void runActivity(final ReactionContext<T, Activity, Event> ctx, final String activityId, final Activity activity) {
    setDynamic(queryContext, new InnerQuerier<>(this, ctx::now), () ->
        setDynamic(reactionContext, ctx, () -> {
          ctx.emit(Event.activity(ActivityEvent.startActivity(activityId, new SerializedActivity(activity.getClass().getName(), Collections.emptyMap()))));
          activity.modelEffects();
          ctx.waitForChildren();
          ctx.emit(Event.activity(ActivityEvent.endActivity(activityId)));
        }));
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

    public ActivityModelQuerier getActivityQuerier() {
      return this.querier.activityQuery.getAt(this.currentTime.get());
    }
  }
}
