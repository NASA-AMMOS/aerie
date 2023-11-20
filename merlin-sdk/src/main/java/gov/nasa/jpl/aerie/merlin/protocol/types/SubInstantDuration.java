package gov.nasa.jpl.aerie.merlin.protocol.types;

/**
 * A {@link Duration} (interpreted as a time offset) paired with an index into a sequence of events occurring within
 * that atomic time value.
 * @param duration
 * @param index
 */
public record SubInstantDuration(Duration duration, Integer index) implements Comparable<SubInstantDuration> {

  public static SubInstantDuration ZERO = new SubInstantDuration(Duration.ZERO, 0);
  public static SubInstantDuration MAX_VALUE = new SubInstantDuration(Duration.MAX_VALUE, Integer.MAX_VALUE);
  public static SubInstantDuration MIN_VALUE = new SubInstantDuration(Duration.MIN_VALUE, 0);
  public static SubInstantDuration EPSILON = new SubInstantDuration(Duration.EPSILON, 0);
  public static SubInstantDuration EPSILONI = new SubInstantDuration(Duration.ZERO, 1);

  /**
   * Compares this object with the specified object for order.  Returns a
   * negative integer, zero, or a positive integer as this object is less
   * than, equal to, or greater than the specified object.
   *
   * <p>The implementor must ensure {@link Integer#signum
   * signum}{@code (x.compareTo(y)) == -signum(y.compareTo(x))} for
   * all {@code x} and {@code y}.  (This implies that {@code
   * x.compareTo(y)} must throw an exception if and only if {@code
   * y.compareTo(x)} throws an exception.)
   *
   * <p>The implementor must also ensure that the relation is transitive:
   * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
   * {@code x.compareTo(z) > 0}.
   *
   * <p>Finally, the implementor must ensure that {@code
   * x.compareTo(y)==0} implies that {@code signum(x.compareTo(z))
   * == signum(y.compareTo(z))}, for all {@code z}.
   *
   * @param o the object to be compared.
   * @return a negative integer, zero, or a positive integer as this object
   * is less than, equal to, or greater than the specified object.
   * @throws NullPointerException if the specified object is null
   * @throws ClassCastException   if the specified object's type prevents it
   *                              from being compared to this object.
   * @apiNote It is strongly recommended, but <i>not</i> strictly required that
   * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
   * class that implements the {@code Comparable} interface and violates
   * this condition should clearly indicate this fact.  The recommended
   * language is "Note: this class has a natural ordering that is
   * inconsistent with equals."
   */
  @Override
  public int compareTo(final SubInstantDuration o) {
    int r = this.duration.compareTo(o.duration);
    if (r != 0) return r;
    r = Integer.compare(this.index, o.index);
    return r;
  }

  public int compareTo(final Duration o) {
    return this.duration.compareTo(o);
  }

  public boolean isEqualTo(SubInstantDuration o) {
    return this.duration.isEqualTo(o.duration) && this.index == o.index;
  }

  public boolean isEqualTo(Duration o) {
    return this.duration.isEqualTo(o);
  }

  public boolean longerThan(final SubInstantDuration o) {
    return this.compareTo(o) > 0;
  }

  public boolean longerThan(final Duration o) {
    return this.compareTo(o) > 0;
  }

  public boolean noLongerThan(final SubInstantDuration o) {
    return this.compareTo(o) <= 0;
  }

  public boolean noLongerThan(final Duration o) {
    return this.compareTo(o) <= 0;
  }

  public boolean shorterThan(final SubInstantDuration o) {
    return this.compareTo(o) < 0;
  }

  public boolean shorterThan(final Duration o) {
    return this.compareTo(o) < 0;
  }

  public boolean noShorterThan(final SubInstantDuration o) {
    return this.compareTo(o) >= 0;
  }

  public boolean noShorterThan(final Duration o) {
    return this.compareTo(o) >= 0;
  }


  public static SubInstantDuration min(SubInstantDuration d1, SubInstantDuration d2) {
    return d1.longerThan(d2) ? d2 : d1;
  }

  public static SubInstantDuration max(SubInstantDuration d1, SubInstantDuration d2) {
    return d1.shorterThan(d2) ? d2 : d1;
  }
}
