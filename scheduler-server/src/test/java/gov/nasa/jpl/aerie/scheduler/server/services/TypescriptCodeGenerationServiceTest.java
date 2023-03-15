package gov.nasa.jpl.aerie.scheduler.server.services;

import org.junit.jupiter.api.Test;

import static gov.nasa.jpl.aerie.scheduler.server.services.TypescriptCodeGenerationServiceTestFixtures.MISSION_MODEL_TYPES;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TypescriptCodeGenerationServiceTest {

  @Test
  void testCodeGen() {
    assertEquals(
        """
/** Start Codegen */
import type { ActivityTemplate } from './scheduler-edsl-fluent-api.js';
import type { Windows } from './constraints-edsl-fluent-api.js';
import type * as ConstraintEDSL from './constraints-edsl-fluent-api.js'
interface SampleActivity1 extends ActivityTemplate<ActivityType.SampleActivity1> {}
interface SampleActivity2 extends ActivityTemplate<ActivityType.SampleActivity2> {}
interface SampleActivityEmpty extends ActivityTemplate<ActivityType.SampleActivityEmpty> {}
export function makeAllDiscreteProfile (argument: any) : any{
 if ((argument instanceof Discrete) || (argument instanceof Real)) {
   return argument.__astNode
 } else if ((argument instanceof Temporal.Duration) || (argument.kind === 'IntervalDuration')) {
   return argument;
 } else if(typeof(argument) === "number"){
     if(Number.isInteger(argument)){
       return Discrete.Value(argument).__astNode
     } else{
       return Real.Value(argument).__astNode
     }
   } else if(typeof(argument) === "string" || argument instanceof Temporal.Duration || typeof(argument) === "boolean"){
     return Discrete.Value(argument).__astNode
     }
  else if(Array.isArray(argument)){
   const arr: any[] = [];
   argument.every((element) => { arr.push(makeAllDiscreteProfile(element))});
   return Discrete.List(arr).__astNode;
 } else if (typeof argument === "object" ){
   const obj: { [k: string]: any } = {};
   for(var key in (argument)){
     // @ts-ignore
     obj[key] = makeAllDiscreteProfile((<object>argument)[key])
   }
   return Discrete.Map(obj).__astNode;
 } else{
  throw new Error('[makeAllDiscreteProfile] Type not covered: ' + argument);
 }
}

export function makeAll<T>(args : T):T{
// @ts-ignore
return (<T>makeAllDiscreteProfile(args))
}

const ActivityTemplateConstructors = {
  SampleActivity1: function SampleActivity1Constructor(args:  ConstraintEDSL.Gen.ActivityTypeParameterMap[ActivityType.SampleActivity1]
  ): SampleActivity1 {
  // @ts-ignore
    return { activityType: ActivityType.SampleActivity1, args: makeAll(args) };
  },
  SampleActivity2: function SampleActivity2Constructor(args:  ConstraintEDSL.Gen.ActivityTypeParameterMap[ActivityType.SampleActivity2]
  ): SampleActivity2 {
  // @ts-ignore
    return { activityType: ActivityType.SampleActivity2, args: makeAll(args) };
  },
  SampleActivityEmpty: function SampleActivityEmptyConstructor(): SampleActivityEmpty {
    return { activityType: ActivityType.SampleActivityEmpty, args: {} };
  },
};
const ActivityPresetMap = {
  SampleActivity1: {
  },
  SampleActivity2: {
    "my preset": {
      quantity: 5,
    },
  },
  SampleActivityEmpty: {
  },
};
export enum Resource {
  "/sample/resource/1" = "/sample/resource/1",
  "/sample/resource/3" = "/sample/resource/3",
  "/sample/resource/2" = "/sample/resource/2",
};
declare global {
  var ActivityTemplates: typeof ActivityTemplateConstructors;
  var ActivityPresets: typeof ActivityPresetMap;
  var Resources: typeof Resource;
}
// Make ActivityTemplates and ActivityTypes available on the global object
Object.assign(globalThis, {
  ActivityTemplates: ActivityTemplateConstructors,
  ActivityPresets: ActivityPresetMap,
  ActivityTypes: ActivityType,
  Resources: Resource,
});
/** End Codegen */""",
        TypescriptCodeGenerationService.generateTypescriptTypesFromMissionModel(MISSION_MODEL_TYPES));
  }
}
