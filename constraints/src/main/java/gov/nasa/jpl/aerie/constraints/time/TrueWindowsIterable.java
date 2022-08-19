package gov.nasa.jpl.aerie.constraints.time;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TrueWindowsIterable implements Iterable<Interval> {
  private final Windows windows;

  public TrueWindowsIterable(final Windows windows) {
    this.windows = windows;
  }

  @Override
  public Iterator<Interval> iterator() {
    return StreamSupport.stream(this.windows.spliterator(), false).flatMap(pair -> {
      if (pair.getValue()) {
        return Stream.of(pair.getKey());
      } else {
        return Stream.of();
      }
    }).iterator();
  }
}
