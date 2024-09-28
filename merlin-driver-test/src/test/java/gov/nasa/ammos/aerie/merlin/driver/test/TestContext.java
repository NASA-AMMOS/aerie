package gov.nasa.ammos.aerie.merlin.driver.test;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class TestContext {
  private static Context currentContext = null;

  public record Context(CellMap cells, Scheduler scheduler, ThreadedTask<?> threadedTask) {}

  public static final class CellMap {
    private final Map<SideBySideTest.Cell, CellId<?>> cells = new LinkedHashMap<>();
    public <T> void put(SideBySideTest.Cell cell, CellId<MutableObject<T>> cellId) {
      cells.put(Objects.requireNonNull(cell), Objects.requireNonNull(cellId));
    }

    @SuppressWarnings("unchecked")
    public <T> CellId<T> get(SideBySideTest.Cell cell) {
      return (CellId<T>) Objects.requireNonNull(cells.get(Objects.requireNonNull(cell)));
    }
  }

  public static Context get() {
    return currentContext;
  }

  public static void set(Context context) {
    Objects.requireNonNull(context, "Use clear() instead");
    currentContext = context;
  }

  public static void clear() {
    currentContext = null;
  }
}
