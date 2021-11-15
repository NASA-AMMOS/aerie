package gov.nasa.jpl.aerie.contrib.cells.register;

import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MerlinExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public final class RegisterTest {
  private final Register<Integer> register = Register.forImmutable(0);

  @Test
  @DisplayName("Conflicting writes with different values should mark the conflict flag and have no other effect")
  public void differentValueConflicts() {
    assertEquals(0, register.get());
    spawn(() -> register.set(1));
    spawn(() -> register.set(2));
    delay(Duration.ZERO);
    assertAll(List.of(
        () -> assertTrue(register.isConflicted()),
        () -> assertEquals(0, register.get())));
  }

  @Test
  @DisplayName("Conflicting writes with the same value should mark the conflict flag and have no other effect")
  public void sameValueConflicts() {
    assertEquals(0, register.get());
    spawn(() -> register.set(1));
    spawn(() -> register.set(1));
    delay(Duration.ZERO);
    assertAll(List.of(
        () -> assertTrue(register.isConflicted()),
        () -> assertEquals(0, register.get())));
  }
}
