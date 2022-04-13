package gov.nasa.jpl.aerie.merlin.protocol.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class MissingArgumentsException extends RuntimeException {
  public final String containerName, metaName;
  public final List<ProvidedArgument> providedArguments;
  public final List<MissingArgument> missingArguments;

  private MissingArgumentsException(
      final String containerName,
      final String metaName,
      final List<ProvidedArgument> providedArguments,
      final List<MissingArgument> missingArguments)
  {
    super("Missing arguments for %s \"%s\": %s"
      .formatted(metaName, containerName, missingArguments.stream().map(a -> "\"%s\"".formatted(a.parameterName)).collect(Collectors.joining(", "))));
    this.containerName = containerName;
    this.metaName = metaName;
    this.providedArguments = Collections.unmodifiableList(providedArguments);
    this.missingArguments = Collections.unmodifiableList(missingArguments);
  }

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
    private final String containerName, metaName;

    public Builder(final String containerName, final String metaName) {
      this.containerName = containerName;
      this.metaName = metaName;
    }

    public Builder withProvidedArgument(final String parameterName, final SerializedValue serializedValue) {
      providedArguments.add(new ProvidedArgument(parameterName, serializedValue));
      return this;
    }

    public Builder withMissingArgument(final String parameterName, final ValueSchema schema) {
      missingArguments.add(new MissingArgument(parameterName, schema));
      return this;
    }

    public MissingArgumentsException build() {
      return new MissingArgumentsException(containerName, metaName, providedArguments, missingArguments);
    }
  }
}
