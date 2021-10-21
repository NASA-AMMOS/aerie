package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.Adaptation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SimulationFacadeTest {

 Adaptation<?,?> adaptation;
 Time beginHorizon;
 Time endHorizon;
 Range<Time> planningHorizon;
 MissionModel missionModel;
 SimulationFacade facade;


 @BeforeEach
 public void setUp() throws Exception {
  adaptation = SimulationUtility.getAdaptation();
  beginHorizon = Time.fromMilli(0);
  endHorizon = Time.fromMilli(5000);

  planningHorizon = new Range<Time>(beginHorizon,endHorizon);

  missionModel= new MissionModel(adaptation,planningHorizon);

  facade = new SimulationFacade(planningHorizon, adaptation);
 }

 @AfterEach
 public void tearDown() throws Exception {
  adaptation = null;
  beginHorizon =  null;
  endHorizon = null;
  planningHorizon =  null;
  missionModel=  null;
  facade =  null;
 }

 @Test
 public void boolResourceFetched(){
  var plantRes = facade.getBooleanResource("/flag/conflicted");
  StateConstraintEqual equalCst = StateConstraintExpression.buildEqualConstraint(plantRes, false);
  Plan emptyPlan = new PlanInMemory(missionModel);
  facade.simulatePlan(emptyPlan);
  var winWithoutActs = equalCst.findWindows(emptyPlan, TimeWindows.of(planningHorizon));
  TimeWindows expected = TimeWindows.of(planningHorizon);
  assertEquals(winWithoutActs, expected);
 }

 @Test
 public void stringResourceFetched(){
  var flagRes = facade.getStringResource("/flag");
  StateConstraintEqual equalCst = StateConstraintExpression.buildEqualConstraint(flagRes, "A");
  Plan emptyPlan = new PlanInMemory(missionModel);
  facade.simulatePlan(emptyPlan);
  var winWithoutActs = equalCst.findWindows(emptyPlan, TimeWindows.of(planningHorizon));
  TimeWindows expected = TimeWindows.of(planningHorizon);
  assertEquals(winWithoutActs, expected);
 }

 @Test
 public void intResourceFetched(){
  var plantRes = facade.getIntResource("/plant");
  StateConstraintEqual equalCst = StateConstraintExpression.buildEqualConstraint(plantRes, 200);
  Plan emptyPlan = new PlanInMemory(missionModel);
  facade.simulatePlan(emptyPlan);
  var winWithoutActs = equalCst.findWindows(emptyPlan, TimeWindows.of(planningHorizon));
  TimeWindows expected = TimeWindows.of(planningHorizon);
  assertEquals(winWithoutActs, expected);
 }


 @Test
 public void failsWhenNotSimulated(){
  Plan plan = new PlanInMemory(missionModel);
  var fruitRes = facade.getDoubleResource("/fruit");
  StateConstraintAbove aboveCst = StateConstraintExpression.buildAboveConstraint(fruitRes, 2.9);
  assertThrows(IllegalArgumentException.class, () -> aboveCst.findWindows(plan, TimeWindows.of(planningHorizon)));
 }

 @Test
 public void canCreateConstraints(){
  Plan plan = new PlanInMemory(missionModel);
  var fruitRes = facade.getDoubleResource("/fruit");
  StateConstraintAbove aboveCst = StateConstraintExpression.buildAboveConstraint(fruitRes, 2.9);
  facade.simulatePlan(plan);
  var winWithoutActs = aboveCst.findWindows(plan, TimeWindows.of(planningHorizon));
  TimeWindows expected = TimeWindows.of(planningHorizon);
  assertEquals(winWithoutActs, expected);
 }

 @Test
 public void doubleResourceAreFetchedFromSim() {

  Time t1 = Time.fromMilli(1500);
  var fruitRes = facade.getDoubleResource("/fruit");

  Plan plan = new PlanInMemory(missionModel);

  var actTypeBb = new ActivityType("BiteBanana");
  var actTypePb = new ActivityType("PeelBanana");
  var act1 =new ActivityInstance("PeelBanana1",actTypePb,beginHorizon);
  act1.setParameters(Map.of("peelDirection", "fromStem"));
  var act2 =new ActivityInstance("BiteBanana1",actTypeBb, t1);
  act2.setParameters(Map.of("biteSize", 0.1));
  plan.add(act1);
  plan.add(act2);

  facade.simulatePlan(plan);

  var when = fruitRes.whenValueAbove(2.9, TimeWindows.of(planningHorizon));
  var expected =  TimeWindows.of(new Range<Time>(beginHorizon, t1));
  assertEquals(when,expected);

 }

}
