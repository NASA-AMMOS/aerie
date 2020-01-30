//leveraged from the multi-mission time handling tools in M20-Surface-Ops-Tools
//credit to Forrest Ridenhour, Chris Lawler, et al
//
//ref: https://github.jpl.nasa.gov/M20-Surface-Ops-Tools/jplTime/blob/master/src/main/java/gov/nasa/jpl/serialization/ConvertableFromString.java

//AERIE-MODIFICATION: package gov.nasa.jpl.time;
package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime;


import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//AERIE-MODIFICATION: import gov.nasa.jpl.serialization.ConvertableFromString;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.ConvertableFromString;

import spice.basic.CSPICE;
import spice.basic.KernelVarNotFoundException;
import spice.basic.SpiceErrorException;

//AERIE-MODIFICATION; import static gov.nasa.jpl.time.Duration.*;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Duration.*;


import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.ConvertableFromString;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class is a SPICE-backed object-oriented way to represent Time in Java. It is intended to be imported and
 * used/passed around as the sole way your program represents time, except for exporting to other files or programs.
 * It represents time as TAI 10-nanosecond tics, so calculations should generally be accurate to well within a microsecond
 * (though some SPICE calls have inherent uncertainty greater than that). It is backed by not a floating point number
 * so it is suitable to discrete event simulators where there can be no ambiguity as to the ordering of times, and to
 * allow comparison, addition, and multiplication without having to make SPICE calls (they are only made for creating
 * times and exporting formats).
 */
public class Time implements Comparable<Time>, ConvertableFromString {
    //<editor-fold desc="fields">
    // this is the sole backing data structure for the Time class and represents TAI seconds since the SPICE epoch
    // TAI is needed so adding durations is meaningful, but it means you need conversions to go to ET or UTC
    // Each 'tic' is 10 nanoseconds, to strike a balance between precision and length of time expressible while still using a long
    private long tics;

    // this controls if SPICE or java LocalDateTime (standard but not correctly accounting for leap seconds) is used to parse time objects
    private static boolean useSpiceForMath = false;

    // this represents the epoch for the java LocalDateTime class - we will use it for toUTC() conversions without SPICE
    private static LocalDateTime EPOCH = LocalDateTime.of(2000, 1, 1, 12, 0);

    // UNIX epoch represented as Time object
    private static Time UNIX_EPOCH;

    // GPS epoch represented as Time object
    private static Time GPS_EPOCH;

    // this time can be used for default round behavior
    private static Time DEFAULT_REFERENCE_TIME;

    // these values can be used for convenience when calling methods
    private static Integer DEFAULT_SPACECRAFT_ID;
    private static Integer DEFAULT_LST_BODY_ID;
    private static String DEFAULT_LST_BODY_FRAME;
    private static int DEFAULT_OUTPUT_PRECISION = 6;

    // this map will speed up SCLKD calculations because GDPOOL to get the SCLK fractional part takes a while
    private static Map<Integer, Double> SCLK_FRACTIONAL_PART_MAP = new HashMap<>();

    // this is just used for converting to/from UTC if SPICE is not being used
    private static final int NANOSECONDS_PER_TIC = 10;

    public static final Time MAX_TIME = new Time(Long.MAX_VALUE);
    //AERIE-MODIFICATION:
    /**
     * the minimum representable time; compares less than all other times
     */
    public static final Time MIN_TIME = new Time(Long.MIN_VALUE);


    // Time regex
    public static final String TIME_REGEX = "(?<year>\\d+)-(?<DOY>\\d+)T(?<hours>\\d+):(?<minutes>\\d+):(?<seconds>\\d+)\\.?(?<subseconds>\\d+)?";
    public static final Pattern TIME_PATTERN = Pattern.compile(TIME_REGEX);

    // More information to deal with SPICE SCLKs
    public static final String SCLK_REGEX = "\\d\\/(?<seconds>\\d+)-(?<fraction>\\d+)";
    public static final Pattern SCLK_PATTERN = Pattern.compile(SCLK_REGEX);
    public static final String SCLKD_REGEX = "(?<seconds>\\d+)\\.(?<subsec>\\d+)";
    public static final Pattern SCLKD_PATTERN = Pattern.compile(SCLKD_REGEX);

    // LMST conversion information - LMST is treated under the hood of SPICE as a sclk with 100000 tics
    private static final int LMST_TICS_PER_SECOND = 100000;
    public static final String LMST_STANDARD_REGEX = "(Sol|sol|SOL)?-?\\s*(?<sol>\\d+)(M|\\s+)(?<timeOfDay>(?<hours>[0-1][0-9]|[2][0-3]):(?<minutes>[0-5][0-9]):(?<seconds>[0-5][0-9])\\.?(?<decimal>(\\d+))?)";
    public static final Pattern LMST_STANDARD_REGEX_PATTERN = Pattern.compile(LMST_STANDARD_REGEX);
    public static final String LMST_SPICE_REGEX = "\\d\\/(?<sol>\\d+):(?<hour>\\d+):(?<min>\\d+):(?<sec>\\d+):(?<subsec>\\d+)";
    public static final Pattern LMST_SPICE_REGEX_PATTERN = Pattern.compile(LMST_SPICE_REGEX);

    // UTC input formatter in non-SPICE mode
    private static DateTimeFormatter inputUtcFormat = new java.time.format.DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("[uuuu-DDD'T'HH:mm:ss][uuuu MMM dd HH:mm:ss][uuuu MMM d HH:mm:ss][uuuu-MM-dd'T'HH:mm:ss]")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .toFormatter();

    // UTC output formatter in non-SPICE mode
    private static DateTimeFormatter[] outputUTCFormatters = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss["),
            DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss[.S]"),
            DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss[.SS]"),
            DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss[.SSS]"),
            DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss[.SSSS]"),
            DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss[.SSSSS]"),
            DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss[.SSSSSS]"),
            DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss[.SSSSSSS]"),
            DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss[.SSSSSSSS]")
    };

    // constants
    private static final int EARTH_NAIF_ID = 399;

    // enum for AM/PM
    public enum AM_PM {
        AM("AM", 0), PM("PM", 1);

        private String stringValue;
        private int intValue;

        AM_PM(String stringValue, int intValue) {
            this.stringValue = stringValue;
            this.intValue = intValue;
        }

        @Override
        public String toString() {
            return stringValue;
        }

        public int toInt() {
            return intValue;
        }
    }

    //</editor-fold>

    //<editor-fold desc="constructors">

    /**
     * The main Time constructor, that wraps str2et in SPICE if it is enabled, or a Java
     * DateTime if it is not.
     *
     * @param t Time string. When using SPICE, it can be any of the input formats that
     *          str2et supports (there are a lot)
     */
    public Time(String t) {
        valueOf(t);
    }

    /**
     * This private constructor is used by the fromX and valueOf methods to actually
     * assign the backing data.
     */
    private Time(long tics) {
        this.tics = tics;
    }

    /**
     * This is the empty time constructor, which will use 0 tics for the time input. It is
     * needed when you need to create Time objects then assign them a value later (which
     * should be a rare case).
     */
    public Time() {
        this.tics = 0;
    }
    //</editor-fold>

    //<editor-fold desc="static methods that control behavior of overall class and return epoch times">

    /**
     * call this before you start making Time objects if you want Java datetime to do the
     * conversion to and from strings instead of SPICE
     */
    public static void setUseSpiceForMath(boolean shouldUseSpiceForMath) {
        useSpiceForMath = shouldUseSpiceForMath;
    }

