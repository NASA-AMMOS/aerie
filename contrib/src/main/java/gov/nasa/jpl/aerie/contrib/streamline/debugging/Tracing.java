package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.DynamicsEffect;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Labelled;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching.failure;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentTime;

/**
 * Functions for debugging resources by tracing their calculation.
 */
public final class Tracing {
  private Tracing() {}

  private final static Stack<String> activeTracePoints = new Stack<>();

  public static <D> Resource<D> trace(String name, Resource<D> resource) {
    return traceFull(name, $ -> {}, resource);
  }

  public static <D> Resource<D> trace(String name, Consumer<D> assertion, Resource<D> resource) {
    return traceExpiring(name, $ -> assertion.accept($.data()), resource);
  }

  public static <D> Resource<D> traceExpiring(String name, Consumer<Expiring<D>> assertion, Resource<D> resource) {
    return traceFull(name, $ -> $.match(d -> {
      assertion.accept(d);
      return Unit.UNIT;
    }, e -> {
      throw new AssertionError("%s failed while computing".formatted(formatStack()), e);
    }), resource);
  }

  public static <D> Resource<D> traceFull(String name, Consumer<ErrorCatching<Expiring<D>>> assertion, Resource<D> resource) {
    return () -> traceAction(name, () -> {
      var result = resource.getDynamics();
      try {
        assertion.accept(result);
      } catch (Exception e) {
        result = failure(e);
      }
      return result;
    });
  }

  public static <D extends Dynamics<?, D>> CellResource<D> trace(String name, CellResource<D> resource) {
    return traceFull(name, $ -> {}, resource);
  }

  public static <D extends Dynamics<?, D>> CellResource<D> trace(String name, Consumer<D> assertion, CellResource<D> resource) {
    return traceExpiring(name, $ -> assertion.accept($.data()), resource);
  }

  public static <D extends Dynamics<?, D>> CellResource<D> traceExpiring(String name, Consumer<Expiring<D>> assertion, CellResource<D> resource) {
    return traceFull(name, $ -> $.match(d -> {
      assertion.accept(d);
      return Unit.UNIT;
    }, e -> {
      throw new AssertionError("%s failed while computing".formatted(formatStack()), e);
    }), resource);
  }

  public static <D extends Dynamics<?, D>> CellResource<D> traceFull(String name, Consumer<ErrorCatching<Expiring<D>>> assertion, CellResource<D> resource) {
    return new CellResource<>() {
      private final Resource<D> tracedResource = traceFull(name, assertion, (Resource<D>)resource);

      @Override
      public void emit(final Labelled<DynamicsEffect<D>> effect) {
        resource.emit(effect);
      }

      @Override
      public ErrorCatching<Expiring<D>> getDynamics() {
        return tracedResource.getDynamics();
      }

      @Override
      public void registerName(final String name) {
        resource.registerName(name);
      }
    };
  }

  public static Condition trace(String name, Condition condition) {
    return (positive, atEarliest, atLatest) ->
        traceAction(name + " evaluate (%s, %s, %s)".formatted(positive, atEarliest, atLatest), () -> condition.nextSatisfied(positive, atEarliest, atLatest));
  }

  public static Supplier<Condition> trace(String name, Supplier<Condition> condition) {
    // Trace calling the supplier separately from tracing the condition itself.
    return () -> traceAction(name + " (generation)", () -> trace(name, condition.get()));
  }

  private static <T> T traceAction(String name, Supplier<T> action) {
    activeTracePoints.push(name);
    System.out.printf("TRACE: %s - %s start...%n", currentTime(), formatStack());
    T result = action.get();
    System.out.printf("TRACE: %s - %s: %s%n", currentTime(), formatStack(), result);
    activeTracePoints.pop();
    return result;
  }

  private static String formatStack() {
    return String.join("->", activeTracePoints);
  }
}
