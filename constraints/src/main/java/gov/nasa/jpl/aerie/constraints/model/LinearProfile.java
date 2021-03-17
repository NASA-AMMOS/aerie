package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class LinearProfile implements Profile<LinearProfile> {
  // IMPORTANT: Profile pieces must be non-overlapping, and increasing (based on window field)
  public final List<LinearProfilePiece> profilePieces;

  public LinearProfile(final List<LinearProfilePiece> profilePieces) {
    this.profilePieces = Objects.requireNonNull(profilePieces);
  }

  public LinearProfile(final LinearProfilePiece... profilePieces) {
    this(List.of(profilePieces));
  }

  @Override
  public Windows equalTo(final LinearProfile other, final Window bounds) {
    return this.getWindowsSatisfying(other, bounds, LinearProfilePiece::equalTo);
  }

  @Override
  public Windows notEqualTo(final LinearProfile other, final Window bounds) {
    return this.getWindowsSatisfying(other, bounds, LinearProfilePiece::notEqualTo);
  }

  public Windows lessThan(final LinearProfile other, final Window bounds) {
    return this.getWindowsSatisfying(other, bounds, LinearProfilePiece::lessThan);
  }

  public Windows lessThanOrEqualTo(final LinearProfile other, final Window bounds) {
    return this.getWindowsSatisfying(other, bounds, LinearProfilePiece::lessThanOrEqualTo);
  }

  public Windows greaterThan(final LinearProfile other, final Window bounds) {
    return this.getWindowsSatisfying(other, bounds, LinearProfilePiece::greaterThan);
  }

  public Windows greaterThanOrEqualTo(final LinearProfile other, final Window bounds) {
    return this.getWindowsSatisfying(other, bounds, LinearProfilePiece::greaterThanOrEqualTo);
  }

  private Windows getWindowsSatisfying(final LinearProfile other, final Window bounds, final BiFunction<LinearProfilePiece, LinearProfilePiece, Windows> condition) {
    final var windows = new Windows();
    for (final var satisfying : processIntersections(this, other, condition)) {
      windows.addAll(satisfying);
    }

    return Windows.intersection(
        windows,
        new Windows(bounds)
    );
  }

  /**
   * Process each pair of intersecting profile pieces to build a list of results
   *
   * ASSUMPTION: Both sets of profile pieces are ordered in increasing order
   * @param right LinearProfile to intersect with
   * @param processor BiFunction taking two profile pieces and a desired result based on their intersection
   * @return Set of all windows within bounds for which condition is satisfied between this and another profile
   */
  private static <T> List<T> processIntersections(final LinearProfile left, final LinearProfile right, final BiFunction<LinearProfilePiece, LinearProfilePiece, T> processor) {
    if (left.profilePieces.size() == 0 || right.profilePieces.size() == 0) return new ArrayList<>();

    // Setup to step through profiles simultaneously,
    // finding all windows satisfying condition within the supplied bounds
    final var processedIntersections = new ArrayList<T>();
    final var rightIter = right.profilePieces.iterator();

    // Loop through the left pieces, stepping up the right pieces
    // appropriately each time to get intersections
    if (!rightIter.hasNext()) return processedIntersections;
    var rightPiece = rightIter.next();
    for (final var leftPiece : left.profilePieces) {
      // If left piece ends before right piece, skip left piece
      if (Window.compareEndToStart(leftPiece.window, rightPiece.window) < 0) continue;

      // Step through right pieces ending before left piece starts
      // If no right pieces ending after left piece starts exist, end the loop
      while (Window.compareEndToStart(rightPiece.window, leftPiece.window) < 0) {
        if (rightIter.hasNext()) {
          rightPiece = rightIter.next();
        } else {
          break;
        }
      }

      // Process all intersections with right pieces that start before the left piece ends
      // If we run out of right pieces, end the loop
      while (Window.compareStartToEnd(rightPiece.window, leftPiece.window) <= 0) {
        processedIntersections.add(processor.apply(leftPiece, rightPiece));

        // Only step passed right piece if it doesn't exceed the left piece
        if (Window.compareEndToEnd(rightPiece.window, leftPiece.window) <= 0) {
          if (rightIter.hasNext()) {
            rightPiece = rightIter.next();
            continue;
          }
        }
        break;
      }
    }

    return processedIntersections;
  }

  public String toString() {
    return this.profilePieces.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof LinearProfile)) return false;
    final var other = (LinearProfile)obj;

    return Objects.equals(this.profilePieces, other.profilePieces);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.profilePieces);
  }
}
