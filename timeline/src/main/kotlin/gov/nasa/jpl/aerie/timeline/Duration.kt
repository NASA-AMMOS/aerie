package gov.nasa.jpl.aerie.timeline

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

/**
 * A relative duration of time, represented as a long integer of microseconds.
 */
data class Duration(private val micros: Long) : Comparable<Duration> {

  /** Returns the negative of this duration. */
  fun negate() = Duration(-micros)
  /** @see negate */
  operator fun unaryMinus() = negate()

  /** Adds another duration to this. */
  operator fun plus(other: Duration) = Duration(micros + other.micros)

  /** Adds this to an instant, to produce another instant. */
  operator fun plus(instant: Instant) = instant + this

  /** Add this and another duration, saturating at the maximum and minimum bounds. */
  infix fun saturatingPlus(other: Duration) = Duration(saturatingAddInternal(micros, other.micros))

  /** Subtracts another duration from this. */
  operator fun minus(other: Duration) = Duration(micros - other.micros)

  /** Subtract another duration from this, saturating at the maximum and minimum bounds. */
  infix fun saturatingMinus(other: Duration) = saturatingPlus(-other)

  /** Multiplies this by a long scalar. */
  operator fun times(scalar: Long) = Duration(micros * scalar)
  /** Multiplies this by a double scalar. */
  operator fun times(scalar: Double) = roundNearest(scalar, this)

  /**
   * Divides this by a duration divisor to produce a long, rounded down.
   *
   * To calculate this division as a double, use [ratioOver].
   */
  operator fun div(divisor: Duration) = micros / divisor.micros
  /** Divides this by a long divisor. */
  operator fun div(divisor: Long) = Duration(micros / divisor)
  /** Divides this by a double divisor. */
  operator fun div(divisor: Double) = roundNearest(1 / divisor, this)

  /** Remainder division between this and another duration. */
  operator fun rem(divisor: Duration) = Duration(micros % divisor.micros)

  /** Computes the ratio of this over another duration, as a double. */
  infix fun ratioOver(unit: Duration): Double {
    val integralPart = micros / unit.micros
    val fractionalPart = ((micros % unit.micros).toDouble()
        / unit.micros.toDouble())
    return integralPart + fractionalPart
  }

  /** Whether this is shorter than another duration. */
  infix fun shorterThan(other: Duration) = this < other

  /** Whether this is shorter than or equal to another duration. */
  infix fun shorterThanOrEqualTo(other: Duration) = this <= other

  /** Whether this is longer than another duration. */
  infix fun longerThan(other: Duration) = this > other

  /** Whether this is longer than or equal to another duration. */
  infix fun longerThanOrEqualTo(other: Duration) = this >= other

  /** Creates an interval from this to another duration, inclusive. */
  operator fun rangeTo(other: Duration) = Interval.between(this, other)

  /** Creates an interval from this to another duration, including the start but excluding the end. */
  operator fun rangeUntil(other: Duration) = Interval.betweenClosedOpen(this, other)

  /** Whether this duration is negative. */
  fun isNegative() = micros < 0

  /** Whether this duration is positive. */
  fun isPositive() = micros > 0

  /** Whether this duration is equal to zero. */
  fun isZero() = micros == 0L

  /** Converts this duration to an ISO 8601 duration string. */
  fun toISO8601() = java.time.Duration.of(micros, ChronoUnit.MICROS).toString()

  /**
   * Obtain a human-readable representation of this duration.
   *
   * For example, `duration(-5, MINUTES).plus(1, SECOND).plus(2, MICROSECONDS)` is rendered as "-00:04:58.999998".
   */
  override fun toString(): String {
    var rest = this
    val sign = if (isNegative()) "-" else "+"
    val hours: Long
    if (isNegative()) {
      hours = -(rest / HOUR)
      rest = -(rest % HOUR)
    } else {
      hours = rest / HOUR
      rest %= HOUR
    }
    val minutes = rest / MINUTE
    rest %= MINUTE
    val seconds = rest / SECOND
    rest %= SECOND
    val microseconds = rest / MICROSECOND
    return String.format("%s%02d:%02d:%02d.%06d", sign, hours, minutes, seconds, microseconds)
  }

  /** Determine whether this duration is greater than, less than, or equal to another duration.  */
  override fun compareTo(other: Duration) = micros.compareTo(other.micros)