    /**
     * Updates the default spacecraft id for convenience methods.
     *
     * @param spacecraftId
     */
    public static void setDefaultSpacecraftId(int spacecraftId) {
        DEFAULT_SPACECRAFT_ID = spacecraftId;
    }

    /**
     * Returns the current default spacecraft id and checks to make sure that it is not
     * null.
     *
     * @return
     */
    public static int getDefaultSpacecraftId() {
        if (DEFAULT_SPACECRAFT_ID == null) {
            throw new RuntimeException("Error getting default spacecraft id. Current value is null but must be an integer." +
                    " Set this with the setDefaultSpacecraftId method.");
        }
        return DEFAULT_SPACECRAFT_ID;
    }

    /**
     * Updates the default body id for LST methods. This would be set to 499 for Mars
     * surface missions, for example.
     *
     * @param bodyId
     */
    public static void setDefaultLstBodyId(int bodyId) {
        DEFAULT_LST_BODY_ID = bodyId;
    }

    /**
     * Returns the current default spacecraft id and checks to make sure that it is not
     * null.
     *
     * @return
     */
    public static Integer getDefaultLstBodyId() {
        if (DEFAULT_LST_BODY_ID == null) {
            throw new RuntimeException("Error getting default LST body id. Current value is null but must be an integer. " +
                    "Set this with the setDefaultLstBodyId method.");
        }
        return DEFAULT_LST_BODY_ID;
    }

    /**
     * Sets the default LST body frame, which is tied to the naif body id.
     *
     * @param defaultLstBodyFrame
     */
    public static void setDefaultLstBodyFrame(String defaultLstBodyFrame) {
        DEFAULT_LST_BODY_FRAME = defaultLstBodyFrame;
    }

    /**
     * Returns the default LST body frame and checks that it is not null.
     *
     * @return
     */
    public static String getDefaultLstBodyFrame() {
        if (DEFAULT_LST_BODY_FRAME == null) {
            throw new RuntimeException("Error getting default LST body frame. Current value is null but must be a string. " +
                    "Set this with the setDefaultLstBodyFrame method.");
        }
        return DEFAULT_LST_BODY_FRAME;
    }

    /**
     * Sets the default output decimal precision.
     *
     * @param defaultOutputPrecision
     */
    public static void setDefaultOutputPrecision(int defaultOutputPrecision) {
        DEFAULT_OUTPUT_PRECISION = defaultOutputPrecision;
    }

    /**
     * Returns the current default output decimal precision.
     *
     * @return
     */
    public static int getDefaultOutputPrecision() {
        return DEFAULT_OUTPUT_PRECISION;
    }

    /**
     * @return A Time object representing the Unix 0 time
     */
    public static Time getUnixEpoch() {
        if (UNIX_EPOCH == null) {
            UNIX_EPOCH = new Time("1970-001T00:00:00");
        }
        return UNIX_EPOCH;
    }

    /**
     * @return A Time object representing the GPS epoch
     */
    public static Time getGPSEpoch() {
        if (GPS_EPOCH == null) {
            GPS_EPOCH = new Time("1980-006T00:00:00");
        }
        return GPS_EPOCH;
    }

    /**
     * @return A time object representing Jan 1 2000 00:00:00, to give a reference for
     * time rounding up or down
     */
    public static Time getDefaultReferenceTime() {
        if (DEFAULT_REFERENCE_TIME == null) {
            DEFAULT_REFERENCE_TIME = new Time("2000-001T00:00:00");
        }
        return DEFAULT_REFERENCE_TIME;
    }

    //</editor-fold>

    //<editor-fold desc="math functions">

    /**
     * Adding a time and duration returns a time
     *
     * @param d The Duration you want to add to the calling Time object
     * @return A new Time object
     */
    public Time add(Duration d) {
        return new Time(tics + d.getTics());
    }

    /**
     * Syntactic sugar for add()
     *
     * @param d
     * @return A new Time object
     */
    public Time plus(Duration d) {
        return add(d);
    }

    /**
     * Subtracting a duration from a Time returns another Time
     *
     * @param d The Duration you want to subtract from the calling Time object
     * @return A new Time object
     */
    public Time subtract(Duration d) {
        return new Time(tics - d.getTics());
    }

    /**
     * Syntactic sugar for subtract(Duration)
     *
     * @param d
     * @return A new Time object
     */
    public Time minus(Duration d) {
        return subtract(d);
    }

    /**
     * Subtracting two times returns a duration
     *
     * @param t2 The time you want to subtract from the calling Time object
     * @return A new Duration object representing the length of time elapsed from the
     * parameter to the calling object
     */
    public Duration subtract(Time t2) {
        return new Duration(tics - t2.tics);
    }

    /**
     * Syntactic sugar for subtract(Time)
     *
     * @param t2
     * @return A new Duration object
     */
    public Duration minus(Time t2) {
        return subtract(t2);
    }

    /**
     * Returns a Duration that is the absolute difference between the calling object and
     * the parameter
     *
     * @param t2 The time you want to get the positive difference from
     * @return A new Duration object that is the absolute difference
     */
    public Duration absoluteDifference(Time t2) {
        return new Duration(Math.abs(tics - t2.tics));
    }

    /**
     * Returns true if the parameter is before the calling object in time
     *
     * @param t2 The second time to compare to
     * @return a boolean
     */
    public boolean lessThan(Time t2) {
        return tics < t2.tics;
    }

    /**
     * Returns true if the parameter is after the calling object in time
     *
     * @param t2 The second time to compare to
     * @return a boolean
     */
    public boolean greaterThan(Time t2) {
        return tics > t2.tics;
    }

    /**
     * Returns true if the parameter is before the calling object in time or if they
     * represent the same time
     *
     * @param t2 The second time to compare to
     * @return a boolean
     */
    public boolean lessThanOrEqualTo(Time t2) {
        return tics <= t2.tics;
    }

    /**
     * Returns true if the parameter is after the calling object in time or if they
     * represent the same time
     *
     * @param t2 The second time to compare to
     * @return a boolean
     */
    public boolean greaterThanOrEqualTo(Time t2) {
        return tics >= t2.tics;
    }

    /**
     * Returns the earliest of a list of times. Wraps Collections.min()
     *
     * @param times Time objects
     * @return A time object
     */
    public static Time min(Time... times) {
        return Collections.min(Arrays.asList(times));
    }

    /**
     * Returns the latest of a list of times. Wraps Collections.max()
     *
     * @param times Time objects
     * @return A time object
     */
    public static Time max(Time... times) {
        return Collections.max(Arrays.asList(times));
    }

    /**
     * Rounds the time to the input resolution, based on the reference time. For example,
     * with a reference time of 2019-001T00:00:00 and resolution of 00:05:00
     * 2019-001T00:33:00 would be rounded to 2019-001T00:35:00, but with a reference time
     * of 2019-001T00:00:01 the same input time would be rounded to 2019-001T00:31:00.
     *
     * @param resolution
     * @param referenceTime
     * @return
     */
    public Time round(Duration resolution, Time referenceTime) {
        Duration differenceDuration = this.subtract(referenceTime);
        return referenceTime.add(differenceDuration.round(resolution));
    }

    /**
     * Rounds the time up to the input resolution, based on the reference time. See
     * explanation in round method for reference time.
     *
     * @param resolution
     * @param referenceTime
     * @return
     */
    public Time ceil(Duration resolution, Time referenceTime) {
        Duration differenceDuration = this.subtract(referenceTime);
        return referenceTime.add(differenceDuration.ceil(resolution));
    }

