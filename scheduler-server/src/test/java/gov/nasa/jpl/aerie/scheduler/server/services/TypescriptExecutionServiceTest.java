package gov.nasa.jpl.aerie.scheduler.server.services;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.script.ScriptException;

import static org.junit.jupiter.api.Assertions.*;

class TypescriptExecutionServiceTest {

  @Test
  void testExecuteTypescript_1() throws TypescriptExecutionService.TypescriptExecutionException,
                                        TypescriptExecutionService.TypescriptCompilationException
  {
    final var res = TypescriptExecutionService.executeTypescript("function goal(){ return {abc: \"def\"} }; JSON.stringify(goal())");
    assertEquals("{\"abc\":\"def\"}", res);
  }

  @Test
  void testExecuteTypescript_2() throws TypescriptExecutionService.TypescriptExecutionException,
                                        TypescriptExecutionService.TypescriptCompilationException
  {
    final var res = TypescriptExecutionService.executeTypescript("function goal(){ return {abc: \"xyz\"} }; JSON.stringify(goal())");
    assertEquals("{\"abc\":\"xyz\"}", res);
  }

  @Test
  void testTypescriptExecution() throws TypescriptExecutionService.TypescriptExecutionException,
                                        TypescriptExecutionService.TypescriptCompilationException
  {
    final var res = TypescriptExecutionService.executeTypescript("function goal(){ const abc = \"ghi\"\nreturn {abc} }; JSON.stringify(goal())");
    assertEquals("{\"abc\":\"ghi\"}", res);
  }

  @Disabled
  @Test
  /*
   * TODO: Rather than depend on the function being called goal, it would be better to allow the user to specify a function as a default export.
   */
  void testTypescriptModule() throws TypescriptExecutionService.TypescriptExecutionException,
                                     TypescriptExecutionService.TypescriptCompilationException
  {
    final var res = TypescriptExecutionService.executeTypescript("export default function goal(){ const abc = \"ghi\"\nreturn {abc} }");
    assertEquals(new SchedulingGoalDSLCompilationService.GoalDefinition("abc", "ghi"), res);
  }
}
