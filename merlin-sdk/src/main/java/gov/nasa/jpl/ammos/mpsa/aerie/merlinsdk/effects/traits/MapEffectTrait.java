package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An effect algebra over maps from some index space to another effect algebra.
 *
 * <p>
 * For any pair of maps {@code f, g : Map<A, B>}, where <code>B</code> is itself an effect algebra, we can define
 * <code>(f; g).get(x) = f.get(x); g.get(x)</code> and <code>(f | g).get(x) = f.get(x) | g.get(x)</code>. This is useful
 * when the same kind of effect may apply to a number of independent resources. If <code>a : A</code> is a resource,
 * then <code>f.get(a) : B</code> is the effect upon that resource.
 * </p>
 *
 * <p>
 * If a map does not contain a key, then it should be treated as though that key maps to the empty effect.
 * </p>
 *
 * @param <Index> The type of indices to which to associate effects.
 * @param <Effect> The type of effect to be associated with each index.
 * @see EffectTrait
 * @see Map
 * @see IndexedEffectTrait
 */
public class MapEffectTrait<Index, Effect> implements EffectTrait<Map<Index, Effect>> {
  /**
   * The effect algebra provided at construction time.
   *
   * <p>
   * This is made available to subclasses to avoid a needless extra field in subclasses that need to use the trait.
   * Subclasses <b>must</b> guarantee not to perform operations on this field that observably affect its behavior.
   * </p>
   */
  protected final EffectTrait<Effect> valueTrait;

  /**
   * Construct an effect algebra on maps from an index space to some other effect algebra.
   *
   * <p>
   * Notice that the input type takes no part in effect composition, as it only serves to ensure that effects under one
   * are held independent from effects under another.
   * </p>
   *
   * @param valueTrait An effect algebra on the value type of the map.
   */
  public MapEffectTrait(final EffectTrait<Effect> valueTrait) {
    this.valueTrait = valueTrait;
  }

  @Override
  public final Map<Index, Effect> empty() {
    return Collections.emptyMap();
  }

  @Override
  public final Map<Index, Effect> sequentially(final Map<Index, Effect> prefix, final Map<Index, Effect> suffix) {
    final var sequenced = new HashMap<>(prefix);
    for (final var entry : suffix.entrySet()) {
      final var key = entry.getKey();
      final var suffixEffect = entry.getValue();

      final var prefixEffect = Optional
          .ofNullable(sequenced.getOrDefault(key, null))
          .orElseGet(this.valueTrait::empty);

      sequenced.put(key, this.valueTrait.sequentially(prefixEffect, suffixEffect));
    }
    return sequenced;
  }

  @Override
  public final Map<Index, Effect> concurrently(final Map<Index, Effect> left, final Map<Index, Effect> right) {
    final var concurrenced = new HashMap<>(left);
    for (final var entry : right.entrySet()) {
      final var key = entry.getKey();
      final var rightEffect = entry.getValue();

      final var leftEffect = Optional
          .ofNullable(concurrenced.getOrDefault(key, null))
          .orElseGet(this.valueTrait::empty);

      concurrenced.put(key, this.valueTrait.concurrently(leftEffect, rightEffect));
    }
    return concurrenced;
  }
}
