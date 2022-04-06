package gov.nasa.jpl.aerie.scheduler.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public record MerlinActivityInstance(String type,
                                     Duration startTimestamp,
                                     Map<String, SerializedValue> arguments) {

    public MerlinActivityInstance(final String type,
                                  final Duration startTimestamp,
                                  final Map<String, SerializedValue> arguments) {
      this.type = type;
      this.startTimestamp = startTimestamp;
      this.arguments = (arguments != null) ? Map.copyOf(arguments) : null;
    }

  }
