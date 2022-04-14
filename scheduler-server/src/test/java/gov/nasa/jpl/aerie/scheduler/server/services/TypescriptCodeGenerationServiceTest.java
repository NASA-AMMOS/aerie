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
              "SampleActivity1",
              Map.of(
                  "variant",
                  ValueSchema.ofVariant(List.of(
                      new ValueSchema.Variant(
                          "option1", "option1"
                      ),
                      new ValueSchema.Variant(
                          "option2",
                          "option2"))),
                  "duration",
                  ValueSchema.DURATION,
                  "fancy",
                  ValueSchema.ofStruct(Map.of(
                      "subfield1",
                      ValueSchema.STRING,
                      "subfield2",
                      ValueSchema.ofSeries(ValueSchema.ofStruct(Map.of("subsubfield1", ValueSchema.REAL)))
                  )))),
                  new TypescriptCodeGenerationService.ActivityType(
                      "SampleActivity2",
                      Map.of(
                          "quantity",
                          ValueSchema.REAL
                      )
                  )),
          null
      );

  @Test
  void testCodeGen() {
    assertEquals(
        """
            /** Start Codegen */
            import type { ActivityTemplate } from './scheduler-edsl-fluent-api.js';
            export enum ActivityType {
              SampleActivity1 = 'SampleActivity1',
              SampleActivity2 = 'SampleActivity2',
            }
            interface SampleActivity1 extends ActivityTemplate {}
            interface SampleActivity2 extends ActivityTemplate {}
            const ActivityTemplateConstructors = {
              SampleActivity1: function SampleActivity1Constructor(args: {
                duration: Duration,
                fancy: { subfield1: string, subfield2: { subsubfield1: Double, }[], },
                variant: ("option1" | "option2"),
              }): SampleActivity1 {
                return { activityType: ActivityType.SampleActivity1, args };
              },
              SampleActivity2: function SampleActivity2Constructor(args: {
                quantity: Double,
              }): SampleActivity2 {
                return { activityType: ActivityType.SampleActivity2, args };
              },
            };
            declare global {
              var ActivityTemplates: typeof ActivityTemplateConstructors;
              var ActivityTypes: typeof ActivityType;
            }
            // Make ActivityTemplates and ActivityTypes available on the global object
            Object.assign(globalThis, {
              ActivityTemplates: ActivityTemplateConstructors,
              ActivityTypes: ActivityType,
            });
            /** End Codegen */""",
        TypescriptCodeGenerationService.generateTypescriptTypesFromMissionModel(MISSION_MODEL_TYPES));
  }
}
