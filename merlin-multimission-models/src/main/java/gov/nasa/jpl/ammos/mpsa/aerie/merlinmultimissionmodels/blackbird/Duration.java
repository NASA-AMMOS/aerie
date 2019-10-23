package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.blackbird;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.ConvertableFromString;
import org.apache.commons.lang3.time.DurationFormatUtils;



//TODO: TAKEN FROM COMMIT 46c233211ccd68376953ed6408ad0e094bb07dbb at https://github.jpl.nasa.gov/M20-Surface-Ops-Tools/jplTime/commit/46c233211ccd68376953ed6408ad0e094bb07dbb
//TODO: GET RID OF THIS FILE AND GET FROM ARTIFACTORY

/**
 * This class is used to represent Durations of time. It is the complement of the SPICE-backed Time class in the same
 * package - subtracting two Times returns a Duration, for example. It keeps the same backing structure as the time class
 * (TAI tics of 10 nanoseconds each), so doing math between them is extremely easy. It supports many math functions
 * that planning system developers have found useful in the past, and outputs to the JPL-standard duration string format.
 * Even though conceptually a Duration is always a positive value, this class can represent negative Durations as well,
 * since it is a very common use case to offset Times in a negative direction.
 */
public class Duration implements Comparable<Duration>, ConvertableFromString {
    //<editor-fold desc="fields">

    // the backing data structure for this class - as a balance between accuracy and range each tic will be ten TAI nanoseconds
    // we will claim that the class is only accurate to within a microsecond - this will be good enough for all use cases but not build up too much rounding error
    // this is now called tics instead of what it stands for directly so if we change precision we don't have to rename everything
    private long tics;
    public final static long TICS_PER_SECOND_LONG           = 100000000L;
    final static int numberDecimalDigits                    = (int) Math.log10(TICS_PER_SECOND_LONG);
    private static int DEFAULT_OUTPUT_PRECISION             = 6;

    public final static double TICS_PER_SECOND_DOUBLE               = (double) TICS_PER_SECOND_LONG;
    public final static long ONE_DAY                                = 86400 * TICS_PER_SECOND_LONG;
    public final static long ONE_HOUR                               = 3600 * TICS_PER_SECOND_LONG;
    public final static long ONE_MINUTE                             = 60 * TICS_PER_SECOND_LONG;
    public final static long ONE_SECOND                             = TICS_PER_SECOND_LONG;
    public final static long ONE_MILLISECOND                        = TICS_PER_SECOND_LONG/1000L;
    public final static long ONE_MICROSECOND                        = TICS_PER_SECOND_LONG/1000000L;

    public final static Duration ZERO_DURATION          = new Duration(0);
    public final static Duration MICROSECOND_DURATION   = new Duration(TICS_PER_SECOND_LONG/ONE_MICROSECOND);
    public final static Duration SECOND_DURATION        = new Duration(TICS_PER_SECOND_LONG);
    public final static Duration MINUTE_DURATION        = new Duration(ONE_MINUTE);
    public final static Duration HOUR_DURATION          = new Duration(ONE_HOUR);
    public final static Duration DAY_DURATION           = new Duration(ONE_DAY);

    // Duration input regex
    public static final String DURATION_REGEX = "(?<sign>-)?((?<days>\\d+)?T|T?)(?<timeOfDay>(?<hours>\\d+):(?<minutes>\\d+):(?<fullSeconds>(?<seconds>\\d+)(?:\\.(?<decimal>\\d+))?))";
    public final static Pattern durationPattern = Pattern.compile(DURATION_REGEX);

    // mars duration regex
    public static final String SOLS_IN_DURATION_REGEX = "(\\d+)";
    public static final String MARS_DURATION_LESS_THAN_ONE_SOL_REGEX = "M(\\d+):(\\d+):(\\d+(\\.\\d+)?)";
    public static final String MARS_DUR_REGEX = "^(-)?" + SOLS_IN_DURATION_REGEX + "?" + MARS_DURATION_LESS_THAN_ONE_SOL_REGEX + "$";
    public static final Pattern MARS_DUR_PATTERN = Pattern.compile(MARS_DUR_REGEX);

