package gov.nasa.jpl.aerie.stats;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Timer measures both wall clock, CPU time, and counting.
 * Individual instances of Timer capture a single time interval.
 * A resettable static map keeps statistics across multiple Timers,
 * which could be in separate threads.
 * Users of Timer should be careful about threads.  A Timer instance
 * only measures time for the existing thread, excluding the CPU time
 * of spawned threads.  Timers must be instantiated separately for
 * each thread.
 */
public class Timer {

  // STATIC MEMBERS

  private static final Logger logger = LoggerFactory.getLogger(Timer.class);

  /**
   * The stats recorded for multiple occurrences (Timer instantiations) -- since it's static and could be accessed
   * by multiple threads, we use a ConcurrentMap for thread safety.
   */
  protected static ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, Long>> stats = new ConcurrentSkipListMap<>();

  /**
   *  A map from the start time so that we can write stats out in time order.
   */
  protected static ConcurrentSkipListMap<Long, ConcurrentSkipListSet<String>> labelsByStartTime = new ConcurrentSkipListMap<>();

  /**
   *
   *  @return the stats map for custom use
   */
  public static ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, Long>> getStats() {
    return stats;
  }

  /**
   * Clear the existing statically recorded stats maps to start collect stats
   */
  public static void reset() {
    stats.clear();
    labelsByStartTime.clear();
  }

  /**
   * Utility for getting or creating a map nested in another map.
   * @param label key of the outer map
   * @param stat key of the inner map
   * @return
   */
  protected static ConcurrentSkipListMap<String, Long> getInnerMap(String label, String stat) {
    ConcurrentSkipListMap<String, Long> innerMap;
    if (stats.keySet().contains(label)) {
      innerMap = stats.get(label);
    } else {
      innerMap = new ConcurrentSkipListMap<>();
      stats.put(label, innerMap);
    }
    return innerMap;
  }

  /**
   * Add the value to the existing one for the stat and label.
   * @param label the thing for which the stat applies
   * @param stat the kind of stat (e.g. "cpu time")
   * @param value the increase in the stat value
   */
  public static void addStat(String label, String stat, long value) {
    // Don't add start or end time values.  Call putStat() to overwrite instead of add.
    if (stat.startsWith("start") || stat.startsWith("end")) {
      putStat(label, stat, value);
      return;
    }
    // Make sure map entries exist before adding.
    final ConcurrentSkipListMap<String, Long> innerMap = getInnerMap(label, stat);
    if (!innerMap.containsKey(stat)) {
      innerMap.put(stat, value);
    } else {
      innerMap.put(stat, innerMap.get(stat) + value);
    }
  }

  /**
   * Insert or overwrite the value of the stat for the label.
   * @param label the thing for which the stat applies
   * @param stat the kind of stat (e.g. "start")
   * @param value the increase in the stat value
   */
  public static void putStat(String label, String stat, long value) {
    // Make sure map entries exist before adding.
    final ConcurrentSkipListMap<String, Long> innerMap = getInnerMap(label, stat);
    innerMap.put(stat, value);

    // If this is the start time, add to the labelsByStart map.
    if (stat.startsWith("start")) {
      final ConcurrentSkipListSet<String> timeList;
      if (labelsByStartTime.containsKey(value)) {
        timeList = labelsByStartTime.get(value);
      } else {
        timeList = new ConcurrentSkipListSet<>();
        labelsByStartTime.put(value, timeList);
      }
      timeList.add(label);
    }
  }


  public static <T> T run(String label, Supplier<T> r) {
    Timer timer = new Timer(label);
    T t = r.get();
    timer.stop();
    return t;
  }

  public static String formatDuration(Long nanoseconds) {
    return (nanoseconds / 1.0e9) + " seconds";
  }

