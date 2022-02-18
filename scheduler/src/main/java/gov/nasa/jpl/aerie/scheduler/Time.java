package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Objects;

/**
 * represents an particular single instant on a given timeline
 */
public class Time implements Comparable<Time> {

  /**
   * returns reference to time point at exactly the reference epoch
   *
   * @return a time representing the reference epoch
   */
  public static Time ofZero() {
    return zeroConstant;
  }

  /**
   * Hardcoded max time (created to handle state cache)
   *
   * @return the max time
   */
  public static Time ofMax() {
    return fromDOM("2500-01-01T00:00:00.000");
  }

  /**
   * returns the minimum between two times
   *
   * @param t1 a time
   * @param t2 a time
   * @return the minimum betwwen two times
   */
  public static Time min(Time t1, Time t2) {
    return t1.compareTo(t2) >= 0 ? t2 : t1;
  }

  /**
   * returns the maximum between two times
   *
   * @param t1 a time
   * @param t2 a time
   * @return the maximum betwwen two times
   */
  public static Time max(Time t1, Time t2) {
    return t1.compareTo(t2) <= 0 ? t2 : t1;
  }

  @Override
  public int hashCode() {
    return Objects.hash(jplET_s);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Time time = (Time) o;
    return Double.compare(time.jplET_s, jplET_s) == 0;
  }

  /**
   * creates a new time advanced from this time by the input duration
   *
   * note that the input may be negative, resulting in a subtraction
   *
   * @param duration IN the duration forward from this time to return a
   *     timepoint for
   * @return a new time object that is advanced from this time by the
   *     provided duration object
   */
  public Time plus(Duration duration) {
    return new Time(Math.min(this.jplET_s + duration.in(Duration.SECONDS), Double.MAX_VALUE));
  }


  /**
   * creates a new time prior to this time by the input duration
   *
   * note that the input may be negative, resulting in an addition
   *
   * @param duration IN the duration backwards from this time to return a
   *     timepoint for
   * @return a new time object that is prior to this time by the provided
   *     duration object
   */
  public Time minus(Duration duration) {
    return new Time(Math.max(this.jplET_s - duration.in(Duration.SECONDS), 0.0));
  }


  /**
   * creates a new duration that is the difference of this minus provided time
   *
   * note that the input may be prior to this time, resulting in a negative
   * duration
   *
   * @param o IN the time to calculate the duration difference to
   * @return a new duration object representing the duration elapsed from
   *     this time up to the provided argument time, possibly negative
   */
  public Duration minus(Time o) {
    return Duration.of((long) (this.jplET_s - o.jplET_s), Duration.SECONDS);
  }


  /**
   * {@inheritDoc}
   *
   * naturally orders times based on distance from the reference epoch (which
   * may be negative)
   *
   * @return a negative, zero, or positive number in the event this time
   *     is, respectively, less, equal, or greater than the provided
   *     time argument
   */
  @Override
  public int compareTo(Time o) {
    return Double.compare(this.jplET_s, o.jplET_s);
  }

  public boolean biggerThan(Time otherTime) {
    return compareTo(otherTime) >= 0;
  }

  public boolean smallerThan(Time otherTime) {
    return compareTo(otherTime) <= 0;
  }

  /**
   * returns the millisecond timespan since the reference epoch of this time point
   *
   * the result may be negative, indicating the time point preceeds the reference
   * epoch time point
   *
   * @return the number of milliseconds of ephemeris time elapsed since the
   *     reference epoch up to this time point
   */
  public long toEpochMilliseconds() {
    return (long) (jplET_s * 1000.0);
  }

  public long toSeconds() {
    return (long) jplET_s;
  }

  /**
   * {inheritDoc}
   *
   * serializes this time into a format amenable to use by jpl seq
   * toolchain tools (apgen, raven, etc)
   *
   * output is in UTC time zone
   */
  @Override
  public String toString() {
    return formatDOY.format(toInstant());
  }