    // mars duration variables
    public static final double MARS_TIME_SCALE = 1.02749125170;

    //</editor-fold>

    //<editor-fold desc="constructors">

    /**
     * The main constructor everyone should be using - given a duration string, returns an equivalent Duration object
     * @param d A string in format HH:MM:SS.ssssss (though it is pretty forgiving about not including leading or trailing zeros)
     */
    public Duration(String d) {
        valueOf(d);
    }

    /**
     * Constructor just for the math functions to use
     * @param tics
     */
    Duration(long tics) {
        this.tics = tics;
    }

    /**
     * This is the empty Duration constructor, which will use 0 tics for the input. It is needed when you need to create
     * Duration objects then assign them a value later (which should be a rare case).
     */
    public Duration() {
        tics = 0;
    }

    //</editor-fold>

    //<editor-fold desc="static methods to create durations">

    /**
     * Takes in a number of seconds as a double and returns a corresponding duration.
     * @param seconds
     * @return
     */
    public static Duration fromSeconds(double seconds) {
        return SECOND_DURATION.multiply(seconds);
    }

    /**
     * Takes in a number of seconds as a long and returns a corresponding duration.
     * @param seconds
     * @return
     */
    public static Duration fromSeconds(long seconds) {
        return SECOND_DURATION.multiply(seconds);
    }

    /**
     * Takes in a number of minutes as a double and returns a corresponding duration.
     * @param minutes
     * @return
     */
    public static Duration fromMinutes(double minutes) {
        return MINUTE_DURATION.multiply(minutes);
    }

    /**
     * Takes in a number of minutes as a long and returns a corresponding duration.
     * @param minutes
     * @return
     */
    public static Duration fromMinutes(long minutes) {
        return MINUTE_DURATION.multiply(minutes);
    }

    /**
     * Takes in a number of hours as a double and returns a corresponding duration.
     * @param hours
     * @return
     */
    public static Duration fromHours(double hours) {
        return HOUR_DURATION.multiply(hours);
    }

    /**
     * Takes in a number of hours as a long and returns a corresponding duration.
     * @param hours
     * @return
     */
    public static Duration fromHours(long hours) {
        return HOUR_DURATION.multiply(hours);
    }

    /**
     * Takes in a number of days as a double and returns a corresponding duration.
     * @param days
     * @return
     */
    public static Duration fromDays(double days) {
        return DAY_DURATION.multiply(days);
    }

    /**
     * Takes in a number of days as a long and returns a corresponding duration.
     * @param days
     * @return
     */
    public static Duration fromDays(long days) {
        return DAY_DURATION.multiply(days);
    }

    /**
     * Takes in a Mars duration string of the form
     * ddddMhh:mm:ss.fff or Mhh:mm:ss.fff
     * where dddd is the sol number, hh is the hour, mm
     * is the minutes, and ss.fff is the seconds.
     * @param marsDurString
     * @return
     */
    public static Duration fromMarsDur(String marsDurString) {
        Matcher matcher = MARS_DUR_PATTERN.matcher(marsDurString);
        if (matcher.find()) {
            String earthDurString = marsDurString.replace("M", "T");
            Duration earthDuration = new Duration(earthDurString);
            return earthDuration.multiply(MARS_TIME_SCALE);
        }
        else {
            throw new RuntimeException("Error creating Mars duration from string " + marsDurString + ". String" +
                    " did not match expected form dddMhh:mm:ss.fff");
        }
    }

    //</editor-fold>

    //<editor-fold desc="getter functions that just get different powers of underlying tic value">

    /**
     * @return A long, the underlying number of tics that comprise the duration
     */
    public long getTics() {
        return tics;
    }

