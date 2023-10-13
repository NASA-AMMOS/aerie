package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.models.Command;
import gov.nasa.jpl.aerie.banananation.models.Sequence;
import gov.nasa.jpl.aerie.banananation.models.SequenceEngines;
import gov.nasa.jpl.aerie.contrib.models.Accumulator;
import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MerlinExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SequenceEngineTest {
  public static final Instant START_TIME = Instant.ofEpochMilli(1697171292);
  public SequenceEngines sequenceEngines = SequenceEngines.init(32, START_TIME);
  public Register<Boolean> flag = Register.forImmutable(false);
  public Accumulator secondsElapsed = new Accumulator(0, 1);

  @Test
  void basicSpawnTest() {
    sequenceEngines.spawn(this, Sequence.of("eng_00000", Command.absolute(START_TIME.plusSeconds(100), $ -> flag.set(true))));
    delay(99, SECONDS);
    assertEquals(false, flag.get());
    delay(2, SECONDS);
    assertEquals(true, flag.get());
  }

  @Test
  void testNestedSpawn() {
    final var sequence2 = Sequence.<SequenceEngineTest>of("eng_00000",
        Command.relative(
            Duration.of(5, SECONDS),
            $ -> $.flag.set(true)));
    final var sequence1 = Sequence.<SequenceEngineTest>of("eng_00001",
        Command.relative(
            Duration.ZERO,
            $ -> $.sequenceEngines.spawn(this, sequence2)));

    sequenceEngines.spawn(this, sequence1);
    delay(4, SECONDS);
    assertEquals(false, flag.get());
    delay(2, SECONDS);
    assertEquals(true, flag.get());
  }

  @Test
  void testSimultaneousSequences() {
    final var sequence1 = Sequence.<SequenceEngineTest>of("eng_00001",
                                                          Command.relative(
                                                              Duration.of(2, SECONDS),
                                                              $ -> $.flag.set(true)));
    final var sequence2 = Sequence.<SequenceEngineTest>of("eng_00002",
                                                          Command.relative(
                                                              Duration.of(4, SECONDS),
                                                              $ -> $.flag.set(false)));

    sequenceEngines.spawn(this, sequence1);
    sequenceEngines.spawn(this, sequence2);
    System.out.println("Spawned both");

    delay(1, SECOND);
    System.out.println("Stepping test");

    var activeCount = 0;
    for (final var engine : sequenceEngines.sequenceEngines()) {
      if (engine.active().get()) {
        activeCount++;
      }
    }

    assertEquals(2, activeCount);

    assertEquals(false, flag.get());
    delay(2, SECONDS);
    assertEquals(true, flag.get());
    delay(2, SECONDS);
    assertEquals(false, flag.get());
  }

  @Test
  void testRunningOutOfSequenceEngines() {
    for (int i = 0; i < 32; i++) {
      sequenceEngines.spawn(this, Sequence.of("abc_" + (i + 1), Command.relative(Duration.of(1, SECOND), $ -> {})));
    }
    try {
      sequenceEngines.spawn(this, Sequence.of("abc_" + 33, Command.relative(Duration.of(1, SECOND), $ -> {})));
    } catch (RuntimeException e) {
      assertEquals("No available sequence engines", e.getMessage());
      return;
    }
    fail("Expected an exception to be thrown");
  }

  // TODO when is a sequence done? Should commands be "spawned" and not awaited?
  @Test
  void testCall() {
    sequenceEngines.call(this, Sequence.of("eng_00000", Command.absolute(START_TIME.plusSeconds(10), $ -> {})));
    assertEquals(secondsElapsed.get(), 10);
  }
}
