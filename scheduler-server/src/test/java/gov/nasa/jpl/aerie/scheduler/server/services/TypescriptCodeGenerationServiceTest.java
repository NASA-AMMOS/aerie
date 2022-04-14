package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        """
            /** Start Codegen */
            import type { ActivityTemplate } from './scheduler-edsl-fluent-api.js';
            interface PeelBanana extends ActivityTemplate {}
            export const ActivityTemplates = {
              PeelBanana: function PeelBanana(
                args: {
                  duration: Duration,
                  fancy: { subfield1: string, subfield2: { subsubfield1: Double, }[], },
                  peelDirection: ("fromTip" | "fromStem"),
                }): PeelBanana {
                  return { activityType: 'PeelBanana', args };
                },
            }
            declare global {
              var ActivityTemplates: {
                PeelBanana: (
                  args: {
                    duration: Duration,
                    fancy: { subfield1: string, subfield2: { subsubfield1: Double, }[], },
                    peelDirection: ("fromTip" | "fromStem"),
                  }) => PeelBanana
              }
            };
            // Make ActivityTemplates available on the global object
            Object.assign(globalThis, { ActivityTemplates });
            /** End Codegen */""",
        TypescriptCodeGenerationService.generateTypescriptTypesFromMissionModel(MISSION_MODEL_TYPES));
  }
}