    /**
     * @return A long, the number of microseconds in the duration. Truncates instead of rounding
     */
    public long getMicroseconds(){
        return tics/ONE_MICROSECOND;
    }

    /**
     * @return A long, the number of milliseconds in the duration. Truncates instead of rounding
     */
    public long getMilliseconds(){
        return tics/ONE_MILLISECOND;
    }

    /**
     * this one is special because it is a float
     * @return A float representing the fractional seconds in the span of time. Does not round or truncate
     */
    public double totalSeconds() {
        return ((double) tics) / TICS_PER_SECOND_DOUBLE;
    }

    /**
     * @return A long, the number of seconds in the duration. Truncates instead of rounding
     */
    public long getSeconds(){
        return tics/ONE_SECOND;
    }

    /**
     * @return A long, the number of minutes in the duration. Truncates instead of rounding
     */
    public long getMinutes(){
        return tics/ONE_MINUTE;
    }

    /**
     * @return A long, the number of hours in the duration. Truncates instead of rounding
     */
    public long getHours(){
        return tics/ONE_HOUR;
    }

    /**
     * @return A long, the number of days in the duration. Truncates instead of rounding
     */
    public long getDays(){
        return tics/ONE_DAY;
    }

    //</editor-fold>

    //<editor-fold desc="math functions">
    /**
     * Adds the parameter duration to the calling object.
     * @param d2
     * @return A new Duration object
     */
    public Duration add(Duration d2) {
        return new Duration(tics + d2.tics);
    }

    /**
     * Subtracts the parameter duration from the calling object
     * @param d2
     * @return A new Duration object
     */
    public Duration subtract(Duration d2) {
        return new Duration(tics - d2.tics);
    }

    /**
     * Multiplies the duration by the input double value. Rounds to the nearest microsecond or double floating point precision, whichever is larger.
     * @param multiplyBy
     * @return
     */
    public Duration multiply(double multiplyBy) {
        return new Duration(Math.round(tics * multiplyBy));
    }

    /**
     * Multiplies the duration by the input long value.
     * @param multiplyBy
     * @return
     */
    public Duration multiply(long multiplyBy) {
        return new Duration(tics * multiplyBy);
    }

    /**
     * Multiplies the duration by the input int value.
     * @param multiplyBy
     * @return
     */
    public Duration multiply(int multiplyBy) {
        return new Duration(tics * multiplyBy);
    }

    /**
     * Divides the duration tics by the input double value and returns a duration. Rounds to the nearest microsecond or double floating point precision, whichever is larger.
     * @param divideBy
     * @return
     */
    public Duration divide(double divideBy) {
        return new Duration(Math.round(tics / divideBy));
    }

    /**
     * Divides the duration tics (floating point division) by the input long value and returns a duration. Rounds to the nearest microsecond or double floating point precision, whichever is larger.
     * @param divideBy
     * @return
     */
    public Duration divide(long divideBy) {
        return new Duration(Math.round((double) tics / divideBy));
    }

    /**
     * Divides the duration tics (floating point division) by the input int value and returns a duration. Rounds to the nearest microsecond or double floating point precision, whichever is larger.
     * @param divideBy
     * @return
     */
    public Duration divide(int divideBy) {
        return new Duration(Math.round((double) tics / divideBy));
    }

    /**
     * Divides the duration by an input duration and returns a double value.
     * @param divideByDuration
     * @return
     */
    public double divide(Duration divideByDuration) {
        return ((double) tics) / divideByDuration.tics;
    }

    /**
     * Returns the absolute value of the duration. i.e. -10:00:00 will become 10:00:00.
     * @return
     */
    public Duration abs() {
        return new Duration(Math.abs(tics));
    }

    /**
     * Rounds the duration to an input resolution.
     * @param resolution
     * @return
     */
    public Duration round(Duration resolution) {
        long scale = Math.round(((double) tics) / resolution.getTics());
        return new Duration(scale * resolution.getTics());
    }

