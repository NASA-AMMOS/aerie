package gov.nasa.jpl.aerie.scheduler.worker.services;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
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
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SimulationResultsComparisonUtils {

  final static boolean ACTIVATE_LOCAL_DUMP = false;
  final static int MODEL_ID = 87;

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
      if(ACTIVATE_LOCAL_DUMP) dumpPlanToLocalAerie(MODEL_ID, planForDebug, expected.startTime, expected.duration);
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
    final var uploadedPlan = aerieService.createActivityDirectives(id, plan.entrySet().stream().map((entry) -> fromActivityDirective(entry.getKey(), entry.getValue())).toList(), Map.of());
  }

  public static SerializedValue removeFieldsFromSerializedValue(
      SerializedValue serializedValue,
      final Collection<String> fieldsToRemove){
    final var visitor = new SerializedValue.Visitor<SerializedValue>(){
      @Override
      public SerializedValue onNull() {
        return SerializedValue.NULL;
      }

      @Override
      public SerializedValue onNumeric(final BigDecimal value) {
        return SerializedValue.of(value);
      }

      @Override
      public SerializedValue onBoolean(final boolean value) {
        return SerializedValue.of(value);
      }

      @Override
      public SerializedValue onString(final String value) {
        return SerializedValue.of(value);
      }

      @Override
      public SerializedValue onMap(final Map<String, SerializedValue> value) {
        final var newVal = new HashMap<String, SerializedValue>();
        for(final var entry: value.entrySet()){
          if(!fieldsToRemove.contains(entry.getKey())){
            newVal.put(entry.getKey(), removeFieldsFromSerializedValue(entry.getValue(), fieldsToRemove));
          }
        }
        return SerializedValue.of(newVal);
      }

      @Override
      public SerializedValue onList(final List<SerializedValue> value) {
        final var newList = new ArrayList<SerializedValue>();
        for(final var val : value){
          newList.add(removeFieldsFromSerializedValue(val, fieldsToRemove));
        }
        return SerializedValue.of(newList);
      }
    };
    return serializedValue.match(visitor);
  }

  public static Set<TreeSimulatedActivity> convertSimulatedActivitiesToTree(final SimulationResults simulationResults){
    return simulationResults.simulatedActivities.values().stream().map(simulatedActivity -> TreeSimulatedActivity.fromSimulatedActivity(
        simulatedActivity,
        simulationResults)).collect(Collectors.toSet());
  }

  public static void assertEqualsTSA(final Set<TreeSimulatedActivity> expected,
                                     final Set<TreeSimulatedActivity> actual){
    assertEquals(expected.size(), actual.size());
    for(final var inB: actual){
      if(!expected.contains(inB)){
        fail();
      }
    }
  }

  public record TreeSimulatedActivity(StrippedSimulatedActivity activity,
                                      Set<TreeSimulatedActivity> children){
    public static TreeSimulatedActivity fromSimulatedActivity(SimulatedActivity simulatedActivity, SimulationResults simulationResults){
      final var stripped = StrippedSimulatedActivity.fromSimulatedActivity(simulatedActivity);
      final HashSet<TreeSimulatedActivity> children = new HashSet<>();
      for(final var childId: simulatedActivity.childIds()) {
        final var child = fromSimulatedActivity(simulationResults.simulatedActivities.get(childId), simulationResults);
        children.add(child);
      }
      return new TreeSimulatedActivity(stripped, children);
    }
  }

  public record StrippedSimulatedActivity(
      String type,
      Map<String, SerializedValue> arguments,
      Instant start,
      Duration duration,
      SerializedValue computedAttributes
  ){
    public static StrippedSimulatedActivity fromSimulatedActivity(SimulatedActivity simulatedActivity){
      return new StrippedSimulatedActivity(
          simulatedActivity.type(),
          simulatedActivity.arguments(),
          simulatedActivity.start(),
          simulatedActivity.duration(),
          simulatedActivity.computedAttributes()
      );
    }
  }
}
