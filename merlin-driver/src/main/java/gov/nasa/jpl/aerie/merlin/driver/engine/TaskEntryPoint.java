package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;

public sealed interface TaskEntryPoint {
  record Directive(SerializedActivity directive) implements TaskEntryPoint {}
  record Daemon() implements TaskEntryPoint {}
}
