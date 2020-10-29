package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.CollectingEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.ConcurrentUpdateTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.SumEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealSolver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics.delimited;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics.persistent;

final class DurativeRealModel implements Model<Collection<DelimitedDynamics<RealDynamics>>, DurativeRealModel> {
  private final PriorityQueue<Pair<Window, RealDynamics>> activeEffects;
  private Duration elapsedTime;

  public DurativeRealModel() {
    this.activeEffects = new PriorityQueue<>(Comparator.comparing(x -> x.getLeft().end));
    this.elapsedTime = Duration.ZERO;
  }

  private DurativeRealModel(final DurativeRealModel other) {
    this.activeEffects = new PriorityQueue<>(other.activeEffects);
    this.elapsedTime = other.elapsedTime;
  }

  @Override
  public DurativeRealModel duplicate() {
    return new DurativeRealModel(this);
  }

  @Override
  public EffectTrait<Collection<DelimitedDynamics<RealDynamics>>> effectTrait() {
    return new CollectingEffectTrait<>();
  }

  @Override
  public void react(final Collection<DelimitedDynamics<RealDynamics>> effects) {
    for (final var dynamics : effects) {
      this.activeEffects.add(Pair.of(
          Window.between(this.elapsedTime, dynamics.getEndTime().plus(this.elapsedTime)),
          dynamics.getDynamics()));
    }
  }

  @Override
  public void step(final Duration duration) {
    this.elapsedTime = this.elapsedTime.plus(duration);

    final var iter = this.activeEffects.iterator();
    while (iter.hasNext()) {
      final var entry = iter.next();
      if (this.elapsedTime.shorterThan(entry.getLeft().end)) break;
      iter.remove();
    }

    if (this.activeEffects.isEmpty()) {
      this.elapsedTime = Duration.ZERO;
    }
  }

  public static final RealResource<DurativeRealModel> value = (model) -> {
    var acc = persistent(RealDynamics.constant(0.0));

    for (final var entry : model.activeEffects) {
      final var x = delimited(
          entry.getLeft().end.minus(model.elapsedTime),
          entry.getRight());
      acc = acc.parWith(x, RealDynamics::plus);
    }

    return acc;
  };
}

final class RegisterModel<T> implements Model<Pair<Optional<T>, Set<T>>, RegisterModel<T>> {
  private T _value;
  private boolean _conflicted;

  public RegisterModel(final T initialValue, final boolean conflicted) {
    this._value = initialValue;
    this._conflicted = conflicted;
  }

  public RegisterModel(final T initialValue) {
    this(initialValue, false);
  }

  @Override
  public RegisterModel<T> duplicate() {
    return new RegisterModel<>(this._value, this._conflicted);
  }

  @Override
  public ConcurrentUpdateTrait<T> effectTrait() {
    return new ConcurrentUpdateTrait<>();
  }

  @Override
  public void react(final Pair<Optional<T>, Set<T>> concurrentValues) {
    concurrentValues.getLeft().ifPresent(newValue -> this._value = newValue);
    this._conflicted = (concurrentValues.getRight().size() > 1);
  }


  /// Resources
  public static <T> DiscreteResource<RegisterModel<T>, T> value() {
    return (model) -> persistent(model._value);
  }

  public static DiscreteResource<RegisterModel<?>, Boolean> conflicted =
      (model) -> persistent(model._conflicted);
}

public final class DataModel implements Model<Double, DataModel> {
  private double _volume;
  private double _rate;

  public DataModel(final double volume, final double rate) {
    this._volume = volume;
    this._rate = rate;
  }

  @Override
  public DataModel duplicate() {
    return new DataModel(this._volume, this._rate);
  }

  @Override
  public EffectTrait<Double> effectTrait() {
    return new SumEffectTrait();
  }

  @Override
  public void react(final Double delta) {
    this._rate += delta;
  }

  @Override
  public void step(final Duration elapsedTime) {
    // Law: The passage of time shall not alter a valid dynamics.
    this._volume = new RealSolver().valueAt(DataModel.volume.getDynamics(this).getDynamics(), elapsedTime);
  }


  /// Resources
  public static final RealResource<DataModel> volume =
      (model) -> persistent(RealDynamics.linear(model._volume, model._rate));

  public static final RealResource<DataModel> rate =
      (model) -> persistent(RealDynamics.constant(model._rate));
}

final class Foo {
  public static void main(final String[] args) {
    if (false) {
      foo(SimulationTimeline.create());
    }

    if (true) {
      try {
        bar(SimulationTimeline.create(), new FooAdaptation());
      } catch (final ActivityType.UnconstructableActivityException ex) {
        ex.printStackTrace();
      }
    }
  }

