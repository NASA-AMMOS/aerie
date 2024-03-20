package gov.nasa.jpl.aerie.constraints.time;

import java.util.Comparator;

/** A basic container used by {@link IntervalMap} that associates an interval with a value */
public record Segment<V>(Interval interval, V value) implements Comparable<Segment<V>> {
  public static <V> Segment<V> of(final Interval interval, final V value) {
    return new Segment<>(interval, value);
  }

  //private Comparator<Segment<V>> comparator = Comparator.comparing(Segment<V>::interval).thenComparing(Segment::value, ObjectComparator.getInstance());
  @Override
  public int compareTo(final Segment<V> o) {
    final var comparator =
        Comparator.comparing(Segment<V>::interval).thenComparing(Segment::value, ObjectComparator.getInstance());
    return comparator.compare(this, o);
  }

  public static class ObjectComparator implements Comparator<Object> {
    private static ObjectComparator INSTANCE = null;
    public static ObjectComparator getInstance() {
      if (INSTANCE == null) {
        INSTANCE = new ObjectComparator();
      }
      return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compare(Object o1, Object o2) {
      if (o1 == o2) return 0;
      if (o1 == null) return -1;
      if (o2 == null) return 1;

      // Compare using Comparable if applicable.
      // o1's comparator may assume the type of o2, causing a ClassCastException.
      // This could result in poor performance if the exception is thrown regularly.
      try {
        if (o1 instanceof Comparable && o2 instanceof Comparable) {
          return ((Comparable)o1).compareTo(o2);
        }
      } catch (ClassCastException t) {}

      // Fallback comparison
      int classComparison = o1.getClass().getName().compareTo(o2.getClass().getName());
      if (classComparison != 0) {
        return classComparison;
      }

      // When class names are the same, compare hashCodes to enforce a deterministic order
      return Integer.compare(System.identityHashCode(o1), System.identityHashCode(o2));
    }
  }
}
