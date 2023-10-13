package gov.nasa.jpl.aerie.banananation.models;

import gov.nasa.jpl.aerie.contrib.models.Clock;
import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.merlin.framework.ModelActions;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

public record SequenceEngines(List<SequenceEngine> sequenceEngines, LockModel<String> lock) {
  public static SequenceEngines init(int numberOfEngines, final Instant startTime) {
    final Clock clock = new Clock(startTime);
    final var sequenceEngines = new ArrayList<SequenceEngine>();
    for (int i = 0; i < numberOfEngines; i++) {
      sequenceEngines.add(new SequenceEngine(Register.forImmutable(false), clock));
    }
    return new SequenceEngines(sequenceEngines, new LockModel<>(String::compareTo));
  }

  public <Model> void spawn(Model model, Sequence<Model> sequence) {
    final var engine = availableEngine(sequence.name());
    engine.active.set(true);
    ModelActions.spawn(() -> {
      engine.run(model, sequence.commands());
      engine.active.set(false);
    });
  }

  public <Model> void call(Model model, Sequence<Model> sequence) {
    final var engine = availableEngine(sequence.name());
    engine.active.set(true);
    ModelActions.call(() -> {
      engine.run(model, sequence.commands());
      engine.active.set(false);
    });
  }

  private SequenceEngine availableEngine(String newSeqId) {
    try (final var ignored = lock.lock(newSeqId)) {
      for (int i = 0; i < sequenceEngines.size(); i++) {
        final SequenceEngine engine = sequenceEngines.get(i);
        if (!engine.active().get()) {
          System.out.println(newSeqId + " claimed engine: " + i);
          return engine;
        }
      }
      throw new RuntimeException("No available sequence engines");
    }
  }

  public record SequenceEngine(Register<Boolean> active, Clock clock) {
    public <Model> void run(Model model, List<Command<Model>> commands) {
      final var commandQueue = new LinkedList<>(commands);
      while (!commandQueue.isEmpty()) {
        final var command = commandQueue.remove();
        if (command instanceof Command.AbsoluteTimeCommand<Model> c) {
          delayUntil(c.startTime());
          c.effect().accept(model);
        } else if (command instanceof Command.RelativeTimeCommand<Model> c) {
          delay(c.startOffset());
          c.effect().accept(model);
        } else {
          throw new Error("Unhandled variant of Command: " + command);
        }
      }
    }

    private void delayUntil(Instant instant) {
      delay(Duration.of(instant.toEpochMilli() - this.clock.getTime().toEpochMilli(), Duration.MILLISECONDS));
    }
  }
}
