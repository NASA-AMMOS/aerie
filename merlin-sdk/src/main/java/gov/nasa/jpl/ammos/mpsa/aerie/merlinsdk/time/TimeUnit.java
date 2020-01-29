package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

/**
 * Units of time.
 *
 * MONTH and YEAR are *not* included, because they are calendrical quantities depending on time.
 * * The value of MONTH would depend on whether it's February (29 days) or September (30) or August (31).
 * * The value of YEAR would depend on whether it's a leap year.
 * For such quantities, use a calendrical library like JodaTime, JPLTime, or SPICE.
 */
public enum TimeUnit {
  MICROSECONDS,  // The base unit
  MILLISECONDS,  // 1000 microseconds
  SECONDS,       // 1000 milliseconds
  MINUTES,       // 60 seconds
  HOURS,         // 60 minutes
  DAYS,          // 24 hours
  WEEKS,         // 7 days
}
