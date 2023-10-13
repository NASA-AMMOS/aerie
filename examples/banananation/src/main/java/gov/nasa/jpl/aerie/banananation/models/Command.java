package gov.nasa.jpl.aerie.banananation.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.util.function.Consumer;

public sealed interface Command<Model> {
  record AbsoluteTimeCommand<Model>(Instant startTime, Consumer<Model> effect) implements Command<Model> {}
  record RelativeTimeCommand<Model>(Duration startOffset, Consumer<Model> effect) implements Command<Model> {}
  static <Model> AbsoluteTimeCommand<Model> absolute(Instant instant, Consumer<Model> effect) {
    return new AbsoluteTimeCommand<>(instant, effect);
  }
  static <Model> RelativeTimeCommand<Model> relative(Duration startOffset, Consumer<Model> effect) {
    return new RelativeTimeCommand<>(startOffset, effect);
  }
}
