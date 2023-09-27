package gov.nasa.jpl.aerie.scheduler.worker.services;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.scheduler.server.services.GraphQLMerlinService;
import gov.nasa.jpl.aerie.scheduler.server.services.MerlinServiceException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.scheduler.worker.services.SimulationResultsComparisonUtils.assertEqualsTSA;
import static gov.nasa.jpl.aerie.scheduler.worker.services.SimulationResultsComparisonUtils.convertSimulatedActivitiesToTree;
import static gov.nasa.jpl.aerie.scheduler.worker.services.SimulationResultsComparisonUtils.removeFieldsFromSerializedValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Clipper-testing specific utils
 */
public class ClipperSimulationComparisonUtils {
  final static boolean ACTIVATE_LOCAL_DUMP = false;
  final static int LOCAL_DUMP_MODEL_ID = 87;

  public static void assertEqualsSimResultsClipper(final SimulationResults expected, final SimulationResults simulationResults2, final Map<ActivityDirectiveId, ActivityDirective> planForDebug)
  throws MerlinServiceException, NoSuchPlanException, URISyntaxException, IOException
  {
    assertEquals(expected.unfinishedActivities, simulationResults2.unfinishedActivities);
    assertEquals(expected.topics, simulationResults2.topics);
    assertEqualsTSA(convertSimulatedActivitiesToTree(expected), convertSimulatedActivitiesToTree(simulationResults2));
    final var differencesDiscrete = new HashMap<String, Map<Integer, DiscreteProfileDifference>>();
    for(final var discreteProfile: simulationResults2.discreteProfiles.entrySet()){
      final var filteredActualProfileElements = new ArrayList<ProfileSegment<SerializedValue>>();
      discreteProfile.getValue().getRight().forEach(a -> filteredActualProfileElements.add(new ProfileSegment<>(a.extent(), removeFieldsFromSerializedValue(a.dynamics(), List.of("uuid")))));
      final var filteredExpectedProfileElements = new ArrayList<ProfileSegment<SerializedValue>>();
      expected.discreteProfiles.get(discreteProfile.getKey()).getRight().forEach(a -> filteredExpectedProfileElements.add(new ProfileSegment<>(a.extent(), removeFieldsFromSerializedValue(a.dynamics(), List.of("uuid")))));
      final var differences = equalsDiscreteProfile(filteredExpectedProfileElements, filteredActualProfileElements);
      if(!differences.isEmpty()){
        differencesDiscrete.put(discreteProfile.getKey(), differences);
      }
    }
    final var differencesReal = new HashMap<String, Map<Integer, RealProfileDifference>>();
    for(final var realProfile: simulationResults2.realProfiles.entrySet()){
      final var profileElements = realProfile.getValue().getRight();
      final var expectedProfileElements = expected.realProfiles.get(realProfile.getKey()).getRight();
      final var differences = equalsRealProfile(expectedProfileElements, profileElements);
      if(!differences.isEmpty()) {
        differencesReal.put(realProfile.getKey(), differences);
      }
    }
    if(!differencesDiscrete.isEmpty() || !differencesReal.isEmpty()){
      System.out.println("Differences in real profiles: " + differencesReal);
      System.out.println("Differences in discrete profiles " + differencesDiscrete);
      if(ACTIVATE_LOCAL_DUMP) dumpPlanToLocalAerie(LOCAL_DUMP_MODEL_ID, planForDebug, expected.startTime, expected.duration);
      fail();
    }
  }

  public record RealProfileDifference(ProfileSegment<RealDynamics> expected, ProfileSegment<RealDynamics> actual){}
  public record DiscreteProfileDifference(ProfileSegment<SerializedValue> expected, ProfileSegment<SerializedValue> actual){}

  public static Map<Integer, RealProfileDifference> equalsRealProfile(List<ProfileSegment<RealDynamics>> expected, List<ProfileSegment<RealDynamics>> actual){
    final var differences = new HashMap<Integer, RealProfileDifference>();
    for(int i = 0; i < expected.size(); i++){
      if(!actual.get(i).equals(expected.get(i))){
        differences.put(i, new RealProfileDifference(expected.get(i), actual.get(i)));
      }
    }
    return differences;
  }
  public static Map<Integer, DiscreteProfileDifference> equalsDiscreteProfile(List<ProfileSegment<SerializedValue>> expected, List<ProfileSegment<SerializedValue>> actual){
    final var differences = new HashMap<Integer, DiscreteProfileDifference>();
    for(int i = 0; i < expected.size(); i++){
      if(!actual.get(i).equals(expected.get(i))){
        differences.put(i, new DiscreteProfileDifference(expected.get(i), actual.get(i)));
      }
    }
    return differences;
  }

  public static SchedulingActivityDirective fromActivityDirective(ActivityDirectiveId activityDirectiveId, ActivityDirective activityDirective){
    return new SchedulingActivityDirective(new SchedulingActivityDirectiveId(activityDirectiveId.id()),
                                           new ActivityType(activityDirective.serializedActivity().getTypeName()),
                                           activityDirective.startOffset(),
                                           null,
                                           activityDirective.serializedActivity().getArguments(),
                                           null,
                                           activityDirective.anchorId() == null ? null : new SchedulingActivityDirectiveId(activityDirective.anchorId().id()),
                                           activityDirective.anchoredToStart());
  }

  public static void dumpPlanToLocalAerie(int modelId, Map<ActivityDirectiveId, ActivityDirective> plan, Instant startTime, Duration duration)
  throws URISyntaxException, MerlinServiceException, NoSuchPlanException, IOException
  {
    final var aerieService = new GraphQLMerlinService(new URI("http://localhost:8080/v1/graphql"), "aerie");
    final var id = aerieService.createEmptyPlan("dump_duplicate_test" +new Date().toString(), modelId, startTime, duration);
    aerieService.createActivityDirectives(id, plan.entrySet().stream().map((entry) -> fromActivityDirective(entry.getKey(), entry.getValue())).toList(), Map.of());
  }
}
