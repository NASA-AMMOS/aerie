package gov.nasa.jpl.ammos.mpsa.aerie.banananation.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events.ApgenEvent;

import java.util.Objects;
import java.util.Optional;

// This can be mechanically derived from `EventHandler`.
public abstract class BananaEvent {
  private BananaEvent() {}

  public abstract <Result> Result visit(BananaEventHandler<Result> visitor);

  public static BananaEvent apgen(final ApgenEvent event) {
    Objects.requireNonNull(event);
    return new BananaEvent() {
      @Override
      public <Result> Result visit(final BananaEventHandler<Result> visitor) {
        return visitor.apgen(event);
      }
    };
  }

  public final Optional<ApgenEvent> asApgen() {
    return this.visit(new DefaultBananaEventHandler<>() {
      @Override
      public Optional<ApgenEvent> unhandled() {
        return Optional.empty();
      }

      @Override
      public Optional<ApgenEvent> apgen(final ApgenEvent event) {
        return Optional.of(event);
      }
    });
  }

  @Override
  public final String toString() {
    return this.visit(new BananaEventHandler<>() {
      @Override
      public String apgen(final ApgenEvent event) {
        return String.format("apgen.%s", event);
      }
    });
  }
}
