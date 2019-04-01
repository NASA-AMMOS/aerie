package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.ConvertableFromString;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Time implements Comparable<Time>, ConvertableFromString {
    // this represents milliseconds since beginning of epoch
    private long ms;

    // we'll be using this to do things like writing to UTC until we have native SPICE integration
    private LocalDateTime dt;

    // this represents the epoch - we'll build dts by adding seconds to this
    private static LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    // this is the UTC formatter we'll be using for now
    private static DateTimeFormatter utcFormat = DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss[.SSS]");

    public Time(String t) {
        valueOf(t);
    }

    public Time(long ms) {
        this.ms = ms;
        dt = EPOCH.plusNanos(ms * 1000000);
    }

    /**
     * This is the empty time constructor, which will use 1 ms for the time input.
     */
    public Time() {
        this.ms = 1;
        dt = EPOCH.plusNanos(ms * 1000000);
    }

    // adding a time and duration returns a time
    public Time add(Duration d) {
        return new Time(ms + d.getMS());
    }

    // subtracting a duration from a Time returns another Time
    public Time subtract(Duration d) {
        return new Time(ms - d.getMS());
    }

    // subtracting two times returns a duration
    public Duration subtract(Time t2) {
        return new Duration(ms - t2.ms);
    }

    public Duration absoluteDifference(Time t2){
        return new Duration(Math.abs(ms-t2.ms));
    }

    public double toET() {
        double[] et = new double[1];
        String[] errorMessage = new String[1];
        try {
            CSPICE.tparse(toUTC(), et, errorMessage);
        }
        catch (SpiceErrorException e) {
            return 0;
        }
        return et[0];
    }

    public static Time fromET(double ephemerisTime) throws SpiceErrorException {
        return new Time(CSPICE.et2utc(ephemerisTime, "ISOD", 3));
    }

    //TO-UPDATE
    public String toUTC() {
        return dt.format(utcFormat);
    }

    //TO-UPDATE
    public String toString() {
        return toUTC();
    }

    // for XMLTOL use only
    public String getMS() {
        return String.valueOf(ms);
    }

    //TO-UPDATE
    public void valueOf(String utcFormattedString) {
        LocalDateTime localDT = LocalDateTime.parse(utcFormattedString, utcFormat);
        Long localms = java.time.Duration.between(EPOCH, localDT).toNanos() / 1000000;
        ms = localms;
        dt = EPOCH.plusNanos(ms * 1000000);
    }

    public boolean isInitialized() {
        return ms != 0;
    }

    public boolean lessThan(Time t2) {
        return ms < t2.ms;
    }

    public boolean greaterThan(Time t2) {
        return ms > t2.ms;
    }

    public boolean lessThanOrEqualTo(Time t2) {
        return ms <= t2.ms;
    }

    public boolean greaterThanOrEqualTo(Time t2) {
        return ms >= t2.ms;
    }


    public static Time min(Time t1, Time t2) {
        if (t1.lessThan(t2)) {
            return t1;
        }
        else {
            return t2;
        }
    }

    public static Time max(Time t1, Time t2) {
        if (t1.greaterThan(t2)) {
            return t1;
        }
        else {
            return t2;
        }
    }

    // the next three methods are specified in the java Object standard so built-in data structures work well with them
    @Override
    public int compareTo(Time t2) {
        return Long.valueOf(ms).compareTo(t2.ms);
    }

    @Override
    public boolean equals(Object t2) {
        if (!(t2 instanceof Time)) {
            return false;
        }
        else {
            return ms == ((Time) t2).ms;
        }
    }

    @Override
    public int hashCode() {
        return (int) ms;
    }

}