    /**
     * Rounds the duration up to an input resolution.
     * @param resolution
     * @return
     */
    public Duration ceil(Duration resolution) {
        long scale = (long) Math.ceil(((double) tics) / resolution.getTics());
        return new Duration(scale * resolution.getTics());
    }

    /**
     * Rounds the duration down to an input resolution.
     * @param resolution
     * @return
     */
    public Duration floor(Duration resolution) {
        long scale = (long) Math.floor(((double) tics) / resolution.getTics());
        return new Duration(scale * resolution.getTics());
    }

    /**
     * Returns true if the calling object is less than (closer to negative infinity) the parameter, false otherwise
     * @param t2
     * @return a boolean
     */
    public boolean lessThan(Duration t2) {
        return tics < t2.tics;
    }

    /**
     * Returns true if the calling object is greater than (closer to positive infinity) the parameter, false otherwise
     * @param t2
     * @return
     */
    public boolean greaterThan(Duration t2) {
        return tics > t2.tics;
    }

    /**
     * Returns true if the calling object is less than or equal to the parameter, false otherwise
     * @param t2
     * @return a boolean
     */
    public boolean lessThanOrEqualTo(Duration t2) {
        return tics <= t2.tics;
    }

    /**
     * Returns true if the calling object is greater than or equal to the parameter, false otherwise
     * @param t2
     * @return a boolean
     */
    public boolean greaterThanOrEqualTo(Duration t2) {
        return tics >= t2.tics;
    }

    /**
     * Compares the duration to another duration object and returns true if d2
     * is within an input resolution of d1.
     * @param d2
     * @param resolution
     * @return
     */
    public boolean equalToWithin(Duration d2, Duration resolution) {
        long lower_bound = d2.tics - resolution.getTics();
        long upper_bound = d2.tics + resolution.getTics();

        return ((tics >= lower_bound) && tics <= upper_bound);
    }

    //</editor-fold>

    //<editor-fold desc="serialization">

    /**
     * Calls toString() with default precision
     * @return The default String representation of the object
     */
    @Override
    public String toString() {
        return toString(getDefaultOutputPrecision());
    }

    /**
     * Returns string representation to number of decimal places specified
     * @param numDecimalPlaces The number of places to print subseconds to
     * @return A string representation of the duration
     */
    public String toString(int numDecimalPlaces){
        if(numDecimalPlaces > numberDecimalDigits){
            numDecimalPlaces = numberDecimalDigits;
        }

        // Duration output formats
        String formatForLessThanOneDay;
        if(numDecimalPlaces == 0) {
            formatForLessThanOneDay = "%02d:%02d:%02d";
        }
        else{
            formatForLessThanOneDay = "%02d:%02d:%02d.%0" + numDecimalPlaces + "d";
        }

        String formatForGreaterThanOneDay = "%dT" + formatForLessThanOneDay;

        long ticsRounder = 1;
        for(int i = 0; i<(numberDecimalDigits-numDecimalPlaces); i++){
            ticsRounder *= 10;
        }

        long roundedTics = Math.round(((double) tics) / ticsRounder)*ticsRounder;

        long remainder;
        long days = Math.abs(roundedTics) / ONE_DAY;
        remainder = Math.abs(roundedTics) % ONE_DAY;
        long hours = remainder / ONE_HOUR;
        remainder = remainder % ONE_HOUR;
        long minutes = remainder / ONE_MINUTE;
        remainder = remainder % ONE_MINUTE;
        long seconds = remainder / ONE_SECOND;
        long onlyTics = remainder % ONE_SECOND;
        long subseconds = onlyTics / ticsRounder;

        // need to add a - to negative durations
        String sign = "";
        if (roundedTics < 0) {
            sign = "-";
        }

        if (Math.abs(roundedTics) >= ONE_DAY) {
            return sign + String.format(formatForGreaterThanOneDay, days, hours, minutes, seconds, subseconds);
        }
        else {
            return sign + String.format(formatForLessThanOneDay, hours, minutes, seconds, subseconds);
        }


    }