    /**
     * Rounds the time down to the input resolution, based on the reference time. See
     * explanation in round method for reference time.
     *
     * @param resolution
     * @param referenceTime
     * @return
     */
    public Time floor(Duration resolution, Time referenceTime) {
        Duration differenceDuration = this.subtract(referenceTime);
        return referenceTime.add(differenceDuration.floor(resolution));
    }

    /**
     * Compares the time to another time object and returns true if t2 is within an input
     * resolution of t1.
     *
     * @param t2
     * @param resolution
     * @return
     */
    public boolean equalToWithin(Time t2, Duration resolution) {
        long lower_bound = t2.tics - resolution.getTics();
        long upper_bound = t2.tics + resolution.getTics();

        return ((tics >= lower_bound) && tics <= upper_bound);
    }
    //</editor-fold>

    //<editor-fold desc="string IO and conversion to different time systems">

    /**
     * Overrides the default toString() method so you get a default representation of the
     * object. In this case, we are using UTC format with the default number of decimal
     * places. To get other string methods, use toX() methods
     *
     * @return
     */
    @Override
    public String toString() {
        return toUTC(getDefaultOutputPrecision());
    }

    /**
     * Given a UTC time format (anything allowed by str2et), mutates the calling object to
     * represent that time Implements the convertableFromString interface so engines can
     * call valueOf on whatever object including Times.
     *
     * @param utcFormattedString
     */
    @Override
    public void valueOf(String utcFormattedString) {
        if (useSpiceForMath) {
            try {
                tics = et2tai(CSPICE.str2et(utcFormattedString));
            } catch (SpiceErrorException e) {
                throw new RuntimeException("Cannot turn String " + utcFormattedString + " into a valid time - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
            }
        } else {
            // THIS WILL NOT MATCH SPICE LEAP SECOND FILES BUT IS A GOOD APPROXIMATION, gets leap seconds from java version updates
            // this is also significantly less flexible with allowable input time formats than str2et
            LocalDateTime localDT = LocalDateTime.parse(utcFormattedString, inputUtcFormat);
            tics = java.time.Duration.between(EPOCH, localDT).toNanos() / NANOSECONDS_PER_TIC;
        }
    }

    /**
     * Returns a utc string with the default precision.
     *
     * @return
     */
    public String toUTC() {
        return toUTC(getDefaultOutputPrecision());
    }

    /**
     * Outputs a string representing a time with the given number of decimal precision
     *
     * @param precision An integer, the number of decimal places put out by the function
     * @return A string representing the time in UTC DOY format
     */
    public String toUTC(int precision) {
        if (useSpiceForMath) {
            try {
                return CSPICE.et2utc(tai2et(tics), "ISOD", precision);
            } catch (SpiceErrorException e) {
                throw new RuntimeException("Cannot turn time with TAI = " + tics + " into a string - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
            }
        } else {
            // bound decimal places between 0 and max precision supported by backing data structure
            int numDecimalPlaces = precision > numberDecimalDigits ? numberDecimalDigits : precision < 0 ? 0 : precision;

            long ticsRounder = 1;
            for (int i = 0; i < (numberDecimalDigits - numDecimalPlaces); i++) {
                ticsRounder *= 10;
            }
            long roundedTics = Math.round(((double) tics) / ticsRounder) * ticsRounder;

            // since Java DateTime only gets new leap seconds from version updates, we will not get the correct time adding across leap seconds
            LocalDateTime localDT = EPOCH.plusNanos(roundedTics * NANOSECONDS_PER_TIC);

            return localDT.format(outputUTCFormatters[numDecimalPlaces]);
        }
    }

    /**
     * Outputs a double representing the time in SPICE TAI
     *
     * @return A double, SPICE TAI of the instant of this object
     */
    public double toTAI() {
        if (useSpiceForMath) {
            return tics / (double) ONE_SECOND;
        } else {
            try {
                return et2tai(toET()) / (double) ONE_SECOND;
            } catch (SpiceErrorException e) {
                throw new RuntimeException("Cannot convert Time " + toString() + " to tai - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
            }
        }
    }

    /**
     * Creates a Time object from Spice TAI seconds
     *
     * @param TAI Spice TAI seconds
     * @return A new Time object
     */
    public static Time fromTAI(double TAI) {
        if (useSpiceForMath) {
            return new Time(Math.round(TAI * ONE_SECOND));
        } else {
            try {
                return fromET(tai2et(Math.round(TAI * ONE_SECOND)));
            } catch (SpiceErrorException e) {
                throw new RuntimeException("Cannot convert create time from ET " + TAI + " - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
            }
        }
    }

    /**
     * Converts the calling Time object to Spice ephemeris time
     *
     * @return A double of the Spice ET equivalent to the calling object
     */
    public double toET() {
        if (useSpiceForMath) {
            try {
                return tai2et(tics);
            } catch (SpiceErrorException e) {
                throw new RuntimeException("Cannot convert Time " + toString() + " to et - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
            }
        } else {
            try {
                return CSPICE.str2et(toString());
            } catch (SpiceErrorException e) {
                throw new RuntimeException("Cannot convert Time " + toString() + " to et - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
            }
        }
    }

    /**
     * Creates a Time object from Spice ET seconds
     *
     * @param ephemerisTime Spice ET seconds
     * @return A new Time object
     */
    public static Time fromET(double ephemerisTime) {
        if (useSpiceForMath) {
            try {
                return new Time(et2tai(ephemerisTime));
            } catch (SpiceErrorException e) {
                throw new RuntimeException("Cannot convert create time from ET " + ephemerisTime + " - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
            }
        } else {
            Time t = new Time();
            try {
                t.valueOf(CSPICE.et2utc(ephemerisTime, "D", numberDecimalDigits).replace(" // ", "T"));
            } catch (SpiceErrorException e) {
                throw new RuntimeException("Cannot convert create time from ET " + ephemerisTime + " - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
            }
            return t;
        }
    }

    /**
     * Outputs a string representing a time with the default number of decimal precision
     *
     * @return A string representing the time in ISOC format
     */
    public String toISOC() {
        return toISOC(getDefaultOutputPrecision());
    }

    /**
     * Outputs a string representing a time with the given number of decimal precision
     *
     * @param precision An integer, the number of decimal places put out by the function
     * @return A string representing the time in ISOC format
     */
    public String toISOC(int precision) {
        try {
            return CSPICE.et2utc(toET(), "ISOC", precision);
        } catch (SpiceErrorException e) {
            throw new RuntimeException("Cannot turn time with TAI = " + tics + " into a string - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
        }
    }

    /**
     * Outputs a string representing a time with the default number of decimal precision
     *
     * @return A string representing the time in Julian date format
     */
    public String toJulian() {
        return toJulian(getDefaultOutputPrecision());
    }

    /**
     * Outputs a string representing a time with the given number of decimal precision
     *
     * @param precision An integer, the number of decimal places put out by the function
     * @return A string representing the time in Julian date format
     */
    public String toJulian(int precision) {
        try {
            return CSPICE.et2utc(toET(), "J", precision);
        } catch (SpiceErrorException e) {
            throw new RuntimeException("Cannot turn time with TAI = " + tics + " into a string - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
        }
    }

    /**
     * Outputs a string representing a time with the default number of decimal precision
     *
     * @return A string representing the time in UTC calendar format
     */
    public String toCalendar() {
        return toCalendar(getDefaultOutputPrecision());
    }

    /**
     * Outputs a string representing a time with the given number of decimal precision
     *
     * @param precision An integer, the number of decimal places put out by the function
     * @return A string representing the time in UTC calendar format
     */
    public String toCalendar(int precision) {
        try {
            return CSPICE.et2utc(toET(), "C", precision);
        } catch (SpiceErrorException e) {
            throw new RuntimeException("Cannot turn time with TAI = " + tics + " into a string - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
        }
    }

    /**
     * Returns whether or not the UTC time is before noon as an AM_PM enum object.
     *
     * @return
     */
    public AM_PM toUtcAmPm() {
        String utcString = toUTC(getDefaultOutputPrecision());
        Matcher utcMatcher = TIME_PATTERN.matcher(utcString);
        if (utcMatcher.find()) {
            if (Integer.valueOf(utcMatcher.group("hours")) < 12) {
                return AM_PM.AM;
            } else {
                return AM_PM.PM;
            }
        } else {
            throw new RuntimeException("Error parsing output of toUTC() " + utcString + " for time with TAI: " + tics);
        }
    }

    /**
     * Outputs the SCLK string that is equivalent to the calling object. Uses Spice sce2s
     * with the default spacecraft id
     *
     * @return This time's SCLK string
     */
    public String toSCLK() {
        return toSCLK(getDefaultSpacecraftId());
    }

    /**
     * Outputs the SCLK string that is equivalent to the calling object. Uses Spice sce2s
     *
     * @param sc_id The NAIF ID of the spacecraft whose SCLK it is. This is typically a
     *              negative 3 digit number.
     * @return This time's SCLK string
     */
    public String toSCLK(int sc_id) {
        try {
            return CSPICE.sce2s(sc_id, toET());
        } catch (SpiceErrorException e) {
            throw new RuntimeException("Cannot convert time with TAI = " + tics + " to SCLK - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
        }
    }

    /**
     * Returns a Time object that is equivalent to a given SCLK string. Uses default
     * spacecraft id.
     *
     * @param sclk_string The SCLK string itself - must be in a format that Spice scs2e
     *                    can understand
     * @return A new Time object
     */
    public static Time fromSCLK(String sclk_string) {
        return fromSCLK(sclk_string, getDefaultSpacecraftId());
    }

    /**
     * Returns a Time object that is equivalent to a given SCLK string
     *
     * @param sclk_string The SCLK string itself - must be in a format that Spice scs2e
     *                    can understand
     * @param sc_id       The NAIF ID of the spacecraft whose SCLK it is. This is
     *                    typically a negative 3 digit number.
     * @return A new Time object
     */
    public static Time fromSCLK(String sclk_string, int sc_id) {
        try {
            return Time.fromET(CSPICE.scs2e(sc_id, sclk_string));
        } catch (SpiceErrorException e) {
            throw new RuntimeException("Could not convert SCLK string " + sclk_string + " for spacecraft " + sc_id + " into a Time object, for more info see:\n" + e.getMessage());
        }
    }

    /**
     * Outputs the SCLK decimal that is equivalent to the calling object. Uses sce2s then
     * converts to a decimal. Automatically figures out the SCLK sub-second modulus by
     * calling gdpool on the SCLK kernel. Uses default spacecraft id.
     *
     * @return This time's SCLK decimal
     */
    public double toSCLKD() {
        return toSCLKD(getDefaultSpacecraftId());
    }

    /**
     * Outputs the SCLK decimal that is equivalent to the calling object. Uses sce2s then
     * converts to a decimal. Automatically figures out the SCLK sub-second modulus by
     * calling gdpool on the SCLK kernel
     *
     * @param sc_id The NAIF ID of the spacecraft whose SCLK it is. This is typically a
     *              negative 3 digit number.
     * @return This time's SCLK decimal
     */
    public double toSCLKD(int sc_id) {
        String sclk_string = toSCLK(sc_id);
        Matcher sclkMatcher = SCLK_PATTERN.matcher(sclk_string);
        if (sclkMatcher.find()) {
            double subseconds = Integer.valueOf(sclkMatcher.group("fraction")) / getSclkFractionalPart(sc_id);
            return Integer.valueOf(sclkMatcher.group("seconds")) + subseconds;
        } else {
            throw new RuntimeException("Error parsing SCLK string: " + sclk_string);
        }
    }

    /**
     * Creates a time object given a SCLK decimal. Uses default spacecraft id.
     *
     * @param sclkd The SCLK decimal itself - a float number of seconds
     * @return A new Time object
     */
    public static Time fromSCLKD(double sclkd) {
        return fromSCLKD(sclkd, getDefaultSpacecraftId());
    }


    /**
     * Creates a time object given a SCLK decimal and a SC NAIF id
     *
     * @param sclkd The SCLK decimal itself - a float number of seconds
     * @param sc_id The NAIF ID of the spacecraft whose SCLK it is. This is typically a
     *              negative 3 digit number.
     * @return A new Time object
     */
    public static Time fromSCLKD(double sclkd, int sc_id) {
        // we need the %f formatter to make sure to get the full decimal as expected, not scientific notation
        Matcher sclkdMatcher = SCLKD_PATTERN.matcher(String.format("%f", sclkd));
        if (sclkdMatcher.find()) {
            long seconds = Long.valueOf(sclkdMatcher.group("seconds"));
            double subseconds = Double.valueOf("0." + sclkdMatcher.group("subsec"));
            long correctedSubseconds = Math.round(subseconds * getSclkFractionalPart(sc_id));
            String sclk_string = String.format("1/%s-%s", seconds, correctedSubseconds);
            return fromSCLK(sclk_string, sc_id);
        } else {
            throw new RuntimeException("Cannot convert SCLKD " + sclkd + " to a Time object - it does not fit the expected format of SSSSSS.ssss (any number of each digits for each field are acceptable)");
        }
    }

    /**
     * Checks if the SCLK fractional part has already been calculated before returning or
     * calculating the correct value.
     *
     * @param sc_id
     * @return
     */
    private static double getSclkFractionalPart(int sc_id) {
        if (SCLK_FRACTIONAL_PART_MAP.containsKey(sc_id)) {
            return SCLK_FRACTIONAL_PART_MAP.get(sc_id);
        } else {
            try {
                String quantityNeeded = "SCLK01_MODULI_" + Math.abs(sc_id);
                double[] moduli = CSPICE.gdpool(quantityNeeded, 0, 2);
                SCLK_FRACTIONAL_PART_MAP.put(sc_id, moduli[1]);
                return moduli[1];
            } catch (SpiceErrorException | KernelVarNotFoundException e) {
                throw new RuntimeException("Could not find the SCLK fractional part - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
            }
        }
    }

    /**
     * Wraps toLMST but uses default spacecraft id and precision
     *
     * @return LMST string
     */
    public String toLMST() {
        return toLMST(getDefaultSpacecraftId(), getDefaultOutputPrecision());
    }

    /**
     * Wraps toLMST but uses default spacecraft id.
     *
     * @param precision precision for output string
     * @return LMST string
     */
    public String toLMST(int precision) {
        return toLMST(getDefaultSpacecraftId(), precision);
    }

    /**
     * Outputs a string that represents this instant in Local Mean Solar Time (LMST) for
     * the SC specified
     *
     * @param sc_id     The NAIF ID of the spacecraft. This is typically a negative 3
     *                  digit number. To get the separate LMST SCLK ID/landing site ID,
     *                  this routine multiplies by -1 then appends '900', which is the
     *                  NAIF unofficial standard
     * @param precision The number of decimal places that should be included in the
     *                  output
     * @return The calling object represented as an LMST string
     */
    public String toLMST(int sc_id, int precision) {
        int LMST_ID = (sc_id * 1000) - 900;
        try {
            return reformatSPICELMST(CSPICE.sce2s(LMST_ID, toET()), precision);
        } catch (SpiceErrorException e) {
            // check to make sure that they are not failing because the time is before sol 0
            Time lmstSol0 = Time.fromLMST("Sol-0000M00:00:00", sc_id);
            if (tics < lmstSol0.tics) {
                throw new RuntimeException("Error converting " + toUTC() + " to LMST. Time is before LMST epoch " + lmstSol0.toUTC() + ".");
            }

            // if not then throw the spice error which is probably from kernels missing
            throw new RuntimeException("Cannot convert time with TAI = " + tics + " to LMST - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
        }
    }

    /**
     * Returns a Time object equivalent to the input LMST string. Uses default spacecraft
     * id.
     *
     * @param lmst The LMST string - many different formats are accepted (the standard is
     *             Sol-####M##:##:##.###)
     * @return A new Time object
     */
    public static Time fromLMST(String lmst) {
        return fromLMST(lmst, getDefaultSpacecraftId());
    }

    /**
     * Returns a Time object equivalent to the input LMST string
     *
     * @param lmst  The LMST string - many different formats are accepted (the standard is
     *              Sol-####M##:##:##.###)
     * @param sc_id The NAIF ID of the spacecraft. This is typically a negative 3 digit
     *              number. To get the separate LMST SCLK ID/landing site ID, this routine
     *              multiplies by -1 then appends '900', which is the NAIF unofficial
     *              standard
     * @return A new Time object
     */
    public static Time fromLMST(String lmst, int sc_id) {
        Matcher lmstMatcher = LMST_STANDARD_REGEX_PATTERN.matcher(lmst);
        int LMST_ID = (sc_id * 1000) - 900;

        if (lmstMatcher.find()) {
            // the decimal in SPICE is actually a number of "ticks" similar to SCLK
            Double decimalPart;
            if (lmstMatcher.group("decimal") != null && !lmstMatcher.group("decimal").equals("")) {
                decimalPart = Double.valueOf("0." + lmstMatcher.group("decimal"));
            } else {
                decimalPart = 0.0;
            }

            // convert the decimal to a fraction of the LMST modulus
            String correctedDecimal = String.valueOf(Math.round(decimalPart * LMST_TICS_PER_SECOND));

            // SPICE LMST is formatted differently than typical mars missions expect
            String spiceLMST = String.format("%s:%s:%s:%s:%s", lmstMatcher.group("sol"), lmstMatcher.group("hours"), lmstMatcher.group("minutes"), lmstMatcher.group("seconds"), correctedDecimal);

            Double et;
            try {
                et = CSPICE.scs2e(LMST_ID, spiceLMST);
            } catch (SpiceErrorException e) {
                throw new RuntimeException("Could not convert " + spiceLMST + " to et using SPICE: " + e.getMessage());
            }

            return Time.fromET(et);
        } else {
            throw new RuntimeException("Error parsing LMST string: " + lmst + ". String did not match expected regex: " + LMST_STANDARD_REGEX);
        }
    }

    /**
     * Returns whether or not the LMST time is before noon as an AM_PM enum object. Uses
     * default spacecraft id.
     *
     * @return
     */
    public AM_PM toLmstAmPm() {
        return toLmstAmPm(getDefaultSpacecraftId());
    }

    /**
     * Returns whether or not the LMST time is before noon as an AM_PM enum object.
     *
     * @param sc_id
     * @return
     */
    public AM_PM toLmstAmPm(int sc_id) {
        String lmstString = toLMST(sc_id, getDefaultOutputPrecision());
        Matcher lmstMatcher = LMST_STANDARD_REGEX_PATTERN.matcher(lmstString);
        if (lmstMatcher.find()) {
            if (Integer.valueOf(lmstMatcher.group("hours")) < 12) {
                return AM_PM.AM;
            } else {
                return AM_PM.PM;
            }
        } else {
            throw new RuntimeException("Error parsing output of toLMST() " + lmstString + " for time with TAI: " + tics);
        }
    }

    /**
     * The integer sol number at the calling object Time. First calls toLMST() then uses
     * regex to get the sol number. Uses default spacecraft id
     *
     * @return An integer of the sol number at the Time queried
     */
    public int toSolNumber() {
        return toSolNumber(getDefaultSpacecraftId());
    }

    /**
     * The integer sol number at the calling object Time. First calls toLMST() then uses
     * regex to get the sol number.
     *
     * @param sc_id The NAIF ID of the spacecraft. This is typically a negative 3 digit
     *              number. To get the separate LMST SCLK ID/landing site ID, this routine
     *              multiplies by -1 then appends '900', which is the NAIF unofficial
     *              standard
     * @return An integer of the sol number at the Time queried
     */
    public int toSolNumber(int sc_id) {
        String LMST = toLMST(sc_id, getDefaultOutputPrecision());
        Matcher lmstMatcher = LMST_STANDARD_REGEX_PATTERN.matcher(LMST);
        String solNum;
        if (lmstMatcher.find()) {
            solNum = lmstMatcher.group("sol");
        } else {
            throw new RuntimeException("Error parsing output of toLMST() for time with TAI: " + tics);
        }

        return Integer.valueOf(solNum);
    }

    /**
     * The calling object Time expressed as a sol-fraction since the LMST epoch. For
     * example, Sol-0026M12:00:00 is 26.5. First calls LMST() then converts HH:MM:SS to
     * decimal. Uses default spacecraft id.
     *
     * @return A double of the fractional sols elapsed since the LMST epoch
     */
    public double toFractionalSols() {
        return toFractionalSols(getDefaultSpacecraftId());
    }

    /**
     * The calling object Time expressed as a sol-fraction since the LMST epoch. For
     * example, Sol-0026M12:00:00 is 26.5. First calls LMST() then converts HH:MM:SS to
     * decimal
     *
     * @param sc_id The NAIF ID of the spacecraft. This is typically a negative 3 digit
     *              number. To get the separate LMST SCLK ID/landing site ID, this routine
     *              multiplies by -1 then appends '900', which is the NAIF unofficial
     *              standard
     * @return A double of the fractional sols elapsed since the LMST epoch
     */
    public double toFractionalSols(int sc_id) {
        String LMST = toLMST(sc_id, getDefaultOutputPrecision());
        Matcher lmstMatcher = LMST_STANDARD_REGEX_PATTERN.matcher(LMST);
        String solNum;
        String timeOfSol;
        if (lmstMatcher.find()) {
            solNum = lmstMatcher.group("sol");
            timeOfSol = lmstMatcher.group("timeOfDay");
        } else {
            throw new RuntimeException("Error parsing output of toLMST() for time with TAI: " + tics);
        }

        return Integer.valueOf(solNum) + (new Duration(timeOfSol).getTics() / (double) ONE_DAY);
    }

    /**
     * Wraps toLST but uses default spacecraft id, body id and frame.
     *
     * @return LST duration (time of day)
     */
    public Duration toLST() {
        return toLST(getDefaultSpacecraftId());
    }

    /**
     * Wraps toLST but uses default body id and frame.
     *
     * @param sc_id naif id of spacecraft
     * @return LST duration (time of day)
     */
    public Duration toLST(int sc_id) {
        return toLST(sc_id, getDefaultLstBodyId(), getDefaultLstBodyFrame());
    }

    /**
     * Gets the local solar time at a SC's location given that body ID and body frame.
     * This returns just a Duration between 0 and 24 hours, since with just this
     * information and no epoch it is impossible to have a true time system. To get the
     * hours, minutes, seconds from the resulting duration object, you can call
     * .toHours(), toMinutes()%60, and toSeconds%60
     *
     * @param sc_id      The SC at whose position you want to know the local solar time
     * @param body_id    The NAIF ID of the body that the spacecraft is on or orbiting
     * @param body_frame The NAIF frame string for the body the spacecraft is on or
     *                   orbiting
     * @return A duration object between 0 and 24 hours in magnitude that contains the
     * local solar time of the point on the given body
     */
    public Duration toLST(int sc_id, int body_id, String body_frame) {
        // variables needed to call spkez
        double[] state = new double[6];
        double[] lt = new double[1];
        double longitude_radians;

        // variables needed to call et2lst
        int[] hr = new int[1];
        int[] min = new int[1];
        int[] sec = new int[1];
        String[] time = new String[1];
        String[] ampm = new String[1];

        try {
            // position vector in xyz -> latlonrad -> et2lst takes a longitude
            CSPICE.spkezr(String.valueOf(sc_id), toET(), body_frame, "None", String.valueOf(body_id), state, lt);
            longitude_radians = CSPICE.reclat(Arrays.copyOfRange(state, 0, 3))[1];
            CSPICE.et2lst(toET(), body_id, longitude_radians, "PLANETOCENTRIC", hr, min, sec, time, ampm);
        } catch (SpiceErrorException e) {
            throw new RuntimeException("Cannot convert time with TAI = " + tics + " to LST - SPICE must be loaded to do this or there may be another error:\n" + e.getMessage());
        }

        return new Duration(String.format("%02d:%02d:%02d", hr[0], min[0], sec[0]));
    }

    /**
     * Wraps toLTST but uses default spacecraft id, body id and body frame.
     *
     * @return LTST string
     */
    public String toLTST() {
        return toLTST(getDefaultSpacecraftId(), getDefaultLstBodyId(), getDefaultLstBodyFrame());
    }

    /**
     * Wraps toLTST but uses default body id and frame.
     *
     * @param sc_id naif spacecraft id
     * @return LTST string
     */
    public String toLTST(int sc_id) {
        return toLTST(sc_id, getDefaultLstBodyId(), getDefaultLstBodyFrame());
    }

    /**
     * Calculates the LMST-assisted Local True Solar Time (LTST) value for the calling
     * Time object. What this means is that this routine first calls toLST() to get the
     * time of day, then infers the sol number from LMST (correcting if you need to add or
     * subtract one sol over the midnight boundary). This means you have to have all the
     * LMST kernels loaded to call this.
     *
     * @param sc_id      The SC at whose position you want to know the local true solar
     *                   time
     * @param body_id    The NAIF ID of the body that the spacecraft is on or orbiting
     * @param body_frame The NAIF frame string for the body the spacecraft is on or
     *                   orbiting
     * @return The calling object represented as an LTST string
     */
    public String toLTST(int sc_id, int body_id, String body_frame) {
        //the LST string doesn't have a sol number associated with it, so calculate it using LMST
        String LMST = toLMST(sc_id, getDefaultOutputPrecision());
        Matcher lmstMatcher = LMST_STANDARD_REGEX_PATTERN.matcher(LMST);
        String lmstTOD = "";
        if (lmstMatcher.find()) {
            lmstTOD = lmstMatcher.group("timeOfDay");
        }

        int lmst_sols = Integer.valueOf(lmstMatcher.group("sol"));
        int ltst_sols;

        Duration LMSTTimeOfDay = new Duration(lmstTOD);

        Duration LSTTimeOfDay = toLST(sc_id, body_id, body_frame);

        // assume that LMST is within 12 hours of LTST
        Duration offset = new Duration("12:00:00");

        if (LMSTTimeOfDay.greaterThan(LSTTimeOfDay.add(offset))) {
            ltst_sols = lmst_sols + 1;
        } else if (LSTTimeOfDay.greaterThan(LMSTTimeOfDay.add(offset))) {
            ltst_sols = lmst_sols - 1;
        } else {
            ltst_sols = lmst_sols;
        }

        return String.format("Sol-%04dT%02d:%02d:%02d", ltst_sols, LSTTimeOfDay.getHours(), LSTTimeOfDay.getMinutes() % 60, LSTTimeOfDay.getSeconds() % 60);

    }

    /**
     * Returns a ZonedDateTime that is equivalent to the calling Time object. This is
     * useful because ZonedDateTime has a lot of nice utility methods like DayOfWeek,
     * getHours, etc etc that we don't have to wrap in this class
     *
     * @param timezone The timezone string you want to convert the time into. It must be a
     *                 string acceptable to the ZoneId.of() method
     * @return A ZonedDateTime object representing the same time in a different timezone
     */
    public ZonedDateTime toTimezone(String timezone) {
        ZonedDateTime current_instant = ZonedDateTime.of(LocalDateTime.parse(toString(), inputUtcFormat), ZoneOffset.UTC);
        return current_instant.withZoneSameInstant(ZoneId.of(timezone));
    }

    /**
     * Wraps toTimezoneString but uses default precision.
     *
     * @param timezone The timezone string you want to represent the instant in. It must
     *                 be a string acceptable to the ZoneId.of() method
     * @return A string that contains the time in a different timezone
     */
    public String toTimezoneString(String timezone) {
        return new Time(toTimezone(timezone).format(outputUTCFormatters[getDefaultOutputPrecision()])).toUTC(getDefaultOutputPrecision());
    }

    /**
     * Returns a string representing the calling object instant moved to a different
     * timezone
     *
     * @param timezone  The timezone string you want to represent the instant in. It must
     *                  be a string acceptable to the ZoneId.of() method
     * @param precision decimal precision to use for outputting the string
     * @return A string that contains the time in a different timezone
     */
    public String toTimezoneString(String timezone, int precision) {
        return new Time(toTimezone(timezone).format(outputUTCFormatters[getDefaultOutputPrecision()])).toUTC(precision);
    }

    /**
     * Returns a new Time object given a local Time and the timezone that Time is in -
     * converts to UTC, then feeds that String to standard Time constructor
     *
     * @param timeString A local time, in the YYYY-DDDTHH:MM:SS.ssssss format
     * @param timezone   The timezone string you want to convert the time into. It must be
     *                   a string acceptable to the ZoneId.of() method
     * @return A new Time object
     */
    public static Time fromTimezoneString(String timeString, String timezone) {
        ZonedDateTime current_instant = ZonedDateTime.of(LocalDateTime.parse(timeString, inputUtcFormat), ZoneId.of(timezone));
        return new Time(current_instant.withZoneSameInstant(ZoneOffset.UTC).format(outputUTCFormatters[getDefaultOutputPrecision()]));
    }

    /**
     * Returns a new Time object given a Java ZonedDateTime - converts to UTC, then feeds
     * that String to standard Time constructor
     *
     * @param zdt The input ZonedDateTime
     * @return A new Time object
     */
    public static Time fromTimezone(ZonedDateTime zdt) {
        return new Time(zdt.withZoneSameInstant(ZoneOffset.UTC).format(outputUTCFormatters[getDefaultOutputPrecision()]));
    }

    /**
     * @return A double of the GPS seconds equivalent to the calling object
     */
    public double toGPSSeconds() {
        return toTAI() - getGPSEpoch().toTAI();
    }

    /**
     * @return A Time equivalent to the input gps time as seconds
     */
    public static Time fromGPSSeconds(double gpsSeconds) {
        return Time.fromTAI(getGPSEpoch().toTAI() + gpsSeconds);
    }

    /**
     * @return The time represented as a GPS string
     */
    public String toGPS(int precision) {
        double gpsSeconds = toGPSSeconds();
        long gpsSecondsOnly = (long) Math.floor(gpsSeconds);
        double gpsSubseconds = gpsSeconds - gpsSecondsOnly;

        Instant currentTime = Instant.ofEpochSecond(LocalDateTime.parse(getGPSEpoch().toString(), inputUtcFormat).toInstant(ZoneOffset.UTC).getEpochSecond() + gpsSecondsOnly).plusNanos(Math.round(gpsSubseconds * 1000000000L));

        return Time.fromTimezone(currentTime.atZone(ZoneOffset.UTC)).toUTC(precision);
    }

    /**
     * @param gpsString A time string in the GPS time system
     * @return A time object equivalent to the input string
     */
    public static Time fromGPS(String gpsString) {
        Matcher gpsMatcher = TIME_PATTERN.matcher(gpsString);
        double subseconds = 0;
        if (gpsMatcher.find()) {
            subseconds = Double.valueOf("0." + gpsMatcher.group("subseconds"));
        }

        return Time.fromTAI(getGPSEpoch().toTAI() + (LocalDateTime.parse(gpsString, inputUtcFormat).toInstant(ZoneOffset.UTC).getEpochSecond() - LocalDateTime.parse(getGPSEpoch().toString(), inputUtcFormat).toInstant(ZoneOffset.UTC).getEpochSecond()) + subseconds);
    }

    /**
     * For XMLTOL use only - in UTC seconds since 1970 since that is the spec
     *
     * @return A string whose contents are milliseconds since Unix epoch
     */
    public String getMilliseconds() {
        return String.valueOf(((tics - getUnixEpoch().tics) / ONE_MILLISECOND));
    }

    private static double tai2et(long tai) throws SpiceErrorException {
        return CSPICE.unitim(tai / (double) ONE_SECOND, "TAI", "ET");
    }

    private static long et2tai(double et) throws SpiceErrorException {
        return Math.round(CSPICE.unitim(et, "ET", "TAI") * ONE_SECOND);
    }

    private static String reformatSPICELMST(String SPICELMST, int precision) {
        Matcher spiceLMSTMatcher = LMST_SPICE_REGEX_PATTERN.matcher(SPICELMST);
        if (spiceLMSTMatcher.find()) {
            int sol_number = Integer.valueOf(spiceLMSTMatcher.group("sol"));
            Duration mars_duration = new Duration(String.format("%s:%s:%s.%s",
                    spiceLMSTMatcher.group("hour"),
                    spiceLMSTMatcher.group("min"),
                    spiceLMSTMatcher.group("sec"),
                    spiceLMSTMatcher.group("subsec")));

            // we round the mars duration using the same function as rounding the earth duration
            String roundedDuration = mars_duration.toString(precision);
            Matcher durationMatcher = durationPattern.matcher(roundedDuration);

            if (durationMatcher.find()) {
                if (durationMatcher.group("days") != null) {
                    sol_number += Integer.valueOf(durationMatcher.group("days"));
                }

                return String.format("Sol-%04dM%s", sol_number, durationMatcher.group("timeOfDay"));
            } else {
                throw new RuntimeException("Could not convert SPICE LMST output: " + SPICELMST + " to normal formatted LMST");
            }
        } else {
            throw new RuntimeException("Error parsing SPICE LMST string: " + SPICELMST);
        }
    }

    /**
     * Return the current system time as a Time object.
     *
     * @return
     */
    public static Time currentSystemTime() {
        return Time.fromTimezone(ZonedDateTime.now(ZoneOffset.UTC));
    }

    //</editor-fold>

    //<editor-fold desc="time conversion between different frames using light time">

    // ett to scet

    /**
     * Returns the input earth transmit time + the upleg time to the spacecraft. Uses
     * default spacecraft id.
     *
     * @param ETT A Time object in ETT
     * @return A new Time object shifted by OWLT
     */
    public static Time ETT2SCET(Time ETT) {
        return ETT2SCET(ETT, getDefaultSpacecraftId());
    }

    /**
     * Returns the input earth transmit time + the upleg time to the spacecraft.
     *
     * @param ETT   A Time object in ETT
     * @param sc_id The NAIF id of the spacecraft for which you want the SpaceCraft Event
     *              Time
     * @return A new Time object shifted by OWLT
     */
    public static Time ETT2SCET(Time ETT, int sc_id) {
        return ETT.add(upleg(ETT, sc_id, EARTH_NAIF_ID, "ETT"));
    }

    // ett to ert

    /**
     * Returns the input earth transmit time + the upleg time to the spacecraft + the
     * downleg time from the spacecraft when the signal arrives at the spacecraft. Uses
     * default spacecraft id.
     *
     * @param ETT A Time object in ETT
     * @return A new Time object shifted by RTLT
     */
    public static Time ETT2ERT(Time ETT) {
        return ETT2ERT(ETT, getDefaultSpacecraftId());
    }

    /**
     * Returns the input earth transmit time + the upleg time to the spacecraft + the
     * downleg time from the spacecraft when the signal arrives at the spacecraft.
     *
     * @param ETT   A Time object in ETT
     * @param sc_id The NAIF id of the spacecraft that light is traveling to and from
     * @return A new Time object shifted by RTLT
     */
    public static Time ETT2ERT(Time ETT, int sc_id) {
        return SCET2ERT(ETT2SCET(ETT, sc_id), sc_id);
    }

    // ert to ett

    /**
     * Returns the input earth receive time - the downleg time from the spacecraft - the
     * upleg time to the spacecraft when the signal was sent. Uses default spacecraft id
     *
     * @param ERT A Time object in ERT
     * @return A new Time objected shifted by RTLT
     */
    public static Time ERT2ETT(Time ERT) {
        return ERT2ETT(ERT, getDefaultSpacecraftId());
    }

    /**
     * Returns the input earth receive time - the downleg time from the spacecraft - the
     * upleg time to the spacecraft when the signal was sent.
     *
     * @param ERT   A Time object in ERT
     * @param sc_id The NAIF id of the spacecraft that light is traveling to and from
     * @return A new Time objected shifted by RTLT
     */
    public static Time ERT2ETT(Time ERT, int sc_id) {
        return SCET2ETT(ERT2SCET(ERT, sc_id), sc_id);
    }

    // scet to ert

    /**
     * Returns the input spacecraft event time + the downleg time from the spacecraft.
     * Uses default spacecraft id and earth naif id.
     *
     * @param SCET A Time object in SCET
     * @return A new Time object shifted by OWLT
     */
    public static Time SCET2ERT(Time SCET) {
        return SCET2ERT(SCET, getDefaultSpacecraftId());
    }

    /**
     * Returns the input spacecraft event time + the downleg time from the spacecraft.
     *
     * @param SCET  A Time object in SCET
     * @param sc_id The Naif id of the spacecraft that the SCET is tied to
     * @return A new Time object shifted by OWLT
     */
    public static Time SCET2ERT(Time SCET, int sc_id) {
        return SCET.add(downleg(SCET, sc_id, EARTH_NAIF_ID, "SCET"));
    }

    // scet to ett

    /**
     * Returns the input spacecraft time - the upleg time to the spacecraft. Uses default
     * spacecraft id.
     *
     * @param SCET A Time object in SCET
     * @return A new Time object shifted by OWLT
     */
    public static Time SCET2ETT(Time SCET) {
        return SCET2ETT(SCET, getDefaultSpacecraftId());
    }

    /**
     * Returns the input spacecraft time - the upleg time to the spacecraft.
     *
     * @param SCET  A Time object in SCET
     * @param sc_id The Naif id of the spacecraft that the SCET is tied to
     * @return A new Time object shifted by OWLT
     */
    public static Time SCET2ETT(Time SCET, int sc_id) {
        return SCET.subtract(upleg(SCET, sc_id, EARTH_NAIF_ID, "SCET"));
    }

    // ert to scet

    /**
     * Returns the input spacecraft event time - upleg time to the spacecraft. Uses
     * default spacecraft id.
     *
     * @param ERT A Time object in ERT
     * @return A new Time objected shifted by OWLT
     */
    public static Time ERT2SCET(Time ERT) {
        return ERT2SCET(ERT, getDefaultSpacecraftId());
    }

    /**
     * Returns the input spacecraft event time - upleg time to the spacecraft.
     *
     * @param ERT   A Time object in ERT
     * @param sc_id The NAIF id of the spacecraft for which you want the SpaceCraft Event
     *              Time
     * @return A new Time objected shifted by OWLT
     */
    public static Time ERT2SCET(Time ERT, int sc_id) {
        return ERT.subtract(downleg(ERT, sc_id, EARTH_NAIF_ID, "ERT"));
    }

    // upleg

    /**
     * Wraps upleg(t, sc_id, body_id), but always passes body_id = EARTH_NAIF_ID (Earth)
     * Uses default spacecraft id
     *
     * @param t
     * @return
     */
    public static Duration upleg(Time t) {
        return upleg(t, getDefaultSpacecraftId(), EARTH_NAIF_ID);
    }

    /**
     * Wraps upleg(t, sc_id, body_id), but always passes body_id = EARTH_NAIF_ID (Earth)
     *
     * @param t
     * @param sc_id
     * @return
     */
    public static Duration upleg(Time t, int sc_id) {
        return upleg(t, sc_id, EARTH_NAIF_ID);
    }

    /**
     * Wraps upleg(t, sc_id, body_id, time_reference) but always passes in "SCET" as time
     * reference
     *
     * @param t
     * @param sc_id
     * @param body_id
     * @return
     */
    public static Duration upleg(Time t, int sc_id, int body_id) {
        return upleg(t, sc_id, body_id, "SCET");
    }

    /**
     * Gets the upleg duration to a spacecraft represented by sc_id from a body
     * represented by body_id at the specified Time t. The Spice calculation of the
     * duration is different depending on your time_reference frame, though they are
     * usually very close.
     *
     * @param t
     * @param sc_id
     * @param body_id
     * @param time_reference
     * @return A new Duration object
     */
    public static Duration upleg(Time t, int sc_id, int body_id, String time_reference) {
        if (time_reference.equals("SCET")) {
            return SECOND_DURATION.multiply(getLightTime(t.toET(), sc_id, "<-", body_id));
        } else if (time_reference.equals("ETT") || time_reference.equals("ERT")) {
            return SECOND_DURATION.multiply(getLightTime(t.toET(), body_id, "->", sc_id));
        } else {
            throw new RuntimeException("Error calculating upleg with input time_reference " + time_reference + ". This value must be either SCET, ERT, or ETT.");
        }
    }

    // downleg

    /**
     * Wraps downleg(t, sc_id, body_id), but always passes body_id = EARTH_NAIF_ID (Earth)
     * Uses default spacecraft id.
     *
     * @param t
     * @return
     */
    public static Duration downleg(Time t) {
        return downleg(t, getDefaultSpacecraftId(), EARTH_NAIF_ID);
    }

    /**
     * Wraps downleg(t, sc_id, body_id), but always passes body_id = EARTH_NAIF_ID
     * (Earth)
     *
     * @param t
     * @param sc_id
     * @return
     */
    public static Duration downleg(Time t, int sc_id) {
        return downleg(t, sc_id, EARTH_NAIF_ID);
    }

    /**
     * Wraps downleg(t, sc_id, body_id, time_reference) but always passes in "SCET" as
     * time reference
     *
     * @param t
     * @param sc_id
     * @param body_id
     * @return
     */
    public static Duration downleg(Time t, int sc_id, int body_id) {
        return downleg(t, sc_id, body_id, "SCET");
    }

    /**
     * Gets the downleg duration from a spacecraft represented by sc_id to a body
     * represented by body_id at the specified Time t. The Spice calculation of the
     * duration is different depending on your time_reference frame, though they are
     * usually very close.
     *
     * @param t
     * @param sc_id
     * @param body_id
     * @param time_reference
     * @return A new Duration object
     */
    public static Duration downleg(Time t, int sc_id, int body_id, String time_reference) {
        if (time_reference.equals("SCET")) {
            return owlt(t, sc_id, "->", body_id);
        } else if (time_reference.equals("ETT") || time_reference.equals("ERT")) {
            return owlt(t, body_id, "<-", sc_id);
        } else {
            throw new RuntimeException("Error calculating downleg with input time_reference " + time_reference + ". This value must be either SCET, ERT, or ETT.");
        }
    }

    /**
     * Returns the OWLT as a duration object.
     *
     * @param time
     * @param observerID
     * @param arrow      "->" or "<-"
     * @param targetID
     * @return
     */
    public static Duration owlt(Time time, int observerID, String arrow, int targetID) {
        return Duration.fromSeconds(getLightTime(time.toET(), observerID, arrow, targetID));
    }

    /**
     * Returns the RTLT as a duration object.
     *
     * @param time
     * @param observerID
     * @param arrow
     * @param targetID
     * @param addOwlt    True = add OWLT, False = subtract OWLT
     * @return
     */
    public static Duration rtlt(Time time, int observerID, String arrow, int targetID, boolean addOwlt) {
        Duration owlt = owlt(time, observerID, arrow, targetID);

        // we don't have enough info with these arguments to know whether we are supposed to be adding or
        // subtracting the OWLT from the input time, so check the boolean
        Time t2;
        if (addOwlt) {
            t2 = time.add(owlt);
        } else {
            t2 = time.subtract(owlt);
        }

        // we need the other direction when going back
        String oppositeArrow = "->";
        if (arrow.equals("->")) {
            oppositeArrow = "<-";
        }

        return owlt.add(owlt(t2, observerID, oppositeArrow, targetID));
    }

    // returns one-way light time in seconds - basically just wraps CSPICE.ltime()
    private static double getLightTime(double et, int observerID, String arrow, int targetID) {
        double[] ettarg = new double[1];
        double[] elapsd = new double[1];
        try {
            CSPICE.ltime(et, observerID, arrow, targetID, ettarg, elapsd);
        } catch (SpiceErrorException e) {
            throw new RuntimeException("Error getting lighttime to " + targetID + " from " + observerID + " at et: " + et + " .\n SPICE needs to be initialized and have the proper kernels to perform calculations. See full information:\n" + e.getMessage());
        }
        return elapsd[0];
    }
    //</editor-fold>

    //<editor-fold desc="methods specified in the java Object standard so built-in data structures work well with them">
    @Override
    public int compareTo(Time t2) {
        return Long.valueOf(tics).compareTo(t2.tics);
    }

    @Override
    public boolean equals(Object t2) {
        if (!(t2 instanceof Time)) {
            return false;
        } else {
            return tics == ((Time) t2).tics;
        }
    }

    @Override
    public int hashCode() {
        return (int) tics;
    }
    //</editor-fold>
}

