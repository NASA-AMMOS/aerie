package gov.nasa.jpl.aerie.scheduler;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import org.junit.jupiter.api.Test;

public class ActivityTypeTest {

  @Test
  public void ctor_1Arg() {
    new ActivityType("A");
  }

  @Test
  public void ctor_throwsNPEOnNullName() {
    assertThrows(NullPointerException.class, () -> new ActivityType(null));
  }

  @Test
  public void getName_ofCtorValue() {
    final var expected = "A";
    final var obj = new ActivityType(expected);
    final var actual = obj.getName();
    assertThat(actual).isEqualTo(expected);
  }
}
