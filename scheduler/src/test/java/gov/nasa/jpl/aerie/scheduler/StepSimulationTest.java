package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;

public class StepSimulationTest {

  @Test
  public void resetEveryTime() {
    manyacts(true);
  }
  @Test
  public void dontreset() {
    manyacts(false);
  }

  public void manyacts(boolean reset){
    var cur = Duration.ZERO;
    final var step = Duration.of(3,SECOND);
    // ten days
    final var durSim = Duration.of(864000, SECOND);

    final var acts = new ArrayList<Triple<Duration, SerializedActivity, String>>();
    for(; cur.shorterThan(durSim); cur = cur.plus(step) ){
      var act = Triple.of(
          cur,
          new SerializedActivity("BasicActivity", Map.of()), String.valueOf(cur));
      acts.add(act);
    }
    var bananaMissionModel = SimulationUtility.getFooMissionModel();

    StepSimulation stepSimulation = new StepSimulation(bananaMissionModel);
    stepSimulation.initSimulation();
    if(reset) {
      stepSimulation.resetEveryTime();
    }
    System.out.println(acts.size() + " activities to simulate");
    int i = 0;
    var curMean = 0.;
    var sum = 0.;
    for(var act : acts) {
      //System.out.println("Sim act " + (i++));
      var start = System.nanoTime();
      stepSimulation.simulateActivity(act.getMiddle(), act.getLeft(), act.getRight());
      stepSimulation.getActivityDuration(act.getMiddle());
      var dur = System.nanoTime() - start;
      i++;
      sum+=dur;
      curMean = sum/i;
      System.out.println(i + " " + curMean);
      if(i>10000){
        break;
      }
    }
  }

}