  /**
   * converts this time into a java.time.Instant
   *
   * @return the java instant matching this time
   */
  public java.time.Instant toInstant() {
    return java.time.Instant.ofEpochMilli((long) (jplET_s * 1000.0));
  }

  /**
   * parses the provided input time string as a jpl seq time specification
   *
   * recognizes times of the format 2020-135T14:33:12.442
   *
   * assumes time zone UTC
   *
   * @param s IN the string to parse as a seq timestamp
   * @return a new time object representing the time specified by the string
   */
  public static Time fromString(String s) {
    return new Time(java.time.Instant.from(formatDOY.parse(s))
                                     .toEpochMilli() / 1000.0);
  }


  public static Duration fromString(String s, PlanningHorizon ph){
    return ph.toDur(fromString(s));
  }

  /**
   * parses the provided input time string as a day-of-month specification
   *
   * recognizes times of the format 2020-11-30T14:33:12.442
   *
   * assumes time zone UTC
   *
   * @param s IN the string to parse as a year, month, day timestamp
   * @return a new time object representing the time specified by the string
   */
  private static Time fromDOM(String s) {
    return new Time(java.time.Instant.from(formatDOM.parse(s))
                                     .toEpochMilli() / 1000.0);
  }


  /**
   * creates a new time at the provided duration from the reference epoch
   *
   * client code should use the other factories / add methods
   *
   * @param jplET_s IN the duration from the reference epoch measured in
   *     ephemeris time seconds
   */
  protected Time(double jplET_s) {
    this.jplET_s = jplET_s;
  }

  public static Time fromMilli(long milli) {
    return new Time(milli / 1000.0);
  }

  /**
   * span of time since the reference epoch
   *
   * expressed in SI seconds, and in the jpl T_eph "ephemeris time" scale
   * unless noted otherwise by client code
   *
   * it represents a physical period of real time, not a period between clock
   * readings, and is thus immune to leap seconds/days etc
   *
   * the span may be negative, expressing a difference to an earlier time point
   */
  //TODO: switch to fixed precision to avoid floating point mess
  private final double jplET_s;


  /**
   * the constant object representing the zero time, ie the reference epoch
   * time point
   */
  private static final Time zeroConstant = new Time(0.0);


  /**
   * the time formatter used to emit seq toolchain friendly time formats
   *
   * produces UTC times in the form 2020-122T14:33:66.221
   */
  private static final DateTimeFormatter formatDOY =
      new DateTimeFormatterBuilder().appendPattern("uuuu-DDD'T'HH:mm:ss")
                                    .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true).toFormatter()
                                    .withZone(ZoneId.of("Z"));


  /**
   * the time formatter used to emit human friendly yearh, month, day format
   *
   * produces UTC times in the form 2020-11-30T14:33:66.221
   */
  private static final DateTimeFormatter formatDOM
      = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                         .withZone(ZoneId.of("Z"));

  public enum Operator {
    PLUS,
    MINUS,
  }

public static boolean isPositiveOverflowAdd(Duration t1, Duration t2){
    if(t1.isPositive() && t2.isPositive()){
      return true;
    } else if(t1.isNegative() && t2.isNegative()){
      return false;
  }
  throw new RuntimeException("Should not be overflow");
}

public static boolean isPositiveOverflowMinus(Duration t1, Duration t2){
  return t1.isPositive() && t2.isNegative() || t1.isNegative() && t2.isPositive();
}

  public static Duration performOperation(Operator op, Duration t1, Duration d) {
    switch (op) {
      case PLUS:
        try {
          return t1.plus(d);
        }catch(ArithmeticException e){
          if(isPositiveOverflowAdd(t1,d)) {
            return Duration.MAX_VALUE;
          } else{
            return Duration.MIN_VALUE;
          }
        }
      case MINUS:
        try {
          return t1.minus(d);
        }catch(ArithmeticException e){
          if(isPositiveOverflowMinus(t1,d)) {
            return Duration.MAX_VALUE;
          } else{
            return Duration.MIN_VALUE;
          }
        }
      default:
        throw new IllegalArgumentException("Unknown operation");
    }
  }

}

