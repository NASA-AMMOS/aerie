package gov.nasa.jpl.aerie.scheduler;

import org.jgrapht.alg.cycle.SzwarcfiterLauerSimpleCycles;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.alg.shortestpath.NegativeCycleDetectedException;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestGraphT {
  private boolean hasNegativeLoop(DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph) {
    SzwarcfiterLauerSimpleCycles<String, DefaultWeightedEdge> cycleDetector = new SzwarcfiterLauerSimpleCycles<>(graph);

    List<List<String>> cycles = cycleDetector.findSimpleCycles();

    for (List<String> cycle : cycles) {
      double cycleWeight = getCycleWeight(graph, cycle);

      if (cycleWeight < 0) return true;
    }

    return false;
  }

  private double getCycleWeight(DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph, List<String> cycle) {
    double totalWeight = 0;

    for (int i = 1; i < cycle.size(); i++) {
      double weight = graph.getEdgeWeight(graph.getEdge(cycle.get(i - 1), cycle.get(i)));

      totalWeight += weight;
    }

    double weightBackToStart = graph.getEdgeWeight(graph.getEdge(cycle.get(cycle.size() - 1), cycle.get(0)));

    return totalWeight + weightBackToStart;
  }

  @Test
  public void test() {

    long ss = 11;
    long se = 11;
    long es = 12;
    long ee = 15;
    long ds = 3;
    long de = 3;


    SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> g =
        new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
    g.addVertex("o");
    g.addVertex("b");
    g.addVertex("e");

    var ob = g.addEdge("o", "b");
    g.setEdgeWeight(ob, se);

    var bo = g.addEdge("b", "o");
    g.setEdgeWeight(bo, -ss);

    var oe = g.addEdge("o", "e");
    g.setEdgeWeight(oe, ee);

    var eo = g.addEdge("e", "o");
    g.setEdgeWeight(eo, -es);

    var be = g.addEdge("b", "e");
    g.setEdgeWeight(be, de);

    var eb = g.addEdge("e", "b");
    g.setEdgeWeight(eb, -ds);

    try {
      BellmanFordShortestPath<String, DefaultWeightedEdge> algo =
          new BellmanFordShortestPath<>(g);
      System.out.println(algo.getPathWeight("o", "b"));
      System.out.println(algo.getPathWeight("b", "o"));

    } catch (NegativeCycleDetectedException e) {
      System.out.println("Error");
    }


  }


}
