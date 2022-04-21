package gov.nasa.jpl.aerie.merlin.server.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypescriptCodeGenerationServiceTest {

  @Test
  void testCodeGen() {
    assertEquals(
        """
            /** Start Codegen */
            /** End Codegen */""",
        TypescriptCodeGenerationService.generateTypescriptTypesFromMissionModel());
  }
}
