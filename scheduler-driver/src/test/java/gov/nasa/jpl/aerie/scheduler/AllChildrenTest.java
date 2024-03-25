package gov.nasa.jpl.aerie.scheduler;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AllChildrenTest {
  @Test
  public void testAllChildrenAnnotation(){
    final var bananaSchedulerModel = SimulationUtility.getBananaSchedulerModel();
    final var children = bananaSchedulerModel.getChildren();
    final var bananaMissionModel = SimulationUtility.getBananaMissionModel();
    final var allActivityTypes = bananaMissionModel.getDirectiveTypes().directiveTypes().keySet();
    //there is one key per activity type in the children map
    allActivityTypes.forEach(at -> assertTrue(children.containsKey(at)));
    //the only declared child of parent is the one present in the children map
    assertEquals(List.of("child"), children.get("parent"));
    //the declared absence of children of grandchild is reported in the children map
    assertEquals(List.of(), children.get("grandchild"));
    //an activity without effect model does not have any children
    assertEquals(List.of(), children.get("ParameterTest"));
    //an activity with an effect model but with no @AllChildren annotation will result in all activity types of the mission model being reported as its potential children
    assertEquals(new HashSet<>(allActivityTypes.stream().toList()), new HashSet<>(children.get("BiteBanana")));
  }
}
