package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class Windows implements Iterable<Window> {
  private final List<Window> windows = new ArrayList<>();

  public Windows() {}

  public Windows(final Windows other) {
    this.windows.addAll(other.windows);
  }

  public Windows(final List<Window> windows) {
    for (final var window : windows) this.add(window);
  }

  public Windows(final Window... windows) {
    for (final var window : windows) this.add(window);
  }

  public void add(final Window window) {
    if (window.isEmpty()) return;

    // OPTIMIZATION: If this window fits at the end of our list, just do that.
    // Common case for building up a set of windows.
    // This whole clause can be removed without affecting correctness.
    if (this.windows.size() == 0 || this.windows.get(this.windows.size() - 1).end.shorterThan(window.start)) {
      this.windows.add(window);
      return;
    } else if (this.windows.get(this.windows.size() - 1).start.shorterThan(window.start)) {
      this.windows.set(this.windows.size() - 1, Window.leastUpperBound(window, this.windows.get(this.windows.size() - 1)));
      return;
    }

    this.addAll(List.of(window));
  }

  public void add(final Duration start, final Duration end) {
    this.add(Window.between(start, end));
  }

  public void add(final long startQuantity, final TimeUnit startUnits, final long endQuantity, final TimeUnit endUnits) {
    this.add(Window.between(startQuantity, startUnits, endQuantity, endUnits));
  }

  public void addPoint(final long quantity, final TimeUnit units) {
    this.add(Window.at(quantity, units));
  }

  public void addAll(final Windows other) {
    this.addAll(other.windows);
  }

  public static Windows union(final Windows left, final Windows right) {
    final var result = new Windows(left);
    result.addAll(right);
    return result;
  }

  // PRECONDITION: `other` is a list of non-overlapping windows ordered by start time.
  private void addAll(final List<Window> other) {
    int index = 0;

    for (final var window : other) {
      // Skip windows that end before this one starts.
      while (index < this.windows.size() && this.windows.get(index).end.shorterThan(window.start)) {
        index += 1;
      }

      // Remove and join with any windows that overlap this window.
      var joined = window;
      while (index < this.windows.size() && !this.windows.get(index).start.longerThan(window.end)) {
        joined = Window.leastUpperBound(joined, this.windows.remove(index));
      }

      this.windows.add(index, joined);
    }
  }

  public void subtract(final Window window) {
    if (window.isEmpty()) return;
    this.subtractAll(List.of(window));
  }

  public void subtract(final Duration start, final Duration end) {
    this.subtract(Window.between(start, end));
  }

  public void subtract(final long startQuantity, final TimeUnit startUnits, final long endQuantity, final TimeUnit endUnits) {
    this.subtract(Window.between(startQuantity, startUnits, endQuantity, endUnits));
  }

  public void subtractPoint(final long quantity, final TimeUnit units) {
    this.subtract(Window.at(quantity, units));
  }

  public void subtractAll(final Windows other) {
    this.subtractAll(other.windows);
  }

  public static Windows minus(final Windows left, final Windows right) {
    final var result = new Windows(left);
    result.subtractAll(right);
    return result;
  }

  // PRECONDITION: `other` is a list of non-overlapping windows ordered by start time.
  private void subtractAll(final List<Window> other) {
    int index = 0;

    for (final var window : other) {
      // Look for the first window ending at or after this one starts.
      while (index < this.windows.size() && this.windows.get(index).end.shorterThan(window.start)) {
        index += 1;
      }

      // Clip the window at the start of this range.
      if (index < this.windows.size() && this.windows.get(index).start.shorterThan(window.start)) {
        this.windows.add(index, Window.between(this.windows.get(index).start, window.start.minus(Duration.EPSILON)));
        index += 1;
        this.windows.set(index, Window.between(window.start, this.windows.get(index).end));
      }

      // Remove any windows contained by this window.
      while (index < this.windows.size() && !this.windows.get(index).end.longerThan(window.end)) {
        this.windows.remove(index);
      }

      // Clip the window at the end of this range.
      if (index < this.windows.size() && !this.windows.get(index).start.longerThan(window.end)) {
        this.windows.set(index, Window.between(window.end.plus(Duration.EPSILON), this.windows.get(index).end));
      }
    }
  }

  public void intersectWith(final Window window) {
    if (window.isEmpty()) {
      this.windows.clear();
    } else {
      this.intersectWith(List.of(window));
    }
  }

  public void intersectWith(final Duration start, final Duration end) {
    this.intersectWith(Window.between(start, end));
  }

  public void intersectWith(final long startQuantity, final TimeUnit startUnits, final long endQuantity, final TimeUnit endUnits) {
    this.intersectWith(Window.between(startQuantity, startUnits, endQuantity, endUnits));
  }

  public void intersectWith(final Windows other) {
    this.intersectWith(other.windows);
  }

  public static Windows intersection(final Windows left, final Windows right) {
    final var result = new Windows(left);
    result.intersectWith(right);
    return result;
  }

  // PRECONDITION: `other` is a list of non-overlapping windows ordered by start time.
  private void intersectWith(final List<Window> other) {
    int index = 0;

    for (final var window : other) {
      // Remove any initial windows that don't intersect this one
      while (index < this.windows.size() && this.windows.get(index).end.shorterThan(window.start)) {
        this.windows.remove(index);
      }

      // Clip the first window intersecting this one.
      if (index < this.windows.size() && this.windows.get(index).start.shorterThan(window.start)) {
        this.windows.set(index, Window.between(window.start, this.windows.get(index).end));
      }

      // Keep any windows contained within this one.
      while (index < this.windows.size() && !this.windows.get(index).end.longerThan(window.end)) {
        index += 1;
      }

      // Clip the window at the end of this range.
      if (index < this.windows.size() && !this.windows.get(index).start.longerThan(window.end)) {
        this.windows.set(index, Window.between(this.windows.get(index).start, window.end));
        index += 1;
      }
    }

    // Remove any remaining windows, since they're after everything in `other`.
    while (index < this.windows.size()) {
      this.windows.remove(index);
    }
  }

  // TODO: implement symmetric difference `negateUnder()`

  public boolean isEmpty() {
    return new Windows().includes(this);
  }

  public boolean includes(final long startQuantity, final TimeUnit startUnits, final long endQuantity, final TimeUnit endUnits) {
    return this.includes(Window.between(startQuantity, startUnits, endQuantity, endUnits));
  }

  public boolean includesPoint(final long quantity, final TimeUnit units) {
    return this.includes(Window.at(quantity, units));
  }

  public boolean includes(final Window probe) {
    if (probe.isEmpty()) return true;
    return this.includes(List.of(probe));
  }

  public boolean includes(final Windows other) {
    return this.includes(other.windows);
  }

  // PRECONDITION: `other` is a list of non-overlapping windows ordered by start time.
  private boolean includes(final List<Window> other) {
    int index = 0;

    for (final var window : other) {
      // Skip any windows that fully precede this one.
      while (index < this.windows.size() && this.windows.get(index).end.shorterThan(window.start)) {
        index += 1;
      }

      // If windows.get(index) doesn't contain `window`, then nothing does.
      if (index >= this.windows.size() || !this.windows.get(index).contains(window)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Iterator<Window> iterator() {
    // SAFETY: calling `.remove()` on the returned iterator does not breach encapsulation.
    // The same effect can be achieved by calling `windows.subtract()` against the data returned by the iterator,
    // except for the added burden of avoiding `ConcurrentModificationException`s.
    return this.windows.iterator();
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof Windows)) return false;
    final var other = (Windows) obj;

    return Objects.equals(this.windows, other.windows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.windows);
  }

  @Override
  public String toString() {
    return this.windows.toString();
  }
}
