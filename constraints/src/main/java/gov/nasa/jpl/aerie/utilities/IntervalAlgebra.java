package gov.nasa.jpl.aerie.utilities;

/**
 * An algebra for interval-like operations on some underlying type {@code I}.
 *
 * @param <Alg> A type witnessing the uniqueness of any given implementation.
 *     Another implementation should not be able to use this witness,
 *     and any implementations with the same witness must be equivalent.
 *     This allows containers that sort based on this algebra to constrain operations with other such collections,
 *     e.g. ensuring that `addAll()` is only allowed (or is differently optimized) when the same algebra is used.
 *
 *     If the implementation is unique for a type, and is private, the implementing class may witness itself.
 *     If the implementation is public, use a `private enum Witness {}` nested into the implementing class.
 *     If the implementation may vary in finitely many ways, use as many witnesses as there are ways.
 *     If the implementation may vary more broadly, may God have mercy on your soul --
 *     either use an existential, or use something less fine-grained and be very very careful.
 * @param <I> The type on which this algebra supports interval operations.
 */
public interface IntervalAlgebra<Alg, I> {
  boolean isEmpty(I x);

  I unify(I x, I y);
  I intersect(I x, I y);

  I lowerBoundsOf(I x);
  I upperBoundsOf(I x);


  default boolean overlaps(I x, I y) {
    return !isEmpty(intersect(x, y));
  }
  default boolean contains(I outer, I inner) {
    // If `inner` doesn't overlap with the complement of `outer`,
    // then `inner` must exist entirely within `outer`.
    return !(overlaps(inner, upperBoundsOf(outer)) || overlaps(inner, lowerBoundsOf(outer)));
  }
  default boolean strictlyContains(I outer, I inner) {
    return contains(outer, inner) && !contains(inner, outer);
  }
  default boolean equals(I x, I y) {
    return contains(x, y) && contains(y, x);
  }

  default boolean startsBefore(I x, I y) {
    return strictlyContains(lowerBoundsOf(y), lowerBoundsOf(x));
  }
  default boolean endsAfter(I x, I y) {
    return strictlyContains(upperBoundsOf(y), upperBoundsOf(x));
  }

  default boolean startsAfter(I x, I y) {
    return endsBefore(y, x);
  }
  default boolean endsBefore(I x, I y) {
    return endsStrictlyBefore(x, y) || meets(x, y);
  }

  default boolean endsStrictlyBefore(I x, I y) {
    return !isEmpty(intersect(upperBoundsOf(x), lowerBoundsOf(y)));
  }
  default boolean startsStrictlyAfter(I x, I y) {
    return endsStrictlyBefore(y, x);
  }

  default boolean meets(I x, I y) {
    return equals(upperBoundsOf(x), upperBoundsOf(lowerBoundsOf(y)));
  }
  default boolean isMetBy(I x, I y) {
    return meets(y, x);
  }
}
