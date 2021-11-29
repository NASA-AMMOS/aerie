package gov.nasa.jpl.aerie.scheduler;


import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import it.univr.di.cstnu.algorithms.STN;
import it.univr.di.cstnu.algorithms.WellDefinitionException;
import it.univr.di.cstnu.graph.LabeledNode;
import it.univr.di.cstnu.graph.STNEdge;
import it.univr.di.cstnu.graph.STNEdgeInt;
import it.univr.di.cstnu.graph.TNGraph;
import org.junit.jupiter.api.Test;

public class TestSTN {


  @Test
  public void testbug() {

    HuginnConfiguration conf = new HuginnConfiguration();

    Time startHorizon = Time.fromString("2030-001T00:00:00.000");
    Time endHorizon = Time.fromString("2030-350T00:00:00.000");

    PlanningHorizon h = new PlanningHorizon(startHorizon,endHorizon);
    conf.setHorizon(h);

    Time swin = Time.fromString("2030-002T00:00:00.000");
    Time ewin = Time.fromString("2030-003T00:00:00.000");
    var encounter = new Windows(Window.between(h.toDur(swin), h.toDur(ewin)));
    var treenc = new TimeRangeExpression.Builder().from(encounter).build();

    var rollGoal = new CoexistenceGoal.Builder()
        .named("RollGoal")
        .forAllTimeIn(h.getHor())
        .forEach(treenc)
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(new ActivityType("ACT"))
                            .duration(Duration.of(1500, Duration.SECONDS))
                            .build())
        .startsAfter(TimeAnchor.START)
        .endsBefore(TimeAnchor.END)
        .build();

    MissionModelWrapper missionModel = new MissionModelWrapper();

    Problem problem = new Problem(missionModel);
    problem.add(rollGoal);


    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, problem);
    Plan plan = solver.getNextSolution().orElseThrow();
    TestUtility.printPlan(plan);

  }

  @Test
  public void test() throws WellDefinitionException {
    TNGraph<STNEdge> graph = new TNGraph<STNEdge>(STNEdgeInt.class);
    STN stn = new STN(graph);
    var edge1 = new STNEdgeInt();
    edge1.setValue(10);
    var edge2 = new STNEdgeInt();
    edge2.setValue(-5);
    var edge3 = new STNEdgeInt();
    edge3.setValue(20);
    var edge4 = new STNEdgeInt();
    edge4.setValue(-20);
    var edge5 = new STNEdgeInt();
    edge5.setValue(10);
    var edge6 = new STNEdgeInt();
    edge6.setValue(-5);
    var edge7 = new STNEdgeInt();
    edge7.setValue(30);
    var edge8 = new STNEdgeInt();
    edge8.setValue(-30);


    var a = new LabeledNode("A");
    var l = new LabeledNode("L");
    var b = new LabeledNode("B");

    var h = new LabeledNode("H");

    graph.addEdge(edge1, a, l);
    graph.addEdge(edge2, l, a);
    graph.addEdge(edge3, l, b);
    graph.addEdge(edge4, b, l);
    graph.addEdge(edge5, b, h);
    graph.addEdge(edge6, h, b);

    graph.addEdge(edge7, a, h);
    graph.addEdge(edge8, h, a);

    stn.consistencyCheck();
    stn.allPairsShortestPaths();


    graph.getEdges();
    System.out.println(graph.getEdges());
    //System.out.println(graph.toString());

  }

}
