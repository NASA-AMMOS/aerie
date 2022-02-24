package gov.nasa.jpl.aerie.scheduler.server.services;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.script.ScriptException;

import static org.junit.jupiter.api.Assertions.*;

class TypescriptExecutionServiceTest {

  @Test
  void testExecuteTypescript_1() throws ScriptException {
    final var res = TypescriptExecutionService.executeTypescript("function goal(){ return {abc: \"def\"} }");
    assertEquals(new TypescriptExecutionService.GoalDefinition("abc", "def"), res);
  }

  @Test
  void testExecuteTypescript_2() throws ScriptException {
    final var res = TypescriptExecutionService.executeTypescript("function goal(){ return {abc: \"xyz\"} }");
    assertEquals(new TypescriptExecutionService.GoalDefinition("abc", "xyz"), res);
  }

  @Test
  void testTypescriptExecution() throws ScriptException {
    final var res = TypescriptExecutionService.executeTypescript("function goal(){ const abc = \"ghi\"\nreturn {abc} }");
    assertEquals(new TypescriptExecutionService.GoalDefinition("abc", "ghi"), res);
  }

  @Disabled
  @Test
  /*
   * TODO: Rather than depend on the function being called goal, it would be better to allow the user to specify a function as a default export.
   */
  void testTypescriptModule() throws ScriptException {
    final var res = TypescriptExecutionService.executeTypescript("export default function goal(){ const abc = \"ghi\"\nreturn {abc} }");
    assertEquals(new TypescriptExecutionService.GoalDefinition("abc", "ghi"), res);
  }
}
