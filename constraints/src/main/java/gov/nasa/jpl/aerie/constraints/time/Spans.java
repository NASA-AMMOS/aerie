package gov.nasa.jpl.aerie.constraints.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A collection of intervals that can overlap.
 */
public class Spans implements Iterable<Window> {
  private final List<Window> windows;

  public Spans() {
    this.windows = new ArrayList<>();
  }

  public Spans(final ArrayList<Window> windows) {
    windows.removeIf(Window::isEmpty);
    this.windows = windows;
  }

  public Spans(final Iterable<Window> iter) {
    this.windows = StreamSupport.stream(iter.spliterator(), false).filter($ -> !$.isEmpty()).toList();
  }

  public Spans(final Window... windows) {
    this.windows = Arrays.stream(windows).filter($ -> !$.isEmpty()).toList();
  }

  public Windows intoWindows() {
    return new Windows(this.windows);
  }

  public void add(final Window window) {
    if (!window.isEmpty()) {
      this.windows.add(window);
    }
  }

  public void addAll(final Iterable<Window> iter) {
    this.windows.addAll(
        StreamSupport
            .stream(iter.spliterator(), false)
            .filter($ -> !$.isEmpty())
            .toList()
    );
  }

  public Spans map(final Function<Window, Window> mapper) {
    return new Spans(this.windows.stream().map(mapper).filter($ -> !$.isEmpty()).toList());
  }

  public Spans flatMap(final Function<Window, ? extends Stream<Window>> mapper) {
    return new Spans(this.windows.stream().flatMap(mapper).filter($ -> !$.isEmpty()).toList());
  }

  public Spans filter(final Predicate<Window> filter) {
    return new Spans(this.windows.stream().filter(filter).toList());
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof final Spans spans)) return false;

    return Objects.equals(this.windows, spans.windows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.windows);
  }

  @Override
  public String toString() {
    return this.windows.toString();
  }

  @Override
  public Iterator<Window> iterator() {
    return this.windows.iterator();
  }
}
