package gov.nasa.jpl.aerie.scheduler;

/**
 * represents a span of time between two time points on the same timeline
 *
 */
public class Duration implements Comparable<Duration> {

  /**
   * returns reference to a zero duration, ie it spans no time at all
   *
   * @return a duration representing zero elapsed time
   */
  public static Duration ofZero() {
    return zeroConstant;
  }

  /**
   * return reference to maximum duration.
   * (created for handling open-ended temporal constraints)
   * @return the maximum duration
   */
  public static Duration ofMaxDur() {
    return maxDur;
  }

  /**
   * creates a duration representing the given number of seconds
   *
   * input may be positive, negative, or zero. the input is interpreted as
   * ephemeris time seconds.
   *
   * @param secs IN the number of elapsed seconds
   * @return a new duration representing the given number of seconds
   */
  public static Duration ofSeconds( double secs ) {
    return new Duration( secs );
  }


  /**
   * creates a duration representing the given number of minutes
   *
   * input may be positive, negative, or zero. the input is interpreted as
   * ephemeris time minutes, ie exactly 60 seconds.
   *
   * @param mins IN the number of elapsed minutes
   * @return a new duration representing the given number of minutes
   */
  public static Duration ofMinutes( double mins ) {
    return new Duration( 60.0 * mins );
  }


  /**
   * creates a duration representing the given number of hours
   *
   * input may be positive, negative, or zero. the input is interpreted as
   * ephemeris time hours, ie exactly 3600 seconds.
   *
   * @param hours IN the number of elapsed hours
   * @return a new duration representing the given number of hours
   */
  public static Duration ofHours( double hours ) {
    return new Duration( 60.0 * 60.0 * hours );
  }


  /**
   * creates a duration representing the given number of hours
   *
   * input may be positive, negative, or zero. the input is interpreted as
   * ephemeris time days, ie exactly 86400 seconds.
   *
   * @param days IN the number of elapsed days
   * @return a new duration representing the given number of days
   */
  public static Duration ofDays( double days ) {
    return new Duration( 24.0 * 60.0 * 60.0 * days );
  }


  /**
   * creates a duration that is the additive inverse of this duration
   *
   * @return a new duration representing the additive inverse of this
   */
  public Duration negate() {
    return new Duration( - this.jplET_s );
  }


  /**
   * creates a duration that is this duration scaled linearly by given factor
   *
   * note that input or this duration may be negative, which may result in
   * sign changes
   *
   * @param factor IN the linear scaling factor to apply
   * @return a new duration that is this duration linearly scaled by the factor
   */
  public Duration times( double factor ) {
    return new Duration( this.jplET_s * factor );
  }


  /**
   * creates a duration that is the sum of this duration and provided argument
   *
   * note that inputs may be negative, in which case it becomes a subtraction
   *
   * @param addend IN additional duration to combine into the result
   * @return a new duration representing the sum of this and the addend
   */
  public Duration plus( Duration addend ) {
    return new Duration( this.jplET_s + addend.jplET_s );
  }


  /**
   * creates a duration that is the difference of this minus provided argument
   *
   * note that the input may be negative, in which case it becomes an addition
   *
   * @param subtrahend IN duration to discount from the result
   * @return a new duration difference representing the subtraction of this
   *         minuend minus the given subtrahend
   */
  public Duration minus( Duration subtrahend ) {
    return new Duration( this.jplET_s - subtrahend.jplET_s );
  }


  /**
   * {@inheritDoc}
   *
   * naturally orders durations based on their length (which may be negative)
   *
   * @return a negative, zero, or positive number in the event this duration
   *         is, respectively, less, equal, or greater than the provided
   *         duration argument
   */
  @Override public int compareTo( Duration o ) {
    return Double.compare( this.jplET_s, o.jplET_s );
  }


  /**
   * converts this duration into a number of ephemeris time seconds
   *
   * @return the number of ephemeris time seconds that this duration represents
   */
  public double toSeconds() {
    return jplET_s;
  }


  /**
   * converts this duration into a number of ephemeris time milliseconds
   *
   * @return the number of ephemeris time milliseconds represented by this duration
   */
  public long toMilliseconds() {
    return (long)( jplET_s * 1000.0 );
  }


  /**
   * {inheritDoc}
   *
   * serializes this duration into a format amenable to use by jpl seq
   * toolchain tools (apgen, raven, etc)
   */
  @Override public String toString() {
    //or could produce standard xml durations (which aren't apgen/raven friendly?)
    //    return java.time.Duration.ofMillis( (long)( jplET_s * 1000.0 ) ).toString();
    final var dur = java.time.Duration.ofMillis( (long)( jplET_s * 1000.0 ) );
    final var hmsfStr = String.format(
      "%d:%02d:%02d.%03d",
      dur.toHours(),
      dur.toMinutesPart(),
      dur.toSecondsPart(),
      dur.toMillisPart()
      );
    return hmsfStr;
  }


  /**
   * parses the input string as a duration in ephemeris time
   *
   * understands the standard xml duration syntax, eg -P2DT3H4M
   *
   * NB: TODO for now this is asymetric with the toString() serialization in
   * order to support the downstream seq toolchain
   *
   * @param s IN the string to parse as an xml duration
   * @return a new duration object representing the time span from the string
   */
  public static Duration fromString( String s ) {
    return new Duration( java.time.Duration.parse( s ).toMillis() / 1000.0 );
  }


  /**
   * internal ctor creates a duration matching provided ephemeris seconds
   *
   * @param jplET_s IN the positive or negative number of ephemeris time
   *        seconds that the new duration should represent
   */
  protected Duration( double jplET_s ) {
    this.jplET_s = jplET_s;
  }


  /**
   * span of time between two points
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
  private double jplET_s;

  /**
   * the constant object representing a zero duration
   */
  private static Duration zeroConstant = new Duration( 0.0 );


  /**
   * TODO:
   */
  private static Duration maxDur = new Duration(Double.MAX_VALUE-1);

}