  private static <$Timeline, Event, Activity extends ActivityInstance>
  void
  bar(
      final SimulationTimeline<$Timeline, Event> timeline,
      final Adaptation<Event, Activity> adaptation)
  throws ActivityType.UnconstructableActivityException
  {
    final var context = adaptation.<$Timeline, String>initializeSimulation(timeline);

    final var activity = adaptation
        .getActivityTypes()
        .get("foo")
        .instantiate(Map.of());

    final var task = context.constructActivityTask(activity);

    final var scheduler = new Scheduler<$Timeline, String, Event, Activity>() {
      // TODO: Track and reduce candelabras of spawned tasks
      public History<$Timeline, Event> now = timeline.origin();

      @Override
      public void emit(final Event event) {
        now = now.emit(event);
      }

      @Override
      public String spawn(final Activity activity) {
        // TODO: Register the spawned activity and give it a name.
        return "";
      }

      @Override
      public String defer(final Duration delay, final Activity activity) {
        // TODO: Register the spawned activity and give it a name.
        return "";
      }

      @Override
      public History<$Timeline, Event> now() {
        return this.now;
      }
    };

    boolean running = true;
    while (running) {
      // TODO: Emit an "activity start" event.
      running = task.step(scheduler).match(new ActivityStatus.Visitor<>() {
        @Override
        public Boolean completed() {
          // TODO: Emit an "activity end" event.
          return false;
        }

        @Override
        public Boolean awaiting(final String s) {
          // TODO: Yield this task if the awaited activity is not yet complete.
          return true;
        }

        @Override
        public Boolean delayed(final Duration delay) {
          // TODO: Yield this task and perform any other tasks between now and the resumption point.
          scheduler.now = scheduler.now.wait(delay);
          return true;
        }
      });

    }

    System.out.print(scheduler.now.getDebugTrace());
    context.getRealResources().forEach((name, resource) -> {
      System.out.printf("%-12s%s%n", name, resource.getDynamics(scheduler.now()));
    });
    context.getDiscreteResources().forEach((name, resource) -> {
      System.out.printf("%-12s%s%n", name, resource.getRight().getDynamics(scheduler.now()));
    });
  }

  private static <$> void foo(final SimulationTimeline<$, Double> timeline) {
    final var dataModel = timeline.register(new DataModel(0.0, 0.0), ev -> ev);
    final var fooModel = timeline.register(new RegisterModel<>(0.0), ev -> Pair.of(Optional.of(ev), Set.of(ev)));
    final var duraModel = timeline.register(
        new DurativeRealModel(),
        ev -> List.of(delimited(Duration.of(
            ((int) (double) ev) * 100,
            Duration.MILLISECONDS), RealDynamics.linear(ev, ev))));

    final var dataVolume = DataModel.volume.connect(dataModel);
    final var dataRate = DataModel.rate.connect(dataModel);
    final var combo = dataVolume.plus(dataRate);

    final var foo = RegisterModel.<Double>value().connect(fooModel);
    final var bar = RegisterModel.conflicted.connect(fooModel);

    final var baz = DurativeRealModel.value.connect(duraModel);

    {
      final var now = timeline
          .origin()
          .branching(
              base -> base.emit(1.0),
              base -> base.emit(2.0));

      System.out.print(now.getDebugTrace());

      System.out.println(foo.getDynamics(now));
      System.out.println(bar.getDynamics(now));
      System.out.println();
    }

    {
      final var now = timeline
          .origin()
          .emit(1.0)
          .wait(1, Duration.SECONDS)
          .emit(2.0)
          .wait(100, Duration.MILLISECONDS);

      System.out.print(now.getDebugTrace());

      System.out.println(dataVolume.getDynamics(now));
      System.out.println(dataRate.getDynamics(now));
      System.out.println(combo.getDynamics(now));
      System.out.println(foo.getDynamics(now));
      System.out.println(bar.getDynamics(now));
    }

    {
      var now = timeline
          .origin()
          .emit(1.0)
          .emit(2.0)
          .emit(3.0);

      System.out.println(baz.getDynamics(now));

      now = now.wait(100, Duration.MILLISECONDS);
      System.out.println(baz.getDynamics(now));

      now = now.wait(100, Duration.MILLISECONDS);
      System.out.println(baz.getDynamics(now));

      now = now.wait(100, Duration.MILLISECONDS);
      System.out.println(baz.getDynamics(now));
    }
  }
}

final class FooAdaptation implements Adaptation<Double, FooActivity> {
  public @Override
  Map<String, ActivityType<FooActivity>>
  getActivityTypes()
  {
    return Map.of("foo", new ActivityType<>() {
      @Override
      public String getName() {
        return "foo";
      }

      @Override
      public Map<String, ValueSchema> getParameters() {
        return Map.of();
      }

      @Override
      public FooActivity instantiate(final Map<String, SerializedValue> arguments) {
        return new FooActivity();
      }
    });
  }

