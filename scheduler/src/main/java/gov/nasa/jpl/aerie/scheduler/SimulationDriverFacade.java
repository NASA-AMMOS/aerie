package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SimulationDriverFacade {

  private final IncrementalSimulationDriver driver;

  private final Map<String, ActivityInstanceId> activityNameToActivityId = new HashMap<>();

  private static long count = 0;

  public SimulationDriverFacade(IncrementalSimulationDriver driver){
    this.driver = driver;
  }

  public void simulateActivity(SerializedActivity activity, String nameAct, Duration startTime){
    final var activityId = new ActivityInstanceId(count++);
    activityNameToActivityId.put(nameAct, activityId);
    driver.simulateActivity(activity,startTime, activityId);
  }

  public Optional<Duration> getActivityDuration(String activityName){
    var activityId = activityNameToActivityId.get(activityName);
    return driver.getActivityDuration(activityId);
  }

  public SimulationResults getSimulationResults(){
    return driver.getSimulationResults();
  }

  public SimulationResults getSimulationResultsUntil(Duration endTime){
    return driver.getSimulationResultsUntil(endTime);
  }

}
