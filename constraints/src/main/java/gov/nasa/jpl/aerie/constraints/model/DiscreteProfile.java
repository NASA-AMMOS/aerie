package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;

public final class DiscreteProfile implements Profile<DiscreteProfile> {
  public final List<DiscreteProfilePiece> profilePieces;

  public DiscreteProfile(final List<DiscreteProfilePiece> profilePieces) {
    this.profilePieces = profilePieces;
  }

  public DiscreteProfile(final DiscreteProfilePiece... profilePieces) {
    this(List.of(profilePieces));
  }

  private static boolean profileOutsideBounds(final DiscreteProfilePiece piece, final Interval bounds){
    return piece.interval.isStrictlyBefore(bounds) || piece.interval.isStrictlyAfter(bounds);
  }

  @Override
  public Windows equalTo(final DiscreteProfile other, final Interval bounds) {
    final var windows = new Windows();
    for (final var profilePiece : this.profilePieces) {
      if(profileOutsideBounds(profilePiece, bounds)) continue;
      for (final var otherPiece : other.profilePieces) {
        if(profileOutsideBounds(otherPiece, bounds)) continue;
        final var overlap = Interval.intersect(profilePiece.interval, otherPiece.interval);
        if (!overlap.isEmpty()) {
          windows.set(overlap, profilePiece.value.equals(otherPiece.value));
        }
      }
    }
    windows.bound(bounds);
    return windows;
  }

  @Override
  public Windows notEqualTo(final DiscreteProfile other, final Interval bounds) {
    return this.equalTo(other, bounds).not();
  }

  @Override
  public Windows changePoints(final Interval bounds) {
    final var changePoints = new Windows();
    if (this.profilePieces.size() == 0) return changePoints;

    final var iter = this.profilePieces.iterator();
    var prev = iter.next();
    if (prev.interval.start.noLongerThan(bounds.start)) {
      changePoints.set(prev.interval, false);
    } else {
      changePoints.set(
          Interval.between(
              prev.interval.start,
              Interval.Inclusivity.Exclusive,
              prev.interval.end,
              prev.interval.endInclusivity
          ),
          false
      );
    }

    while (iter.hasNext()) {
      final var curr = iter.next();

      if (Interval.meets(prev.interval, curr.interval)) {
        changePoints.set(curr.interval, false);
        if (!prev.value.equals(curr.value)) changePoints.setTrue(Interval.at(curr.interval.start));
      } else {
        changePoints.set(
            Interval.between(
                curr.interval.start,
                Interval.Inclusivity.Exclusive,
                curr.interval.end,
                curr.interval.endInclusivity),
            false
        );
      }

      prev = curr;
    }

    changePoints.bound(bounds);
    return changePoints;
  }

  public Windows transitions(final SerializedValue oldState, final SerializedValue newState, final Interval bounds) {
    final var transitionPoints = new Windows();
    if (this.profilePieces.size() == 0) return transitionPoints;

    final var iter = this.profilePieces.iterator();
    var prev = iter.next();
    if (prev.interval.start.noLongerThan(bounds.start)) {
      transitionPoints.set(prev.interval, false);
    } else {
      transitionPoints.set(
          Interval.between(
              prev.interval.start,
              Interval.Inclusivity.Exclusive,
              prev.interval.end,
              prev.interval.endInclusivity
          ),
          false
      );
    }

    while (iter.hasNext()) {
      final var curr = iter.next();

      if (Interval.meets(prev.interval, curr.interval)) {
        transitionPoints.set(curr.interval, false);
        if (prev.value.equals(oldState) && curr.value.equals(newState)) transitionPoints.setTrue(Interval.at(curr.interval.start));
      } else {
        transitionPoints.set(
            Interval.between(
                curr.interval.start,
                Interval.Inclusivity.Exclusive,
                curr.interval.end,
                curr.interval.endInclusivity),
            false
        );
      }

      prev = curr;
    }

    transitionPoints.bound(bounds);
    return transitionPoints;
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof final DiscreteProfile other)) return false;
    return Objects.equals(this.profilePieces, other.profilePieces);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.profilePieces);
  }
}
