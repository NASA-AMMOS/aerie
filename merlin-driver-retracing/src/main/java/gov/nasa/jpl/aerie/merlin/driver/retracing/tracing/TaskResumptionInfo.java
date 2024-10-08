package gov.nasa.jpl.aerie.merlin.driver.retracing.tracing;

import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;

public record TaskResumptionInfo<T>(List<Object> reads, MutableInt numSteps, TaskFactory<T> restarter) {
  TaskResumptionInfo<T> duplicate() {
    return new TaskResumptionInfo<>(new ArrayList<>(reads), new MutableInt(numSteps), restarter);
  }
}
