package gov.nasa.jpl.aerie.merlin.protocol.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class UnconstructableException extends Exception {
  public final Reason reason;

  public UnconstructableException(final Reason reason) {
    super(reason.toString());
    this.reason = Objects.requireNonNull(reason);
  }

  public sealed interface Reason {

    record MissingArguments(
        List<ProvidedArgument> providedArguments,
        List<MissingArgument> missingArguments
    ) implements Reason {

      public record ProvidedArgument(String parameterName, SerializedValue serializedValue) {
        public ProvidedArgument {
          Objects.requireNonNull(parameterName);
          Objects.requireNonNull(serializedValue);
        }
      }

      public record MissingArgument(String parameterName, ValueSchema schema) {
        public MissingArgument {
          Objects.requireNonNull(parameterName);
          Objects.requireNonNull(schema);
        }
      }

      public static final class Builder {
        private final List<ProvidedArgument> providedArguments = new ArrayList<>();
        private final List<MissingArgument> missingArguments = new ArrayList<>();

        public Builder withProvidedArgument(final String parameterName, final SerializedValue serializedValue) {
          providedArguments.add(new ProvidedArgument(parameterName, serializedValue));
          return this;
        }

        public Builder withMissingArgument(final String parameterName, final ValueSchema schema) {
          missingArguments.add(new MissingArgument(parameterName, schema));
          return this;
        }

        public MissingArguments build() {
          return new MissingArguments(providedArguments, missingArguments);
        }
      }
    }

    record UnconstructableArgument(String parameterName, String failure) implements Reason { }

    record NonexistentArgument(String parameterName) implements Reason { }

    record NonexistentType(String typeName) implements Reason { }
  }
}
