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
    assertEquals(
        "/** Start Codegen */\n"
        + "interface PeelBanana extends ActivityTemplate {}\n"
        + "\n"
        + "export const ActivityTemplates = {\n"
        + "  PeelBanana: function PeelBanana(\n"
        + "    name: string,\n"
        + "    args: {\n"
        + "      duration: AST.Duration,\n"
        + "  fancy: {\n"
        + "    subfield1: string,\n"
        + "    subfield2: {\n"
        + "      subsubfield1: AST.Double,\n"
        + "    }[],\n"
        + "  },\n"
        + "  peelDirection: (\"fromTip\" | \"fromStem\"),\n"
        + "\n"
        + "    }): PeelBanana {\n"
        + "    return {\n"
        + "      name,\n"
        + "      activityType: 'PeelBanana',\n"
        + "      args: args,\n"
        + "    };\n"
        + "  },\n"
        + "}\n"
        + "/** End Codegen */",
        TypescriptCodeGenerationService.generateTypescriptTypesFromMissionModel(MISSION_MODEL_TYPES));
  }
}
