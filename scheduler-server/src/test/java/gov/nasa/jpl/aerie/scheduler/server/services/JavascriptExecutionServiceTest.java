package gov.nasa.jpl.aerie.scheduler.server.services;

import org.junit.jupiter.api.Test;

import javax.script.ScriptException;

import static org.junit.jupiter.api.Assertions.*;

class JavascriptExecutionServiceTest {

  @Test
  void testExecuteJavascript_1() throws ScriptException {
    final var res = JavascriptExecutionService.executeJavascript("function foo(){ return {abc: \"def\"} }");
    assertEquals(res, new JavascriptExecutionService.GoalDefinition("abc", "def"));
  }

  @Test
  void testExecuteJavascript_2() throws ScriptException {
    final var res = JavascriptExecutionService.executeJavascript("function foo(){ return {abc: \"xyz\"} }");
    assertEquals(res, new JavascriptExecutionService.GoalDefinition("abc", "xyz"));
  }
}
