package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public final class ThreadedTaskTest {
  @Test
  @DisplayName("Thrown exceptions can be caught transparently")
  public <$Timeline> void testTransparentExceptions() {
    final var mockScheduler = new Scheduler<$Timeline>() {
      @Override
      public <State> State get(final Query<? super $Timeline, ?, State> query) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <Event> void emit(final Event event, final Query<? super $Timeline, ? super Event, ?> query) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String spawn(final Task<$Timeline> task) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String spawn(final String type, final Map<String, SerializedValue> arguments) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String defer(final Duration delay, final Task<$Timeline> task) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String defer(final Duration delay, final String type, final Map<String, SerializedValue> arguments) {
        throw new UnsupportedOperationException();
      }
    };

    class TestException extends RuntimeException {}

    final var task = new ThreadedTask<$Timeline>(Scoped.create(), () -> {
      throw new TestException();
    });

    final var ex = assertThrows(TestException.class, () -> task.step(mockScheduler));
    assertSuppressed(ThreadedTask.TaskFailureException.class, ex);
  }

  private static void assertSuppressed(final Class<? extends Throwable> expected, final Throwable ex) {
    for (final var suppressed : ex.getSuppressed()) {
      if (expected.isAssignableFrom(suppressed.getClass())) return;
    }

    fail("Missing suppressed exception of type `" + expected + "`", ex);
  }
}
