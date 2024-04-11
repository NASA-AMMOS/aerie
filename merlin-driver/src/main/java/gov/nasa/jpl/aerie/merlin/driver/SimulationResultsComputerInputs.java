package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.SpanId;
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
    Map<ActivityDirectiveId, SpanId> activityDirectiveIdTaskIdMap){

  public SimulationResults computeResults(final Set<String> resourceNames){
    return SimulationEngine.computeResults(
        this.engine(),
        this.simulationStartTime(),
        this.elapsedTime(),
        this.activityTopic(),
        this.timeline(),
        this.serializableTopics(),
        resourceNames
    );
  }

  public SimulationResults computeResults(){
    return SimulationEngine.computeResults(
        this.engine(),
        this.simulationStartTime(),
        this.elapsedTime(),
        this.activityTopic(),
        this.timeline(),
        this.serializableTopics()
    );
  }

  public SimulationEngine.SimulationActivityExtract computeActivitySimulationResults(){
    return SimulationEngine.computeActivitySimulationResults(
        this.engine(),
        this.simulationStartTime(),
        this.elapsedTime(),
        this.activityTopic(),
        this.timeline(),
        this.serializableTopics());
  }
}
