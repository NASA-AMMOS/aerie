package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Action;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

import java.util.Objects;
import java.util.function.Function;

public class SettableEffectTrait<Accumulator, Effect> implements EffectTrait<SettableEffect<Accumulator, Effect>> {
  private final EffectTrait<Effect> trait;
  private final Action<Accumulator, Effect> action;
  private final Function<Effect, Boolean> isEffectEmpty;

  public SettableEffectTrait(
      final EffectTrait<Effect> trait,
      final Action<Accumulator, Effect> action,
      final Function<Effect, Boolean> isEffectEmpty
  ) {
    this.trait = trait;
    this.action = action;
    this.isEffectEmpty = isEffectEmpty;
  }

  @Override
  public final SettableEffect<Accumulator, Effect> empty() {
    return SettableEffect.add(trait.empty());
  }

  @Override
  public final SettableEffect<Accumulator, Effect> sequentially(
      final SettableEffect<Accumulator, Effect> prefix,
      final SettableEffect<Accumulator, Effect> suffix
  ) {
    Objects.requireNonNull(prefix);
    Objects.requireNonNull(suffix);

    return suffix.visit(new SettableEffect.Visitor<>() {
      @Override
      public SettableEffect<Accumulator, Effect> conflict() {
        return suffix;
      }

      @Override
      public SettableEffect<Accumulator, Effect> setTo(final Accumulator suffixBase) {
        return suffix;
      }

      @Override
      public SettableEffect<Accumulator, Effect> add(final Effect suffixDelta) {
        return prefix.visit(new SettableEffect.Visitor<>() {
          @Override
          public SettableEffect<Accumulator, Effect> conflict() {
            return prefix;
          }

          @Override
          public SettableEffect<Accumulator, Effect> setTo(Accumulator prefixBase) {
            return SettableEffect.setTo(action.apply(prefixBase, suffixDelta));
          }

          @Override
          public SettableEffect<Accumulator, Effect> add(Effect prefixDelta) {
            return SettableEffect.add(trait.sequentially(prefixDelta, suffixDelta));
          }
        });
      }
    });
  }

  @Override
  public final SettableEffect<Accumulator, Effect> concurrently(
      final SettableEffect<Accumulator, Effect> left,
      final SettableEffect<Accumulator, Effect> right
  ) {
    Objects.requireNonNull(left);
    Objects.requireNonNull(right);

    return left.visit(new SettableEffect.Visitor<>() {
      @Override
      public SettableEffect<Accumulator, Effect> conflict() {
        return left;
      }

      @Override
      public SettableEffect<Accumulator, Effect> setTo(final Accumulator leftBase) {
        return right.visit(new SettableEffect.Visitor<>() {
          @Override
          public SettableEffect<Accumulator, Effect> conflict() {
            return right;
          }

          @Override
          public SettableEffect<Accumulator, Effect> setTo(final Accumulator rightBase) {
            return SettableEffect.conflict();
          }

          @Override
          public SettableEffect<Accumulator, Effect> add(final Effect rightDelta) {
            if (isEffectEmpty.apply(rightDelta)) {
              return left;
            } else {
              return SettableEffect.conflict();
            }
          }
        });
      }

      @Override
      public SettableEffect<Accumulator, Effect> add(final Effect leftDelta) {
        return right.visit(new SettableEffect.Visitor<>() {
          @Override
          public SettableEffect<Accumulator, Effect> conflict() {
            return right;
          }

          @Override
          public SettableEffect<Accumulator, Effect> setTo(final Accumulator rightBase) {
            if (isEffectEmpty.apply(leftDelta)) {
              return right;
            } else {
              return SettableEffect.conflict();
            }
          }

          @Override
          public SettableEffect<Accumulator, Effect> add(final Effect rightDelta) {
            return SettableEffect.add(trait.concurrently(leftDelta, rightDelta));
          }
        });
      }
    });
  }
}