  // Stole this from Duration.  Consider moving Duration to a different package
  public static String formatDurationFancy(long nanoseconds) {
    var rest = nanoseconds;

    var sign = "";
    if (rest < 0L) {
      sign = "-";
      rest  = -rest;
    }

    final long hours;
    hours = rest / (3600L * 1_000_000_000L);
    rest = rest % (3600L * 1_000_000_000L);

    final long minutes = rest / (60L * 1_000_000_000L);
    rest = rest % (60L * 1_000_000_000L);

    final long seconds = rest / 1_000_000_000L;
    rest = rest % 1_000_000_000L;

    final long microseconds = rest / 1000L;

    return String.format("%s%02d:%02d:%02d.%06d", sign, hours, minutes, seconds, microseconds);
  }

  /**
   * These stats are written out differently.
   */
  protected static TreeSet<String> timeAndCountStats =
      new TreeSet<>(Arrays.asList( "start", "end", "count"));

  /**
   * Write out the stats for each label ordered by time.
   * @return a string with each stat written on a different line
   */
  public static String summarizeStats() {
    StringBuilder sb = new StringBuilder();
    TreeMap<Long, List<String>> labelsByEnd = new TreeMap<>();
    TreeSet<Long> endTimesCopy;

    // Loop through labels in order of start time.
    for (Long start : labelsByStartTime.keySet()) {  // nanoseconds
      // Write any passed end times before this start
      endTimesCopy = new TreeSet<>(labelsByEnd.keySet()); // copy so that we can remove entries--consider priority queue
      for (Long end : endTimesCopy) {  // nanoseconds
        if ( end > start + 1000000 ) break;  // only end times before or roughly at the same time the start
        for (String label: labelsByEnd.get(end)) {
          sb.append(label + ": end = " + formatTimestamp(end) + "\n");
        }
        labelsByEnd.remove(end);
      }

      // Write start, duration, and number of occurrences.
      for (String label: labelsByStartTime.get(start)) {
        final ConcurrentSkipListMap<String, Long> statsForLabel = stats.get(label);
        Long end = statsForLabel.get("end");
        sb.append( label + ": start = " + formatTimestamp(start) + "\n");
        if ( end != null ) {
          var labels = labelsByEnd.get(end);
          if (labels == null) {
            labels = new ArrayList<>();
            labelsByEnd.put(end, labels);
          }
          labels.add(label);
          long duration = end - start;
          sb.append(label + ": duration = " + formatDuration(duration) + "\n");
          Long count = statsForLabel.get("count");
          if (count != null && count > 1) {
            sb.append(label + ": " + count + " occurrences\n");
            // Averaging the duration above doesn't make sense since the occurrences may have been sporadic.
            // The "other duration stats" below could be averaged but aren't just to keep output simple.
            // But, maybe a total, min, max, avg column justified would be nice.
          }
        }

        // Write all other duration stats for the label.  (Note that the stats are assumed to all be nanoseconds!)
        for (String stat : statsForLabel.keySet()) {
          if (!timeAndCountStats.contains(stat)) {
            sb.append(label + ": " + stat + " = " + formatDuration(statsForLabel.get(stat)) + "\n");
          }
        }
      }
    }

    // Write remaining end times now that we're done looping through start times.
    endTimesCopy = new TreeSet<>(labelsByEnd.keySet());
    for (Long end : endTimesCopy) {  // nanoseconds
      for (String label: labelsByEnd.get(end)) {
        sb.append(label + ": end = " + formatTimestamp(end) + "\n");
      }
      labelsByEnd.remove(end);
    }
    return sb.toString();
  }

  /**
   * Get the string with lines of stats from summarizeStats() and log each line with a little decoration.
   */
  public static void logStats() {
    logger.info(timestampNow() + " %% REPORTING TIMER STATS %%");
    String stats = summarizeStats();
    final String[] lines = stats.split("\n");
    List.of(lines).forEach(x -> logger.info(" %% " + x ));
  }

  // It would be nice to use the Timestamp class here.  Consider moving Timestamp to a more general package.
  /**
   * ISO timestamp format
   */
  public static final DateTimeFormatter format =
      new DateTimeFormatterBuilder()
          .appendPattern("uuuu-DDD'T'HH:mm:ss")
          .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
          .toFormatter();

  /**
   * Format nanoseconds from Instant into a date-timestamp.
    * @param nanoseconds
   * @return formatted string
   */
  protected static String formatTimestamp(long nanoseconds) {
    return formatTimestamp(Instant.ofEpochSecond(0L, nanoseconds));
  }

