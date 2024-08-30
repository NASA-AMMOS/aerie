package gov.nasa.jpl.aerie.scheduler.solver.stn;

import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.alg.shortestpath.NegativeCycleDetectedException;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Representation of a simple temporal network (Dechter, Meiri, and Pearl, 1991).
 * Set of timepoint variables with linear constraints between them
 * Classical constraint programming setting where
 * - Each variable has a domain
 * - We want to propagate the constraints to find a solution to the problem or ensure there is not one
 *
 * To solve the constraints, we use bellman-ford algorithm.
 * if a negative cycle is detected during propagation, the network is infeasible
 * otherwise, the new variable domains are updated and can be queried. Domains here represent the flexibility associated
 * with each timepoint.
 *
 */
public class STN {

  private static final Logger logger = LoggerFactory.getLogger(STN.class);

  public String toDOT() {
    final var graphAsDot = new ByteArrayOutputStream();
    // Export to DOT format
    DOTExporter<String, DefaultWeightedEdge> exporter = new DOTExporter<>();
    // Vertex ID and label providers
    exporter.setVertexIdProvider(v -> v);
    exporter.setVertexAttributeProvider(v -> Map.of("label", DefaultAttribute.createAttribute(v)));
    // Edge attribute provider for weights
    exporter.setEdgeAttributeProvider(e -> Map.of("label", DefaultAttribute.createAttribute(String.valueOf(graph.getEdgeWeight(e)))));
    exporter.exportGraph(this.graph, graphAsDot);
    return graphAsDot.toString();
  }

  private final Graph<String, DefaultWeightedEdge> graph;

  private BellmanFordShortestPath<String, DefaultWeightedEdge> latestComputation;

  public STN() {
    graph = GraphTypeBuilder
        .<String, DefaultWeightedEdge>directed()
        .allowingMultipleEdges(false)
        .allowingSelfLoops(false)
        .edgeClass(DefaultWeightedEdge.class)
        .weighted(true)
        .buildGraph();
  }

  /**
   * tp1 is before tp2
   * equivalent to the constraint
   * tp1 --- [0, +inf] ---> tp2
   Maps to two edges in a distance graph
   i --- +inf ---> j
   i {@literal <}--- -0 --- j

   we can remove the first one and keep only the second one
   */
  public void addBeforeCst(String tp1, String tp2){
    var e1 = getOrCreateEdge(tp2, tp1);
    graph.setEdgeWeight(e1, -0);
  }

  public void removeTimepoint(String tp1){
    graph.removeVertex(tp1);
  }

  /*
  Adds the constraint
  i --- [a, b] ---> j
  Maps to two edges in a distance graph
  i --- b ---> j
  i <--- -a --- j */
  public void addDurCst(String tp1, String tp2, double min, double max) {
    var e1 = getOrCreateEdge(tp2, tp1);
    var e2 = getOrCreateEdge(tp1, tp2);

    graph.setEdgeWeight(e1, -min);
    graph.setEdgeWeight(e2, max);

  }

  public Pair<Double, Double> getDurCst(String a, String b){
    failIfUpdateNotLaunched();
    failIfTimepointAbsent(a);
    failIfTimepointAbsent(b);
    return Pair.of(-getDist(b, a), getDist(a, b));
  }

  public void addTimepoint(String tp){
    graph.addVertex(tp);
  }

  public boolean update() {
    boolean ret = false;
    BellmanFordShortestPath<String, DefaultWeightedEdge> algo = null;
    if(graph.vertexSet().isEmpty()){
      return ret;
    }
    try {
      algo = new BellmanFordShortestPath<>(graph);
      algo.getPaths(graph.vertexSet().iterator().next());
      ret = true;
    } catch (NegativeCycleDetectedException e) {
      logger.debug("Negative cycle ", e); //this is normal behavior, shouldn't be flagged as an error! If debugging is on, drop the stack trace.
    }
    latestComputation = algo;
    return ret;
  }

  /**
   * gets the weight on link a-->b
   */
  public double getDist(String a, String b){
    failIfUpdateNotLaunched();
    return latestComputation.getPathWeight(a, b);
  }

  private DefaultWeightedEdge getOrCreateEdge(String a, String b) {
    failIfTimepointAbsent(a);
    failIfTimepointAbsent(b);

    var edge = graph.getEdge(a, b);
    if (edge == null) {
      edge = graph.addEdge(a, b);
    }
    return edge;
  }

  private void failIfTimepointAbsent(String tp){
    if(!graph.vertexSet().contains(tp)){
      throw new IllegalArgumentException("Timepoint is not present in temporal network, insert it before use");
    }
  }

  private void failIfUpdateNotLaunched(){
    if(latestComputation == null){
      throw new IllegalArgumentException("Must call update() before getting results");
    }
  }

  public List<String> orderNodesRootToLeaf() {
    List<String> result = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    Set<String> inProcess = new HashSet<>();

    // Start with root nodes
    Set<String> rootNodes = findRootNodes();
    for (String node : rootNodes) {
      dfsVisit(node, result, visited, inProcess);
    }

    // Process any remaining nodes (in case of cycles or disconnected components)
    for (String node : graph.vertexSet()) {
      if (!visited.contains(node)) {
        dfsVisit(node, result, visited, inProcess);
      }
    }

    return result;
  }

  private void dfsVisit(String node, List<String> result, Set<String> visited, Set<String> inProcess) {
    if (inProcess.contains(node)) {
      // Cycle detected, skip this node
      return;
    }
    if (visited.contains(node)) {
      return;
    }

    inProcess.add(node);

    for (DefaultWeightedEdge edge : graph.outgoingEdgesOf(node)) {
      dfsVisit(graph.getEdgeTarget(edge), result, visited, inProcess);
    }

    inProcess.remove(node);
    visited.add(node);
    result.add(0, node); // Add to the beginning of the list
  }

  public Set<String> findRootNodes() {
    Set<String> rootNodes = new HashSet<>();
    for (String vertex : graph.vertexSet()) {
      if (graph.inDegreeOf(vertex) == 0) {
        rootNodes.add(vertex);
      }
    }
    return rootNodes;
  }

  public boolean hasCycle() {
    CycleDetector<String, DefaultWeightedEdge> cycleDetector = new CycleDetector<>(graph);
    return cycleDetector.detectCycles();
  }

  public boolean hasOrphans() {
    for (String vertex : graph.vertexSet()) {
      if (graph.incomingEdgesOf(vertex).isEmpty() && graph.outgoingEdgesOf(vertex).isEmpty()) {
        return true; // Found an orphan
      }
    }
    return false; // No orphans found
  }

  public boolean hasMultipleStartingNodes() {
    int startingNodeCount = 0;
    for (String vertex : graph.vertexSet()) {
      if (graph.incomingEdgesOf(vertex).isEmpty()) {
        startingNodeCount++;
        if (startingNodeCount > 1) {
          return true; // More than one starting node found
        }
      }
    }
    return false; // Zero or one starting node found
  }

}
