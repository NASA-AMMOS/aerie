package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.mocks.StubMissionModelService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypescriptCodeGenerationServiceTest {

  @Test
  void testCodeGen() throws MissionModelService.NoSuchMissionModelException {
    final var codeGenService = new TypescriptCodeGenerationService(new StubMissionModelService());

    assertEquals(
        """
            /** Start Codegen */
            export type ActivityTypeName =
              | "activity"
              ;
            export type ResourceName = "mode" | "state of charge";
            export type DiscreteResourceSchema<R extends ResourceName> =
              R extends "mode" ? ( | "Option1" | "Option2") :
              R extends "state of charge" ? number :
              void;
            export function discreteResourceSchemaDummyValue<R extends ResourceName>(resource: R): DiscreteResourceSchema<R> {
              return (
                resource === "mode" ? "Option1" :
                resource === "state of charge" ? 1.0 :
                undefined
              ) as DiscreteResourceSchema<R>;
            }
            export type RealResourceName = "state of charge";
            /** End Codegen */""",
        codeGenService.generateTypescriptTypesFromMissionModel("abc")
    );
  }
}
