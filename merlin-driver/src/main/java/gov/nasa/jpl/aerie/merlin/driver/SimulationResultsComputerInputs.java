package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record SimulationResultsComputerInputs(
    SimulationEngine engine,
    Instant simulationStartTime,
    Duration elapsedTime,
    Topic<ActivityDirectiveId> activityTopic,
    TemporalEventSource timeline,
    Iterable<MissionModel.SerializableTopic<?>> serializableTopics,
    Map<ActivityDirectiveId, TaskId> activityDirectiveIdTaskIdMap){

  public static SimulationResults computeResults(
      final SimulationResultsComputerInputs simulationResultsInputs,
      final Set<String> resourceNames){
    return SimulationEngine.computeResults(
        simulationResultsInputs.engine(),
        simulationResultsInputs.simulationStartTime(),
        simulationResultsInputs.elapsedTime(),
        simulationResultsInputs.activityTopic(),
        simulationResultsInputs.timeline(),
        simulationResultsInputs.serializableTopics(),
        resourceNames
    );
  }

  public static SimulationResults computeResults(
      final SimulationResultsComputerInputs simulationResultsInputs){
    return SimulationEngine.computeResults(
        simulationResultsInputs.engine(),
        simulationResultsInputs.simulationStartTime(),
        simulationResultsInputs.elapsedTime(),
        simulationResultsInputs.activityTopic(),
        simulationResultsInputs.timeline(),
        simulationResultsInputs.serializableTopics()
    );
  }

  public static SimulationEngine.SimulationActivityExtract computeActivitySimulationResults(
      final SimulationResultsComputerInputs simulationResultsInputs){
    return SimulationEngine.computeActivitySimulationResults(
        simulationResultsInputs.engine(),
        simulationResultsInputs.simulationStartTime(),
        simulationResultsInputs.elapsedTime(),
        simulationResultsInputs.activityTopic(),
        simulationResultsInputs.timeline(),
        simulationResultsInputs.serializableTopics());
  }
}
