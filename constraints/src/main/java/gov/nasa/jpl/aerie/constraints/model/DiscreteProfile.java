package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

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

  private static boolean profileOutsideBounds(final DiscreteProfilePiece piece, final Window bounds){
    return piece.window.isStrictlyBefore(bounds) || piece.window.isStrictlyAfter(bounds);
  }

  @Override
  public Windows notEqualTo(final DiscreteProfile other, final Window bounds) {
    final var windows = new Windows(bounds);
    for (final var profilePiece : this.profilePieces) {
      if(profileOutsideBounds(profilePiece, bounds)) continue;
      for (final var otherPiece : other.profilePieces) {
        if(profileOutsideBounds(otherPiece, bounds)) continue;
        if (profilePiece.value.equals(otherPiece.value)) {
          windows.subtractAll(
              Windows.intersection(
                  new Windows(profilePiece.window),
                  new Windows(otherPiece.window)
              )
          );
        }
      }
    }

    return windows;
  }

  @Override
  public Windows equalTo(final DiscreteProfile other, final Window bounds) {
    final var windows = new Windows();
    for (final var profilePiece : this.profilePieces) {
      if(profileOutsideBounds(profilePiece, bounds)) continue;
      for (final var otherPiece : other.profilePieces) {
        if(profileOutsideBounds(otherPiece, bounds)) continue;
        if (profilePiece.value.equals(otherPiece.value)) {
          windows.addAll(
              Windows.intersection(
                  new Windows(profilePiece.window),
                  new Windows(otherPiece.window)
              )
          );
        }
      }
    }
    return Windows.intersection(windows, new Windows(bounds));
  }

  // TODO: Gaps in profiles will cause an error
  //       We may want to deal with gaps someday
  @Override
  public Windows changePoints(final Window bounds) {
    final var changePoints = new Windows();
    if (this.profilePieces.size() == 0) return changePoints;

    final var iter = this.profilePieces.iterator();
    var prev = iter.next();

    while (iter.hasNext()) {
      final var curr = iter.next();
      //avoid adding transition points for profiles outside of bounds
      if(profileOutsideBounds(prev, bounds) || profileOutsideBounds(curr, bounds)) {prev = curr; continue;}

      if (Window.meets(prev.window, curr.window)) {
        if (prev.value != curr.value) changePoints.add(Window.at(prev.window.end));
      } else {
        throw new Error("Unexpected gap in profile pieces not allowed");
      }

      prev = curr;
    }

    changePoints.intersectWith(bounds);
    return changePoints;
  }

  // TODO: Gaps in profiles will cause an error
  //       We may want to deal with gaps someday
  public Windows transitions(final SerializedValue oldState, final SerializedValue newState, final Window bounds) {
    final var transitionPoints = new Windows();
    if (this.profilePieces.size() == 0) return transitionPoints;

    final var iter = this.profilePieces.iterator();
    var prev = iter.next();

    while (iter.hasNext()) {
      final var curr = iter.next();
      //avoid adding transition points for profiles outside of bounds
      if(profileOutsideBounds(prev, bounds) || profileOutsideBounds(curr, bounds)) {prev = curr; continue;}

      if (Window.meets(prev.window, curr.window)) {
        if (prev.value.equals(oldState) && curr.value.equals(newState)) {
          transitionPoints.add(Window.at(prev.window.end));
        }
      } else {
        throw new Error("Unexpected gap in profile pieces not allowed");
      }

      prev = curr;
    }

    transitionPoints.intersectWith(bounds);
    return transitionPoints;
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof DiscreteProfile)) return false;
    final var other = (DiscreteProfile)obj;
    return Objects.equals(this.profilePieces, other.profilePieces);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.profilePieces);
  }
}
