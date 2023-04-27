package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import java.util.Map;

public interface SchedulerModel {
  Map<String, DurationType> getDurationTypes();
}
