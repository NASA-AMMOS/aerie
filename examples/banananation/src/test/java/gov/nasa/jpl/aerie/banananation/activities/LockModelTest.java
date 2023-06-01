package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.contrib.cells.register.RegisterCell;
import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Comparator;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MerlinExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LockModelTest {
  private final LockModel<Integer> lockModel = new LockModel<>(Comparator.comparingInt($ -> $));
  private final Register<Integer> register = Register.forImmutable(-1);

  @Test
  void test() {
    spawn(() -> {
      final var taskId = 1;
      lockModel.lock(taskId);
      register.set(taskId);
      delay(10, SECOND);
      lockModel.release(taskId);
    });

    spawn(() -> {
      final var taskId = 2;
      lockModel.lock(taskId);
      register.set(taskId);
      lockModel.release(taskId);
    });

    assertEquals(-1, register.get());
    delay(5, SECONDS);
    assertEquals(1, register.get());
    delay(6, SECONDS);
    assertEquals(2, register.get());
  }

}
