package gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;

/* package-local */
abstract class ActivePath<Effect> {
  private ActivePath() {}

  public abstract void accumulate(final Function<Effect, Effect> action);
  public abstract int basePoint();

  public static final class TopLevel<Effect> extends ActivePath<Effect> {
    public int base;
    public Effect effect;
    public final Deque<Pair<Duration, Effect>> effects = new ArrayDeque<>();

    public TopLevel(final int base, final Effect effect) {
      this.base = base;
      this.effect = effect;
    }

    @Override
    public void accumulate(final Function<Effect, Effect> action) {
      this.effect = action.apply(this.effect);
    }

    @Override
    public int basePoint() {
      return this.base;
    }
  }

  public static final class Left<Effect> extends ActivePath<Effect> {
    public final int base;
    public Effect left;
    public final int right;

    public Left(final int base, final Effect left, final int right) {
      this.base = base;
      this.left = left;
      this.right = right;
    }

    @Override
    public void accumulate(final Function<Effect, Effect> action) {
      this.left = action.apply(this.left);
    }

    @Override
    public int basePoint() {
      return this.base;
    }
  }

  public static final class Right<Effect> extends ActivePath<Effect> {
    public final int base;
    public final Effect left;
    public Effect right;

    public Right(final int base, final Effect left, final Effect right) {
      this.base = base;
      this.left = left;
      this.right = right;
    }

    public void accumulate(final Function<Effect, Effect> action) {
      this.right = action.apply(this.right);
    }

    @Override
    public int basePoint() {
      return this.base;
    }
  }
}