  /**
   * Format Instant into a date-timestamp.
   * @param instant
   * @return formatted string
   */
  protected static String formatTimestamp(Instant instant) {
    return format.format(instant.atZone(ZoneOffset.UTC));
  }

  /**
   * Format the current system time into a date-timestamp
    * @return
   */
  protected static String timestampNow() {
    return formatTimestamp(Instant.now());
  }


  // NON-STATIC MEMBERS

  protected String label;  // The name of the thing for which the stats are recorded,
  protected long initialWallClockTime; // nanoseconds
  protected long accumulatedWallClockTime = 0;  // nanoseconds
  protected long initialCpuTime;  // nanoseconds
  protected long accumulatedCpuTime = 0;  // nanoseconds

  protected long threadId;  // TODO - consider getting rid of one or both of these
  protected Thread thread;  // TODO - consider getting rid of one or both of these



  /**
   * Start a timer with a label and optionally log the start event.
   * @param label a name for a category in which stats are collected and summed
   * @param t the Thread from which stats are collected
   * @param writeToLog if true, logs the start of the timer if the first occurrence of this label since the last reset
   */
  public Timer(String label, Thread t, boolean writeToLog) {
    this.label = label;
    this.thread = t;
    this.threadId = t.threadId();

    // Get the wall clock time in nanoseconds
    Instant start = Instant.now();   // Some say that System.nanoTime() is more accurate.
    initialWallClockTime = start.getEpochSecond() * 1_000_000_000L + start.getNano();  // 64-bit long is good until year 2262

    // Only record the start time stat the first time for the label to mark the start of all occurrences.
    if (!stats.containsKey(label) || !stats.get(label).containsKey("start")) {
      putStat(label, "start", initialWallClockTime);
      if (writeToLog) {
        logger.info(formatTimestamp(initialWallClockTime) + " -- " + label + " -- start");
      }
    }

    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    initialCpuTime = threadMXBean.getThreadCpuTime(threadId);
  }

  /**
   * Start a timer with a label and optionally log the start event.
   * @param label
   * @param writeToLog
   */
  public Timer(String label, boolean writeToLog) {
    this(label, Thread.currentThread(), writeToLog);
  }

  /**
   * Start a timer with a label.
   * @param label
   */
  public Timer(String label) {
    this(label, false);  // default - don't log start time
  }

  /**
   * Stop the timer, get stats, combine with static stats (for multiple Timers), and optionally log the end time.
   * @param writeToLog
   */
  public void stop(boolean writeToLog) {
    Instant end = Instant.now();
    long endWallClockTime = end.getEpochSecond() * 1_000_000_000L + end.getNano();    // This is good until 2262-04-11
    accumulatedWallClockTime = endWallClockTime - initialWallClockTime;

    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    accumulatedCpuTime = threadMXBean.getThreadCpuTime(threadId) - initialCpuTime;

    addStat(label, "wall clock time", accumulatedWallClockTime);
    addStat(label, "cpu time", accumulatedCpuTime);
    addStat(label, "count", 1);
    putStat(label, "end", endWallClockTime);
    if (writeToLog) {
      logger.info(formatTimestamp(end) + " -- " + label + " -- end");
    }
  }

  /**
   * Stop the timer, get stats, and combine with static stats (for multiple Timers).
   */
  public void stop() {
    stop(false);  // don't log end time
  }

  /**
   * @return a string reporting the cpu and wall clock time so far this Timer
   * This is not being used -- consider removing
   */
  public String status() {
    Instant now = Instant.now();
    accumulatedWallClockTime = now.getEpochSecond() * 1_000_000_000L + now.getNano() - initialWallClockTime;    // 64-bit long is good until 2262-04-11

    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    accumulatedCpuTime = threadMXBean.getThreadCpuTime(threadId) - initialCpuTime;
    String status = "Thread " + this.thread + " -- cpu time = " + formatDuration(accumulatedCpuTime) +
                    "; wall clock time = " + formatDuration(accumulatedWallClockTime);
    return status;
  }

}