    /**
     * Returns a string representing the duration as a mars duration.
     * @param numberDecimalDigits
     * @return
     */
    public String toMarsDurString(int numberDecimalDigits) {
        Duration marsDur = divide(MARS_TIME_SCALE); // create a fake duration to use the normal formatter
        String earthDurString = marsDur.toString(numberDecimalDigits);
        Matcher dayMatcher = durationPattern.matcher(earthDurString);

        // if there are days in the regex then these represent sols, so just replace the T with an M
        if (dayMatcher.find() && dayMatcher.group("days") != null) {
            return earthDurString.replace("T", "M");
        }
        // if there aren't any days then add an M in front if positive, otherwise add after the negative sign
        else {
            if (marsDur.getTics() >= 0) {
                return "M" + earthDurString;
            }
            else {
                return "-M" + earthDurString.substring(1);
            }
        }
    }

    /**
     * Returns a string representing the duration as a mars duration, using the default decimal precision.
     * @return
     */
    public String toMarsDurString() {
        return toMarsDurString(getDefaultOutputPrecision());
    }

    /**
     * Returns string that is the underlying duration formatted according to the format string
     * The format string is defined by the apache commons DurationFormatUtils specification
     * It only returns down to millisecond accuracy, so it cannot be used for fine precision
     * @param format Format string, an example of which would be "d 'days' HH 'hours' mm 'minutes' ss 'seconds and' S 'milliseconds'"
     */
    public String format(String format){
        return DurationFormatUtils.formatDuration(Math.round(getMicroseconds()/(double)(ONE_MILLISECOND/ONE_MICROSECOND)), format, false);
    }

    /**
     * Given a Duration string, mutates the calling object to represent that duration
     * Implements the convertableFromString interface so engines can call valueOf on whatever object including Durations.
     * @param s
     */
    @Override
    public void valueOf(String s) {
        // Create Matcher objects
        Matcher durationMatcher = durationPattern.matcher(s);

        if (durationMatcher.find()) {

            // if the negative regex matched then we want to multiply the tics by -1
            long negativeMultiplier = 1;
            if(durationMatcher.group("sign") != null) {
                negativeMultiplier = -1;
            }

            long days = 0;
            if(durationMatcher.group("days") != null) {
                days = Integer.parseInt(durationMatcher.group("days"));
            }

            long hours = Long.parseLong(durationMatcher.group("hours"));
            long minutes = Long.parseLong(durationMatcher.group("minutes"));
            double seconds = Double.parseDouble(durationMatcher.group("fullSeconds"));
            tics = days * ONE_DAY + hours * ONE_HOUR + minutes * ONE_MINUTE + Math.round(seconds * ONE_SECOND);
            tics = tics * negativeMultiplier;
        }
        else {
            throw new RuntimeException("Cannot cast " + s + " to Duration - does not match expected format");
        }
    }
    //</editor-fold>

    //<editor-fold desc="methods specified in the java Object standard so built-in data structures work well with them">
    @Override
    public int compareTo(Duration t2) {
        return Long.valueOf(tics).compareTo(t2.tics);
    }

    @Override
    public boolean equals(Object t2) {
        if (!(t2 instanceof Duration)) {
            return false;
        }
        else {
            return tics == ((Duration) t2).tics;
        }
    }

    @Override
    public int hashCode() {
        return (int) tics;
    }
    //</editor-fold>

    //<editor-fold desc="static methods for setting defaults">

    /**
     * Sets the default output decimal precision.
     * @param defaultOutputPrecision
     */
    public static void setDefaultOutputPrecision(int defaultOutputPrecision) {
        DEFAULT_OUTPUT_PRECISION = defaultOutputPrecision;
    }

    /**
     * Returns the current default output decimal precision.
     * @return
     */
    public static int getDefaultOutputPrecision() {
        return DEFAULT_OUTPUT_PRECISION;
    }

    //</editor-fold>
}