package gov.nasa.jpl.ammos.mpsa.aerie.banananation.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.events.IndependentStateEvent;

import java.util.Objects;
import java.util.Optional;

// This can be mechanically derived from `EventHandler`.
public abstract class BananaEvent {
  private BananaEvent() {}

  public abstract <Result> Result visit(BananaEventHandler<Result> visitor);

  public static BananaEvent independent(final IndependentStateEvent event) {
    Objects.requireNonNull(event);
    return new BananaEvent() {
      @Override
      public <Result> Result visit(final BananaEventHandler<Result> visitor) {
        return visitor.independent(event);
      }
    };
  }

  public final Optional<IndependentStateEvent> asIndependent() {
    return this.visit(new DefaultBananaEventHandler<>() {
      @Override
      public Optional<IndependentStateEvent> unhandled() {
        return Optional.empty();
      }

      @Override
      public Optional<IndependentStateEvent> independent(final IndependentStateEvent event) {
        return Optional.of(event);
      }
    });
  }

  @Override
  public final String toString() {
    return this.visit(new BananaEventHandler<>() {
      @Override
      public String independent(final IndependentStateEvent event) {
        return String.format("independent.%s", event);
      }
    });
  }
}
