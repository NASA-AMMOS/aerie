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
            type ActivityTypeName =
              | "activity"
              ;
            /** End Codegen */""",
        codeGenService.generateTypescriptTypesFromMissionModel("abc")
    );
  }
}
