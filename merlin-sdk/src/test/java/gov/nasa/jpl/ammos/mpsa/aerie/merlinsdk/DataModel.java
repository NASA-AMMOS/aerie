package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;
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
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiFunction;

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

  private static <A, B, C>
  BiFunction<Optional<A>, Optional<B>, Optional<C>> maybeApply(BiFunction<A, B, C> f) {
    return ($a, $b) -> $a.flatMap(a -> $b.map(b -> f.apply(a, b)));
  }

  private static <A, B, C>
  Optional<C> parWith(Optional<A> $a, Optional<B> $b, BiFunction<A, B, C> f) {
    return $a.flatMap(a -> $b.map(b -> f.apply(a, b)));
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
    foo(SimulationTimeline.create());
  }

  private static <$> void foo(final SimulationTimeline<$, Double> timeline) {
    final var dataModel = timeline.register(new DataModel(0.0, 0.0), ev -> ev);
    final var fooModel = timeline.register(new RegisterModel<>(0.0), ev -> Pair.of(Optional.of(ev), Set.of(ev)));
    final var duraModel = timeline.register(new DurativeRealModel(), ev -> List.of(delimited(Duration.of(((int) (double) ev) * 100, Duration.MILLISECONDS), RealDynamics.linear(ev, ev))));

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
