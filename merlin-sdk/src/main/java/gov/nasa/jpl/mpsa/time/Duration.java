package gov.nasa.jpl.mpsa.time;


import gov.nasa.jpl.mpsa.utilities.ConvertableFromString;
import gov.nasa.jpl.mpsa.utilities.RegexUtilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Duration implements Comparable<Duration>, ConvertableFromString {
    // Earth milliseconds
    private long ms;

    public final static long ONE_DAY = 86400000; //ms
    public final static long ONE_HOUR = 3600000; //ms
    public final static long ONE_MINUTE = 60000; //ms
    public final static long ONE_SECOND = 1000; //ms

    private static String formatForLessThanOneDay = "%02d:%02d:%02d.%03d";
    private static String formatForGreaterThanOneDay = "%dT" + formatForLessThanOneDay;

    public Duration(long ms) {
        if (ms < 0) {
            throw new RuntimeException("Tried to create a negative duration");
        }
        this.ms = ms;
    }

    public Duration(String d) {
        valueOf(d);
    }

    public Duration() {
        this.ms = 1;
    }

    public long getMS() {
        return ms;
    }

    public double totalSeconds() {
        return ((double) ms) / 1000.0;
    }

    public Duration add(Duration d2) {
        return new Duration(ms + d2.ms);
    }

    public Duration subtract(Duration d2) {
        return new Duration(ms - d2.ms);
    }

    public String toString() {
        long remainder = 0;
        long days = ms / ONE_DAY;
        remainder = ms % ONE_DAY;
        long hours = remainder / ONE_HOUR;
        remainder = remainder % ONE_HOUR;
        long minutes = remainder / ONE_MINUTE;
        remainder = remainder % ONE_MINUTE;
        long seconds = remainder / ONE_SECOND;
        long onlyMS = remainder % ONE_SECOND;

        if (ms > ONE_DAY) {
            return String.format(formatForGreaterThanOneDay, days, hours, minutes, seconds, onlyMS);
        }
        else {
            return String.format(formatForLessThanOneDay, hours, minutes, seconds, onlyMS);
        }
    }

    public void valueOf(String s) {
        // Create Pattern objects
        Pattern regexdays = Pattern.compile(RegexUtilities.DAYS_IN_DURATION_REGEX);
        Pattern regexhours = Pattern.compile(RegexUtilities.DURATION_LESS_THAN_ONE_DAY_REGEX);
        // Create Matcher objects
        Matcher daysMatcher = regexdays.matcher(s);
        Matcher hoursMatcher = regexhours.matcher(s);

        long days;
        if (daysMatcher.find()) {
            days = Integer.parseInt(daysMatcher.group(1));
        }
        else {
            days = 0;
        }
        if (hoursMatcher.find()) {
            long hours = Long.parseLong(hoursMatcher.group(1));
            long minutes = Long.parseLong(hoursMatcher.group(2));
            double seconds = Double.parseDouble(hoursMatcher.group(3));
            ms = days * ONE_DAY + hours * ONE_HOUR + minutes * ONE_MINUTE + Math.round(seconds * ONE_SECOND);
        }
        else {
            throw new RuntimeException("Cannot cast " + s + " to Duration");
        }
    }

    public boolean lessThan(Duration t2) {
        return ms < t2.ms;
    }

    public boolean greaterThan(Duration t2) {
        return ms > t2.ms;
    }

    public boolean lessThanOrEqualTo(Duration t2) {
        return ms <= t2.ms;
    }

    public boolean greaterThanOrEqualTo(Duration t2) {
        return ms >= t2.ms;
    }


    // the next three methods are specified in the java Object standard so built-in data structures work well with them
    public int compareTo(Duration t2) {
        return Long.valueOf(ms).compareTo(t2.ms);
    }

    @Override
    public boolean equals(Object t2) {
        if (!(t2 instanceof Duration)) {
            return false;
        }
        else {
            return ms == ((Duration) t2).ms;
        }
    }

    @Override
    public int hashCode() {
        return (int) ms;
    }
}
