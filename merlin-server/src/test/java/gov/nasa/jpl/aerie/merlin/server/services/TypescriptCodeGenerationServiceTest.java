package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.mocks.StubMissionModelService;
import gov.nasa.jpl.aerie.merlin.server.mocks.StubPlanService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TypescriptCodeGenerationServiceTest {

  @Test
  void testCodeGen() throws MissionModelService.NoSuchMissionModelException, NoSuchPlanException {
    final var codeGenService = new TypescriptCodeGenerationServiceAdapter(new StubMissionModelService(), new StubPlanService());

    assertEquals(
        """
           /** Start Codegen */
           import * as AST from './constraints-ast.js';
           import { Discrete, Real, Windows } from './constraints-edsl-fluent-api.js';
           export enum ActivityType {
             activity2 = "activity2",
             activity = "activity",
           }
           export type Resource = {
             "mode": ( | "Option1" | "Option2"),
             "state of charge": {initial: number, rate: number, },
             "an integer": number,
           };
           export type ResourceName = "mode" | "state of charge" | "an integer";
           export type RealResourceName = "state of charge" | "an integer";
           export const ActivityTypeParameterMap = {
             [ActivityType.activity2]: (alias: string) => ({
               "Param": new Discrete<( | "hello" | "there")>({
                 kind: AST.NodeKind.DiscreteProfileParameter,
                 alias,
                 name: "Param"
               }),
             }),
             [ActivityType.activity]: (alias: string) => ({
               "Param": new Discrete<string>({
                 kind: AST.NodeKind.DiscreteProfileParameter,
                 alias,
                 name: "Param"
               }),
               "AnotherParam": new Real({
                 kind: AST.NodeKind.RealProfileParameter,
                 alias,
                 name: "AnotherParam"
               }),
             }),
           };
           declare global {
             enum ActivityType {
               activity2 = "activity2",
               activity = "activity",
             }
           }
           Object.assign(globalThis, {
             ActivityType
           });
           /** End Codegen */""",
        codeGenService.generateTypescriptTypes("abc", Optional.of(new PlanId(1L)))
    );
  }
}
