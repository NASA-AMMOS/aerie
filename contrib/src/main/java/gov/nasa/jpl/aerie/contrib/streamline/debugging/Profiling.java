package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Comparator.comparingLong;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Functions for profiling resources and conditions
 *
 * <p>
 *   WARNING! Profiling tools use static variables
 *   that are not re-initialized when the simulation restarts.
 *   This may give inconsistent or erroneous output in unit tests or scheduling.
 *   <p><em>Do not depend on profiling data for model behavior!</em></p>
 * </p>
 * <p>
 *   Additionally, all profiling methods short-circuit if a null or empty name is given.
 *   Profile only some invocations of a method by giving "" as a default profiling name
 *   in a non-profiled overload of the method, e.g.
 *   <pre>
 *     void someMethod(int a, String b) {
 *       // By passing "", we turn off profiling implicitly
 *       someMethod("", a, b);
 *     }
 *     void someMethod(String profilingName, int a, String b) {
 *       // some code that uses a profiling method
 *     }
 *   </pre>
 * </p>
 */
public final class Profiling {
  private Profiling() {}

  private static final long overallStartTime = System.nanoTime();
  private static final Map<String, CallStats> resourceSamples = new HashMap<>();
  private static final Map<String, CallStats> conditionEvaluations = new HashMap<>();
  private static final Map<String, CallStats> taskExecutions = new HashMap<>();

  /**
   * Format name, but only if it's non-empty,
   * to respect profiler short-circuiting for empty names.
   */
  public static String formatName(String format, String name) {
    return isEmpty(name) ? name : format.formatted(name);
  }

  public static <D> Resource<D> profile(String name, Resource<D> resource) {
    if (isEmpty(name)) return resource;
    initialize("Resource", resourceSamples, name);
    return () -> resourceSamples.get(name).accrue(resource::getDynamics);
  }

  public static Condition profile(String name, Condition condition) {
    if (isEmpty(name)) return condition;
    initialize("Condition", conditionEvaluations, name);
    return (positive, atEarliest, atLatest) ->
        conditionEvaluations.get(name).accrue(
            () -> condition.nextSatisfied(positive, atEarliest, atLatest));
  }

  public static Supplier<Condition> profile(String name, Supplier<Condition> conditionSupplier) {
    if (isEmpty(name)) return conditionSupplier;
    initialize("Condition", conditionEvaluations, name);
    return () -> {
      final var condition = conditionSupplier.get();
      return (positive, atEarliest, atLatest) ->
          conditionEvaluations.get(name).accrue(
              () -> condition.nextSatisfied(positive, atEarliest, atLatest));
    };
  }

  public static Runnable profile(String name, Runnable task) {
    if (isEmpty(name)) return task;
    initialize("Task", taskExecutions, name);
    return () -> taskExecutions.get(name).accrue(task);
  }

  public static <R> Supplier<R> profileTask(String name, Supplier<R> task) {
    if (isEmpty(name)) return task;
    initialize("Task", taskExecutions, name);
    return () -> taskExecutions.get(name).accrue(task);
  }

  private static void initialize(String typeName, Map<String, CallStats> resourceSamples, String name) {
    if (resourceSamples.containsKey(name)) {
      System.out.printf("WARNING! %s %s is already being profiled. This may be a name collision.%n", typeName, name);
    } else {
      resourceSamples.put(name, new CallStats());
    }
  }

  public static void dump() {
    long overallElapsedNanos = System.nanoTime() - overallStartTime;
    System.out.printf("Overall time: %d ms%n", overallElapsedNanos / 1_000_000);
    if (!resourceSamples.isEmpty()) {
      System.out.println("Profiled resources:");
      dumpSampleMap(resourceSamples, overallElapsedNanos);
    }
    if (!conditionEvaluations.isEmpty()) {
      System.out.println("Profiled conditions:");
      dumpSampleMap(conditionEvaluations, overallElapsedNanos);
    }
    if (!taskExecutions.isEmpty()) {
      System.out.println("Profiled tasks:");
      dumpSampleMap(taskExecutions, overallElapsedNanos);
    }
  }

  private static void dumpSampleMap(Map<String, CallStats> map, long overallElapsedNanos) {
    final var nameLength = Math.max(5, map.keySet().stream().mapToInt(String::length).max().orElse(1));
    final var totalCalls = map.values().stream().mapToLong(c1 -> c1.callsMade).sum();
    final var totalNanos = map.values().stream().mapToLong(c1 -> c1.nanosInCall).sum();
    final var callsLength = String.valueOf(totalCalls).length();
    final var millisLength = String.valueOf(totalNanos / 1_000_000).length();
    final var lineFormat =
        "  %-" + nameLength + "s ->"
        + "   %" + callsLength + "d calls - %5.1f%%   -"
        + "   %" + millisLength + "d ms - %5.1f%% / %5.1f%%%n";
    System.out.printf(
        lineFormat,
        "Total",
        totalCalls,
        100.0,
        totalNanos / 1_000_000,
        100.0,
        100.0 * totalNanos / overallElapsedNanos);
    map.entrySet()
       .stream()
       .sorted(comparingLong(c -> -c.getValue().nanosInCall))
       .forEachOrdered(entry -> {
         var stats = entry.getValue();
         System.out.printf(
             lineFormat,
             entry.getKey(),
             stats.callsMade,
             100.0 * stats.callsMade / totalCalls,
             stats.nanosInCall / 1_000_000,
             100.0 * stats.nanosInCall / totalNanos,
             100.0 * stats.nanosInCall / overallElapsedNanos);
       });
  }

  private static final class CallStats {
    public long callsMade = 0;
    public long nanosInCall = 0;

    public void accrue(Runnable call) {
      accrue(() -> { call.run(); return Unit.UNIT; });
    }

    public <R> R accrue(Supplier<R> call) {
      long start = System.nanoTime();
      var result = call.get();
      long end = System.nanoTime();
      ++callsMade;
      nanosInCall += end - start;
      return result;
    }
  }
}
