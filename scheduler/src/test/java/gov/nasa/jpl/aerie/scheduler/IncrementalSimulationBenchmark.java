package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;

public class IncrementalSimulationBenchmark {

  /**
   * This benchmark builds incrementally longer plans of sequential non-overlapping activities and simulates them with
   * incremental and non-incremental simulation drivers. Each printed timing _left _right shows the time _right it took
   * to simulate a plan with a number of _left activities. Note that this is an ideal case for the incremental driver as
   * it never has to reset.
   */
  public static void main(String[] args){
    System.out.println("Incremental");
    benchmarkIncrementalSimulationDriver();
    System.out.println("Non-incremental");
    benchMarkSimulationDriver();
  }

  private static ArrayList<IncrementalSimulationTest.TestSimulatedActivity> getActivities(){
    final int nbActs = 5000;
    final var step = Duration.of(3,SECOND);
    final var durSim = step.times(nbActs);
    final var acts = new ArrayList<IncrementalSimulationTest.TestSimulatedActivity>();
    for(var cur = Duration.ZERO; cur.shorterThan(durSim); cur = cur.plus(step) ){
      var act = new IncrementalSimulationTest.TestSimulatedActivity(
          cur,
          new SerializedActivity("BasicActivity", Map.of()),
          new ActivityInstanceId(cur.in(MICROSECONDS)));
      acts.add(act);
    }
    return acts;
  }


  private static void benchMarkSimulationDriver(){
    final var acts = getActivities();
    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    int i = 0;
    var sum = 0.;
    final var alreadyIn = new HashMap<ActivityInstanceId, Pair<Duration, SerializedActivity>>();
    //builds incrementally long plans and simulates them
    for(var act:acts){
      alreadyIn.put(act.id(), Pair.of(act.start(), act.activity()));
      final var task = SimulationDriver.buildPlanTask(alreadyIn);
      final var start = System.nanoTime();
      SimulationDriver.simulateTask(fooMissionModel, task);
      final var dur = System.nanoTime() - start;
      sum += dur;
      final var curMean = sum / (++i);
      System.out.println(i + " " + curMean);
    }
  }

  private static void benchmarkIncrementalSimulationDriver(){
    final var acts = getActivities();
    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    final var incrementalSimulationDriver = new IncrementalSimulationDriver(fooMissionModel);
    int i = 0;
    var sum = 0.;
    for(var act : acts) {
      final var start = System.nanoTime();
      incrementalSimulationDriver.simulateActivity(act.activity(), act.start(), act.id());
      final var dur = System.nanoTime() - start;
      sum += dur;
      final var curMean = sum / (++i);
      System.out.println(i + " " + curMean);
    }
  }


}
