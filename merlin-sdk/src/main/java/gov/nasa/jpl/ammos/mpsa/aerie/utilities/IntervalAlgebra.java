package gov.nasa.jpl.ammos.mpsa.aerie.utilities;

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

  IntervalAlgebra.Relation relationBetween(I x, I y);

  default boolean overlaps(I x, I y) {
    switch (relationBetween(x, y)) {
      case Before: case After:
        return true;
      default:
        return false;
    }
  }

  default boolean contains(I outer, I inner) {
    switch (relationBetween(outer, inner)) {
      case Contains: case Equals:
        return true;
      default:
        return false;
    }
  }

  default boolean startsBefore(I x, I y) {
    switch (relationBetween(x, y)) {
      case Before: case LeftOverhang: case Contains:
        return true;
      default:
        return false;
    }
  }

  default boolean endsBefore(I x, I y) {
    return (relationBetween(x, y) == IntervalAlgebra.Relation.Before);
  }

  default boolean endsAfter(I x, I y) {
    switch (relationBetween(x, y)) {
      case After: case RightOverhang: case Contains:
        return true;
      default:
        return false;
    }
  }

  default boolean startsAfter(I x, I y) {
    return (relationBetween(x, y) == IntervalAlgebra.Relation.After);
  }

  enum Relation {
    Before,
    LeftOverhang,
    Contains,
    Equals,
    ContainedBy,
    RightOverhang,
    After,
  }
}