  public @Override
  <$Timeline, ActivityId>
  FooSimulationContext<$Timeline, ActivityId>
  initializeSimulation(final SimulationTimeline<$Timeline, Double> timeline)
  {
    return new FooSimulationContext<>(timeline);
  }
}

final class FooSimulationContext<$Timeline, $ActivityId>
    implements SimulationContext<$Timeline, FooActivity, FooTask<$Timeline, $ActivityId>>
{
  // Framework should provide a way to easily allow resources to be declared.
  // See here for a useful trick: use superclass methods from instance field initializers.
  //   https://stackoverflow.com/questions/15682457/initialize-field-before-super-constructor-runs
  public final RealResource<History<$Timeline, ?>> dataVolume;
  public final RealResource<History<$Timeline, ?>> dataRate;
  public final RealResource<History<$Timeline, ?>> combo;
  public final DiscreteResource<History<$Timeline, ?>, Double> foo;
  public final DiscreteResource<History<$Timeline, ?>, Boolean> bar;

  // Need a clear story for how to logically group resource questions and event emissions together.
  // Need a clear story for how these logical groups are made available to an activity.
  // Need a way to produce a condition for a resource.
  // Need a way to assemble conditions into an overall constraint.
  // Need a way to extract constraints from an adaptation.

  public FooSimulationContext(final SimulationTimeline<$Timeline, Double> timeline) {
    final var dataModel = timeline.register(new DataModel(0.0, 0.0), ev -> ev);
    final var fooModel = timeline.register(new RegisterModel<>(0.0), ev -> Pair.of(Optional.of(ev), Set.of(ev)));

    this.dataVolume = DataModel.volume.connect(dataModel);
    this.dataRate = DataModel.rate.connect(dataModel);
    this.combo = dataVolume.plus(dataRate);

    this.foo = RegisterModel.<Double>value().connect(fooModel);
    this.bar = RegisterModel.conflicted.connect(fooModel);
  }

  public @Override
  FooTask<$Timeline, $ActivityId>
  constructActivityTask(final FooActivity activity)
  {
    return new FooTask<>(this, activity);
  }

  public @Override
  FooTask<$Timeline, $ActivityId>
  duplicateActivityTask(final FooTask<$Timeline, $ActivityId> task)
  {
    return new FooTask<>(task);
  }

  public @Override
  Map<String, Pair<ValueSchema, DiscreteResource<History<$Timeline, ?>, SerializedValue>>>
  getDiscreteResources()
  {
    return Map.of(
        "foo", Pair.of(ValueSchema.REAL, this.foo.map(SerializedValue::of)),
        "bar", Pair.of(ValueSchema.BOOLEAN, this.bar.map(SerializedValue::of)));
  }

  public @Override
  Map<String, RealResource<History<$Timeline, ?>>>
  getRealResources()
  {
    return Map.of(
        "dataVolume", this.dataVolume,
        "dataRate", this.dataRate,
        "combo", this.combo);
  }
}

final class FooTask<$Timeline, $ActivityId>
    implements Task<$Timeline, $ActivityId, Double, FooActivity>
{
  private final FooSimulationContext<$Timeline, ?> resources;
  private final FooActivity activity;
  private int state;

  public FooTask(final FooSimulationContext<$Timeline, ?> resources, final FooActivity activity, final int state) {
    this.resources = resources;
    this.activity = activity;
    this.state = state;
  }

  public FooTask(final FooTask<$Timeline, $ActivityId> other) {
    this(other.resources, other.activity, other.state);
  }

  public FooTask(final FooSimulationContext<$Timeline, ?> resources, final FooActivity activity) {
    this(resources, activity, 0);
  }

  public @Override
  ActivityStatus<$ActivityId>
  step(final Scheduler<$Timeline, $ActivityId, Double, FooActivity> scheduler)
  {
    switch (this.state) {
      case 0:
        scheduler.emit(1.0);
        this.state = 1;
        return ActivityStatus.delayed(1, Duration.SECOND);

      case 1:
        scheduler.emit(2.0);

        final var delimitedDynamics = this.resources.dataRate.getDynamics(scheduler.now());
        final var rate = new RealSolver().valueAt(delimitedDynamics.getDynamics(), Duration.ZERO);
        scheduler.emit(rate);

        this.state = 2;
        return ActivityStatus.delayed(200, Duration.MILLISECONDS);

      case 2:
      default:
        return ActivityStatus.completed();
    }
  }
}

final class FooActivity implements ActivityInstance {
  public @Override
  SerializedActivity
  serialize()
  {
    return new SerializedActivity("foo", Map.of());
  }
}
