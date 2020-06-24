package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.DynamicReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModelApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Function;
import java.util.function.Supplier;

public final class Querier<T> {
  private static final DynamicCell<Pair<ReactionContext<?, Activity, Event>, InnerQuerier<?>>> activeContext = DynamicCell.create();
  public static final Function<String, Double> getVolumeOf = (name) -> activeContext.get().getRight().getVolume(name);
  public static final Function<String, Double> getRateOf = (name) -> activeContext.get().getRight().getRate(name);
  public static final ReactionContext<?, Activity, Event> ctx = new DynamicReactionContext<>(() -> activeContext.get().getLeft());


  private final Query<T, Event, DataModel> dataQuery;

  public Querier(final SimulationTimeline<T, Event> timeline) {
    this.dataQuery = timeline.register(new DataEffectEvaluator(), new DataModelApplicator());
  }

  public InnerQuerier<?> at(final Supplier<Time<T, Event>> currentTime) {
    return new InnerQuerier<>(this, currentTime);
  }

  public void runActivity(final ReactionContext<T, Activity, Event> ctx, final Activity activity) {
    Querier.activeContext.setWithin(Pair.of(ctx, this.at(ctx::now)), activity::modelEffects);
  }

  public static final class InnerQuerier<T> {
    private final Querier<T> querier;
    private final Supplier<Time<T, Event>> currentTime;

    private InnerQuerier(final Querier<T> querier, final Supplier<Time<T, Event>> currentTime) {
      this.querier = querier;
      this.currentTime = currentTime;
    }

    public double getVolume(final String binName) {
      return this.querier.dataQuery.getAt(this.currentTime.get()).getDataBin(binName).getVolume();
    }

    public double getRate(final String binName) {
      return this.querier.dataQuery.getAt(this.currentTime.get()).getDataBin(binName).getRate();
    }
  }
}
