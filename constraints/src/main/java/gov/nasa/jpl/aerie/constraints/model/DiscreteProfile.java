package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;

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

  @Override
  public Windows notEqualTo(final DiscreteProfile other, final Window bounds) {
    final var windows = new Windows(bounds);
    for (final var profilePiece : this.profilePieces) {
      for (final var otherPiece : other.profilePieces) {
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
      for (final var otherPiece : other.profilePieces) {
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
