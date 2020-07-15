package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

/**
 * Units of the passage of time.
 *
 * <p>
 * DAY, WEEK, MONTH, and YEAR are *not* included, because their values depend on properties
 * of the particular calendrical system being used.
 * </p>
 *
 * <ul>
 * <li>
 *   The notion of "day" depends on the astronomical system against which time is measured.
 *   For example, the synodic (solar) day and the sidereal day are distinguished by which celestial body is held fixed
 *   in the sky by the passage of a day. (Synodic time fixes the body being orbited around; sidereal time
 *   fixes the far field of stars.)
 *
 * <li>
 *   The notion of "year" has precisely the same problem, with a similar synodic/sidereal distinction.
 *
 * <li>
 *   <p>
 *   The notion of "month" is worse, in that it depends on the presence of a *tertiary* body whose sygyzies with the
 *   other two bodies delimit integer quantities of the unit. (A syzygy is a collinear configuration of the bodies.)
 *   The lunar calendar (traditionally used in China) is based on a combination of lunar and solar
 *   synodic quantities. ("Month" derives from "moon".)
 *   </p>
 *
 *   <p>
 *   The month of the Gregorian calendar is approximately a lunar synodic month, except that the definition was
 *   intentionally de-regularized (including intercalary days) in deference to the Earth's solar year.
 *   (Other calendars even invoke days *outside of any month*, which Wikipedia claims are called "epagomenal days".)
 *   In retrospect, it is unsurprising that ISO 8601 ordinal dates drop the month altogether,
 *   since "month" is a (complicated) derived notion in the Gregorian calendar.
 *   </p>
 *
 * <li>
 *   The notion of "week" seemingly has no basis in the symmetries of celestial bodies, and is instead a derived unit.
 *   Unfortunately, not only is it fundamentally based on the notion of "day", different calendars assign a different
 *   number of days to the span of a week.
 * </ul>
 *
 * <p>
 * If you are working within the Gregorian calendar, the standard `java.time` package has you covered.
 * </p>
 *
 * <p>
 * If you are working with spacecraft, you may need to separate concepts such as "Earth day" and "Martian day", which
 * are synodic periods measured against the Sun but from different bodies. Worse, you likely need to convert between
 * such reference systems frequently, with a great deal of latitude in the choice of bodies being referenced.
 * The gold standard is the well-known SPICE toolkit, coupled with a good set of ephemerides and clock kernels.
 * </p>
 *
 * <p>
 * If you're just looking for a rough estimate, you can define 24-hour days and 7-day weeks and 30-day months
 * within your own domain in terms of the precise units we give here.
 * </p>
 */
public enum TimeUnit {
  MICROSECONDS,  // The base unit
  MILLISECONDS,  // 1000 microseconds
  SECONDS,       // 1000 milliseconds
  MINUTES,       // 60 seconds
  HOURS,         // 60 minutes
}
