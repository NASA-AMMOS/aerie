package gov.nasa.jpl.aerie.scheduler.worker.services;

import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SimulationResultsComparisonUtils {

  /**
   * Recursively removes all fields with specific names from a SerializedValue
   * @param serializedValue the serialized value
   * @param fieldsToRemove the names of the fields to remove
   * @return a serialized value without the removed fields
   */
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

  /**
   * Converts the activity instances from a SimulationResults object into a set of tree structure representing parent-child activities for comparison purposes
   * @param simulationResults the simulation results
   * @return a set of trees
   */
  public static Set<TreeSimulatedActivity> convertSimulatedActivitiesToTree(final SimulationResults simulationResults){
    return simulationResults.simulatedActivities.values().stream().map(simulatedActivity -> TreeSimulatedActivity.fromSimulatedActivity(
        simulatedActivity,
        simulationResults)).collect(Collectors.toSet());
  }

  /**
   * Asserts whether two sets of activity instances are equal.
   * @param expected the expected set of activities (as trees)
   * @param actual the actual set of activities (as trees)
   */
  public static void assertEqualsTSA(final Set<TreeSimulatedActivity> expected,
                                     final Set<TreeSimulatedActivity> actual){
    assertEquals(expected.size(), actual.size());
    final var copyExpected = new HashSet<>(expected);
    for(final var inB: actual){
      if(!copyExpected.contains(inB)){
        fail();
      }
      //make sure identical trees are not used to validate twice
      copyExpected.remove(inB);
    }
  }

  // Representation of simulated activities as trees of activities
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

  //Representation of SimulatedActivity stripped of parent/child/directive id information
  //used for comparison purposes
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
