package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Resources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models.DataModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models.DurativeRealModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models.RegisterModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealSolver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.BooleanValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleValueMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics.delimited;

public final class Foo {
  public static void main(final String[] args) {
    if (false) {
      foo(SimulationTimeline.create());
    }

    if (true) {
      try {
        bar(FooAdaptation.create());
      } catch (final ActivityType.UnconstructableActivityException ex) {
        ex.printStackTrace();
      }
    }
  }

  private static <$Schema, Event, Activity extends ActivityInstance>
  void bar(final Adaptation<$Schema, Event, Activity> adaptation)
  throws ActivityType.UnconstructableActivityException
  {
    baz(SimulationTimeline.create(adaptation.getResources().getSchema()), adaptation);
  }

  private static <$Timeline, Event, Activity extends ActivityInstance>
  void
  baz(
      final SimulationTimeline<$Timeline, Event> timeline,
      final Adaptation<? super $Timeline, Event, Activity> adaptation)
  throws ActivityType.UnconstructableActivityException
  {
    final var activity = adaptation
        .getActivityTypes()
        .get("foo")
        .instantiate(Map.of());

    final var task = adaptation.<$Timeline>createActivityTask(activity);

    final var scheduler = new Scheduler<$Timeline, Event, Activity>() {
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

    final var visitor = new ActivityStatus.Visitor<Boolean>() {
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
    };

    boolean running = true;
    while (running) {
      // TODO: Emit an "activity start" event.
      running = task.step(scheduler).match(visitor);
    }

    final var resources = adaptation.getResources();
    System.out.print(scheduler.now.getDebugTrace());
    resources.getRealResources().forEach((name, resource) -> {
      System.out.printf("%-12s%s%n", name, resource.getDynamics(scheduler.now()));
    });
    resources.getDiscreteResources().forEach((name, resource) -> {
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

final class FooResources<$Schema> extends Resources<$Schema, Double> {
  // Need a clear story for how to logically group resource questions and event emissions together.
  // Need a clear story for how these logical groups are made available to an activity.
  // Need a way to produce a condition for a resource.
  // Need a way to assemble conditions into an overall constraint.
  // Need a way to extract constraints from an adaptation.

  public FooResources(final Schema.Builder<$Schema, Double> builder) {
    super(builder);
  }

  private final Query<$Schema, DataModel>
      dataModel = model(new DataModel(0.0, 0.0), ev -> ev);
  private final Query<$Schema, RegisterModel<Double>>
      fooModel = model(new RegisterModel<>(0.0), ev -> Pair.of(Optional.of(ev), Set.of(ev)));

  public final RealResource<History<? extends $Schema, ?>>
      dataVolume = resource("volume", dataModel, DataModel.volume);
  public final RealResource<History<? extends $Schema, ?>>
      dataRate = resource("rate", dataModel, DataModel.rate);
  public final RealResource<History<? extends $Schema, ?>>
      combo = resource("combo", dataVolume.plus(dataRate));

  public final DiscreteResource<History<? extends $Schema, ?>, Double>
      foo = resource("foo", fooModel, RegisterModel.value(), new DoubleValueMapper());
  public final DiscreteResource<History<? extends $Schema, ?>, Boolean>
      bar = resource("bar", fooModel, RegisterModel.conflicted, new BooleanValueMapper());
}

final class FooAdaptation<$Schema> implements Adaptation<$Schema, Double, FooActivity> {
  private final FooResources<$Schema> resources;

  private FooAdaptation(final FooResources<$Schema> resources) {
    this.resources = resources;
  }

  public static FooAdaptation<?> create() {
    return new FooAdaptation<>(new FooResources<>(Schema.builder()));
  }

  @Override
  public FooResources<$Schema> getResources() {
    return this.resources;
  }

  public @Override
  Map<String, ActivityType<FooActivity>>
  getActivityTypes() {
    return FooActivity.getActivityTypes();
  }

  public @Override
  <$Timeline extends $Schema>
  FooTask<$Timeline>
  createActivityTask(FooActivity activity) {
    return new FooTask<>(resources, activity);
  }
}

final class FooTask<$Timeline>
    implements Task<$Timeline, Double, FooActivity>
{
  private final FooResources<? super $Timeline> resources;
  private final FooActivity activity;
  private int state;

  public FooTask(final FooResources<? super $Timeline> resources, final FooActivity activity) {
    this.resources = resources;
    this.activity = activity;
    this.state = 0;
  }

  public @Override
  ActivityStatus
  step(final Scheduler<$Timeline, Double, FooActivity> scheduler) {
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
  serialize() {
    return new SerializedActivity("foo", Map.of());
  }

  public static
  Map<String, ActivityType<FooActivity>>
  getActivityTypes() {
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
}
