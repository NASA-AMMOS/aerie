package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TypescriptCodeGenerationServiceTest {

  public static final TypescriptCodeGenerationService.MissionModelTypes MISSION_MODEL_TYPES =
      new TypescriptCodeGenerationService.MissionModelTypes(
          List.of(new TypescriptCodeGenerationService.ActivityType(
              "PeelBanana",
              Map.of(
                  "peelDirection",
                  ValueSchema.ofVariant(List.of(
                      new ValueSchema.Variant(
                          "fromTip", "fromTip"
                      ),
                      new ValueSchema.Variant(
                          "fromStem",
                          "fromStem"))),
                  "duration",
                  ValueSchema.DURATION,
                  "fancy",
                  ValueSchema.ofStruct(Map.of(
                      "subfield1",
                      ValueSchema.STRING,
                      "subfield2",
                      ValueSchema.ofSeries(ValueSchema.ofStruct(Map.of("subsubfield1", ValueSchema.REAL)))
                  ))))),
          null
      );

  @Test
  void testCodeGen() {
    assertEquals("/** Start Codegen */\n"
                 + "interface PeelBanana extends ActivityTemplate {}\n"
                 + "export const ActivityTemplates = {\n"
                 + "  PeelBanana: function PeelBanana(\n"
                 + "    name: string\n"
                 + "    args: {\n"
                 + "      duration: Duration\n"
                 + "      fancy: { subfield1: string, subfield2: { subsubfield1: Double, }[], }\n"
                 + "      peelDirection: (\"fromTip\" | \"fromStem\")\n"
                 + "    }): PeelBanana {\n"
                 + "      return { name, activityType: 'PeelBanana', args };\n"
                 + "    },\n"
                 + "}\n"
                 + "/** End Codegen */",
        TypescriptCodeGenerationService.generateTypescriptTypesFromMissionModel(MISSION_MODEL_TYPES));
  }
}
