package gov.nasa.jpl.aerie.contrib.cells.register;

import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.protocol.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MerlinExtension.class)
public final class RegisterTest {
  private final Register<Integer> register = Register.create(0);

  @Test
  @DisplayName("Conflicting writes should mark the conflict flag and have no other effect")
  public void testConflict() {
    assertEquals(0, register.get());
    spawn(() -> register.set(1));
    spawn(() -> register.set(2));
    delay(Duration.ZERO);
    assertTrue(register.isConflicted());
    assertEquals(0, register.get());
  }
}