  /***/ companion object {
    /**
     * The smallest observable span of time between instants.
     *
     * This quantity can be used to obtain the smallest representable deviation from a given instant, i.e.
     * `instant.minus(EPSILON)` and `instant.plus(EPSILON)`. The precise deviation from `instant` should not be assumed,
     * except that it is no larger than [Duration.MICROSECOND].
     *
     */
    @JvmField val EPSILON = Duration(1)

    /**
     * The empty span of time.
     */
    @JvmField val ZERO = Duration(0)

    /**
     * The largest observable negative span of time. Attempting to go "more negative" will cause an exception.
     *
     * The value of this quantity should not be assumed.
     * Currently, this is precisely -9,223,372,036,854,775,808 microseconds, or approximately -293,274 years.
     *
     */
    @JvmField val MIN_VALUE = Duration(Long.MIN_VALUE)

    /**
     * The largest observable positive span of time. Attempting to go "more positive" will cause an exception.
     *
     * The value of this quantity should not be assumed.
     * Currently, this is precisely +9,223,372,036,854,775,807 microseconds, or approximately 293,274 years.
     *
     */
    @JvmField val MAX_VALUE = Duration(Long.MAX_VALUE)

    /** One microsecond (μs).  */
    @JvmField val MICROSECOND = Duration(1)

    /** One millisecond (ms), equal to 1000μs.  */
    @JvmField val MILLISECOND = MICROSECOND * 1000

    /** One second (s), equal to 1000ms.  */
    @JvmField val SECOND = MILLISECOND * 1000

    /** One minute (m), equal to 60s.  */
    @JvmField val MINUTE = SECOND * 60

    /** One hour (h), equal to 60m.  */
    @JvmField val HOUR = MINUTE * 60

    /** One hour (d), equal to 24h.  */
    @JvmField val DAY = HOUR * 24

    /** Constructs a duration with a given number of microseconds.  */
    @JvmStatic fun microseconds(quantity: Long) = Duration(quantity)

    /** Constructs a duration with a given number of milliseconds.  */
    @JvmStatic fun milliseconds(quantity: Long) = MILLISECOND * quantity

    /** Constructs a duration with a given number of seconds.  */
    @JvmStatic fun seconds(quantity: Long) = SECOND * quantity

    /** Constructs a duration with a given number of minutes.  */
    @JvmStatic fun minutes(quantity: Long) = MINUTE * quantity

    /** Constructs a duration with a given number of hours.  */
    @JvmStatic fun hours(quantity: Long) = HOUR * quantity

    /** Constructs a duration with a given number of days.  */
    @JvmStatic fun days(quantity: Long) = DAY * quantity

    /**
     * Construct a duration in terms of a real quantity of some unit,
     * rounding to the nearest representable value above.
     */
    @JvmStatic fun roundUpward(quantity: Double, unit: Duration): Duration {
      val integerPart = floor(quantity)
      val decimalPart = quantity - integerPart
      return unit * (integerPart.toLong()) + EPSILON.times(ceil(decimalPart * (unit / EPSILON)).toLong())
    }

    /**
     * Construct a duration in terms of a real quantity of some unit,
     * rounding to the nearest representable value below.
     */
    @JvmStatic fun roundDownward(quantity: Double, unit: Duration): Duration {
      val integerPart = floor(quantity)
      val decimalPart = quantity - integerPart
      return unit * (integerPart.toLong()) +
          EPSILON * (floor(decimalPart * (unit / EPSILON)).toLong())
    }

    /**
     * Construct a duration in terms of a real quantity of some unit,
     * rounding to the nearest representable value.
     */
    @JvmStatic fun roundNearest(quantity: Double, unit: Duration): Duration {
      val integerPart = floor(quantity)
      val decimalPart = quantity - integerPart
      return unit * (integerPart.toLong()) + EPSILON * (round(decimalPart * (unit / EPSILON)).toLong())
    }

    private fun saturatingAddInternal(left: Long, right: Long): Long {
      val result = left + right
      return if (result xor left and (result xor right) < 0) {
        Long.MIN_VALUE - (result ushr java.lang.Long.SIZE - 1)
      } else result
    }

    /** Finds the minimum duration among the arguments. */
    @JvmStatic fun min(vararg durations: Duration) = durations.min()

    /** Finds the minimum duration among the arguments. */
    @JvmStatic fun max(vararg durations: Duration) = durations.max()

    /** Adds a duration to an instant, to produce another instant. */
    @JvmStatic operator fun Instant.plus(duration: Duration): Instant = plusMillis(duration / MILLISECOND)
        .plusNanos(1000 * ((duration % MILLISECOND).micros))

    /** Subtracts a duration from an instant, to produce another instant. */
    @JvmStatic operator fun Instant.minus(other: Instant) = Duration(other.until(this, ChronoUnit.MICROS))

    /** Parses a duration from a ISO 8601 formatted duration string. */
    @JvmStatic fun parseISO8601(iso8601String: String?): Duration {
      val javaDuration = java.time.Duration.parse(iso8601String)
      return microseconds(javaDuration.seconds * 1000000L + javaDuration.nano / 1000L)
    }
  }
}
