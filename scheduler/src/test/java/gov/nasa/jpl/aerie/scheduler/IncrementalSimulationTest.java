package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;

public class IncrementalSimulationTest {

  SimulationDriverFacade incrementalSimulationDriver;
  Duration endOfLastAct;

  @BeforeEach
  public void init() {
    final var acts = getActivities();
    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    incrementalSimulationDriver =
        new SimulationDriverFacade(new IncrementalSimulationDriver(fooMissionModel));
    for (var act : acts) {
      final var start = System.nanoTime();
      incrementalSimulationDriver.simulateActivity(act.activity, act.name, act.start);
    }
  }
  @Test
  public void simulationResultsTest(){
    //ensures that simulation results are generated until the end of the last act;
    var simResults = incrementalSimulationDriver.getSimulationResults();
    assert(simResults.realProfiles.get("/utcClock").get(0).getLeft().isEqualTo(endOfLastAct));
    /*ensures that when current simulation results cover more than the asked period and that nothing has happened
    between two requests, the same results are returned*/
    var simResults2 = incrementalSimulationDriver.getSimulationResultsUpTo(Duration.of(7,SECONDS));
    assert(Objects.equals(simResults, simResults2));
  }

  @Test
  public void durationTest(){
    var act1Dur = incrementalSimulationDriver.getActivityDuration("first");
    var act2Dur = incrementalSimulationDriver.getActivityDuration("second");
    assert(act1Dur.isEqualTo(Duration.of(1, SECONDS)));
    assert(act2Dur.isEqualTo(Duration.of(1, SECONDS)));
  }

  private ArrayList<TestSimulatedActivity> getActivities(){
    final var acts = new ArrayList<TestSimulatedActivity>();
    var act1 = new TestSimulatedActivity(
        Duration.of(0, SECONDS),
        new SerializedActivity("BasicActivity", Map.of()),
        "first");
    acts.add(act1);
    var act2 = new TestSimulatedActivity(
        Duration.of(14, SECONDS),
        new SerializedActivity("BasicActivity", Map.of()),
        "second");
    acts.add(act2);

    endOfLastAct = Duration.of(15,SECONDS);
    return acts;
  }


  record TestSimulatedActivity(Duration start, SerializedActivity activity, String name){}


}
