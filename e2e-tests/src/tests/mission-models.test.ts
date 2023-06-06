import {expect, test} from '@playwright/test';
import req from '../utilities/requests.js';

test.describe.serial('Mission Models', () => {
  let jar_id: number;
  let mission_model_id: number;

  test('Upload mission model .jar file to the gateway', async ({ request }) => {
    jar_id = await req.uploadJarFile(request);
    expect(jar_id).not.toBeNull();
    expect(jar_id).toBeDefined();
    expect(typeof jar_id).toEqual('number');
  });

  test('Create mission model using the jar_id returned from the gateway', async ({ request }) => {
    const model: MissionModelInsertInput = {
      jar_id,
      mission: 'aerie_e2e_tests',
      name: 'Banananation (e2e tests)',
      version: '0.0.0',
    };
    mission_model_id = await req.createMissionModel(request, model);
    expect(mission_model_id).not.toBeNull();
    expect(mission_model_id).toBeDefined();
    expect(typeof mission_model_id).toEqual('number');

    // Delay is to give time for Post-Mission-Model-Insert events to complete (Upload resource types, upload activity types)
    await new Promise( resolve => setTimeout(resolve, 2500) );
  });

  test('Mission model resource types were uploaded to database', async({request}) => {
    const resourceTypes = await req.getResourceTypes(request, mission_model_id);
    // resourceTypes is alphabetized by name
    const expectedResourceTypes:ResourceType[] = [
      {
        name: "/data/line_count",
        schema: { type: "int" }
      },
      {
        name: "/flag",
        schema: {
          type: "variant",
          variants: [
            { key: "A", label: "A"},
            { key: "B", label: "B"}
          ]
        }
      },
      {
       name: "/flag/conflicted",
       schema: { type: "boolean" }
      },
      {
        name: "/fruit",
        schema: {
          type: "struct",
          items: {
            "rate": { type:"real" },
            "initial": { type: "real" }
          }
        }
      },
      {
        name: "/peel",
        schema: { type: "real" }
      },
      {
        name: "/plant",
        schema: { type: "int" }
      },
      {
        name: "/producer",
        schema: { type: "string" }
      }
    ];

    expect(resourceTypes.length).toEqual(expectedResourceTypes.length)
    for (let i = 0; i < expectedResourceTypes.length; i++) {
      expect(JSON.stringify(resourceTypes[i]) === JSON.stringify(expectedResourceTypes[i])).toBeTruthy();
    }
  });

  test('Mission model activity types were uploaded to database', async({request}) => {
    const activityTypes = await req.getActivityTypes(request, mission_model_id);
    // activityTypes is alphabetized by name
    const expectedActivityTypes:ActivityType[] = [
      {
        name: "BakeBananaBread",
        parameters: {
          "tbSugar": { order: 1, schema: { type: "int"}},
          "glutenFree": { order: 2, schema: { type: "boolean" }},
          "temperature": { order: 0, schema: { type: "real" }}
        }
      },
      {
        name: "BananaNap",
        parameters: {}
      },
      {
        name: "BiteBanana",
        parameters: { "biteSize": { order: 0, schema: { type: "real" }}}
      },
      {
        name: "ChangeProducer",
        parameters: { "producer": { order: 0, schema: { type: "string" }}}
      },
      {
        name: "child",
        parameters: { "counter": { order: 0, schema: { type: "int" }}}
      },
      {
        name: "ControllableDurationActivity",
        parameters: { "duration": { order: 0, schema: { type: "duration" }}}
      },
      {
        name: "DecomposingSpawnChild",
        parameters: { "counter": { order: 0, schema: { type: "int" }}}
      },
      {
        name: "DecomposingSpawnParent",
        parameters: { "label": { order: 0, schema: { type: "string" }}}
      },
      {
        name: "DownloadBanana",
        parameters: { "connection": { order: 0, schema: { type: "variant", variants: [
            { key: "DSL", label: "DSL" },
            { key: "FiberOptic", label: "FiberOptic"},
            { key: "DietaryFiberOptic", label: "DietaryFiberOptic"}]}}}
      },
      {
        name: "DurationParameterActivity",
        parameters: { "duration": { order: 0, schema: { type: "duration" }}}
      },
      {
        name: "grandchild",
        parameters: { "counter": { order: 0, schema: { type: "int" }}}
      },
      {
        name: "GrowBanana",
        parameters: {
          "quantity": { order: 0, schema: { type: "int" }},
          "growingDuration": { order: 1, schema: { type: "duration" }}
        }
      },
      {
        name: "LineCount",
        parameters: { "path": { order: 0, schema: { type: "path" }}}
      },
      {
        name: "ParameterTest",
        parameters:
        {
          "intMap": {order: 47, schema: {type: "series", items: {type: "struct", items: { "key": { type: "int"}, "value": { type: "int"}}}}},
          "record": {order: 58, schema: {type: "struct", items: {
            "intMap": {type: "series", items: {type: "struct", items: { "key": { type: "int"}, "value": { type: "int"}}}},
            "nested": {type: "struct", items: {
              "a": {type: "string"},
              "b": {type: "series", items: {type: "struct", items: { "key": { type: "int" }, "value": { type: "string" }}}}}},
            "string": {type: "string" },
            "byteMap": {type: "series", items: {type: "struct", items: { "key": { type: "int" }, "value": { type: "int" }}}},
            "charMap": {type: "series", items: {type: "struct", items: { "key": { type: "string" }, "value": { type: "string" }}}},
            "intList": {type: "series", items: {type: "int" }},
            "longMap": {type: "series", items: {type: "struct", items: {"key": {type: "int"}, "value": {type: "int"}}}},
            "boxedInt": {type: "int"},
            "byteList": {type: "series", items: {type: "int"}},
            "charList": {type: "series", items: {type: "string"}},
            "floatMap": {type: "series", items: {type: "struct", items: {"key": {type: "real"}, "value": {type: "real"}}}},
            "intArray": {type: "series", items: {type: "int"}},
            "longList": {type: "series", items: {type: "int"}},
            "mappyBoi": {type: "series", items: {type: "struct", items: {"key": {type: "int"}, "value": {type: "series", items: {type:"string"}}}}},
            "shortMap": {type: "series", items: {type: "struct", items: {"key": {type: "int"}, "value": {type: "int"}}}},
            "testEnum": {type: "variant", variants: [
                {key: "A", label: "A"},
                {key: "B", label: "B"},
                {key: "C", label: "C"}]},
            "boxedByte": {type: "int"},
            "boxedChar": {type: "string"},
            "boxedLong": {type: "int"},
            "byteArray": {type: "series", items: {type: "int"}},
            "charArray": {type: "series", items: {type: "string"}},
            "doubleMap": {type: "series", items: {type: "struct", items: { "key": {type: "real"}, "value": {type: "real"}}}},
            "floatList": {type: "series", items: {type: "real"}},
            "longArray": {type: "series", items: {type: "int"}},
            "obnoxious": {type: "series", items: {type: "series", items: {type: "struct", items: {
              "key": {type: "series", items: {type: "series", items: {type: "string"}}},
              "value": {type: "series", items: {type: "struct", items: {
                "key": {type:"int"},
                "value": {type: "series", items: {type: "series", items: {type: "real"}}}}}}}}}},
            "shortList": {type: "series", items: {type: "int"}},
            "stringMap": {type: "series", items: {type: "struct", items: {"key": {type: "string"}, "value": {type: "string"}}}},
            "booleanMap": {type: "series", items: {type: "struct", items: {"key": {type: "boolean"}, "value": {type: "boolean"}}}},
            "boxedFloat": {type: "real"},
            "boxedShort": {type: "int"},
            "doubleList": {type: "series", items: {type: "real"}},
            "floatArray": {type: "series", items: {type: "real"}},
            "shortArray": {type: "series", items: {type: "int"}},
            "stringList": {type: "series", items: {type: "string"}},
            "booleanList": {type: "series", items: {type: "boolean"}},
            "boxedDouble": {type: "real"},
            "doubleArray": {type: "series", items: {type: "real"}},
            "stringArray": {type: "series", items: {type: "string"}},
            "booleanArray": {type: "series", items: {type: "boolean"}},
            "boxedBoolean": {type: "boolean"},
            "primIntArray": {type: "series", items: {type: "int"}},
            "primitiveInt": {type: "int"},
            "testDuration": {type: "duration"},
            "primByteArray": {type: "series", items: {type: "int"}},
            "primCharArray": {type: "series", items: {type: "string"}},
            "primLongArray": {type: "series", items: {type: "int"}},
            "primitiveByte": {type: "int"},
            "primitiveChar": {type: "string"},
            "primitiveLong": {type: "int"},
            "primFloatArray": {type: "series", items: {type: "real"}},
            "primShortArray": {type: "series", items: {type: "int"}},
            "primitiveFloat": {type: "real"},
            "primitiveShort": {type: "int"},
            "primDoubleArray": {type: "series", items: {type: "real"}},
            "primitiveDouble": {type: "real"},
            "genericParameter": {type: "series", items: {type: "string"}},
            "primBooleanArray": {type: "series", items: {type: "boolean"}},
            "primitiveBoolean": {type: "boolean"},
            "intListArrayArray": {type: "series", items: {type: "series", items: {type: "series", items: {type: "int"}}}},
            "doublePrimIntArray": {type: "series", items: {type: "series", items: {type: "int"}}}}}},
          "string": {order: 16, schema: { type: "string" }},
          "byteMap": {order: 45, schema: {type: "series", items: {type: "struct", items: { "key": { type: "int" }, "value": { type: "int" }}}}},
          "charMap": {order: 49, schema: {type: "series", items: {type: "struct", items: { "key": { type: "string" }, "value": { type: "string" }}}}},
          "intList": {order: 38, schema: {type: "series", items: {type: "int" }}},
          "longMap": {order: 48, schema: {type: "series", items: {type: "struct", items: {"key": {type: "int"}, "value": {type: "int"}}}}},
          "boxedInt": {order: 12, schema: {type: "int"}},
          "byteList": {order: 36, schema: {type: "series", items: {type: "int"}}},
          "charList": {order: 40, schema: {type: "series", items: {type: "string"}}},
          "floatMap": {order: 44, schema: {type: "series", items: {type: "struct", items: {"key": {type: "real"}, "value": {type: "real"}}}}},
          "intArray": {order: 21, schema: {type: "series", items: {type: "int"}}},
          "longList": {order: 39, schema: {type: "series", items: {type: "int"}}},
          "mappyBoi": {order: 54, schema: {type: "series", items: {type: "struct", items: {"key": {type: "int"}, "value": {type: "series", items: {type:"string"}}}}}},
          "shortMap": {order: 46, schema: {type: "series", items: {type: "struct", items: {"key": {type: "int"}, "value": {type: "int"}}}}},
          "testEnum": {order: 53, schema: {type: "variant", variants: [
                {key: "A", label: "A"},
                {key: "B", label: "B"},
                {key: "C", label: "C"}]}},
          "boxedByte": {order: 10, schema: {type: "int"}},
          "boxedChar": {order: 14, schema: {type: "string"}},
          "boxedLong": {order: 13, schema: {type: "int"}},
          "byteArray": {order: 19, schema: {type: "series", items: {type: "int"}}},
          "charArray": {order: 23, schema: {type: "series", items: {type: "string"}}},
          "doubleMap": {order: 43, schema: {type: "series", items: {type: "struct", items: { "key": {type: "real"}, "value": {type: "real"}}}}},
          "floatList": {order: 35, schema: {type: "series", items: {type: "real"}}},
          "longArray": {order: 22, schema: {type: "series", items: {type: "int"}}},
          "obnoxious": {order: 57, schema: {type: "series", items: {type: "series", items: {type: "struct", items: {
                  "key": {type: "series", items: {type: "series", items: {type: "string"}}},
                  "value": {type: "series", items: {type: "struct", items: {
                    "key": {type:"int"},
                    "value": {type: "series", items: {type: "series", items: {type: "real"}}}}}}}}}}},
          "shortList": {order: 37, schema: {type: "series", items: {type: "int"}}},
          "stringMap": {order: 51, schema: {type: "series", items: {type: "struct", items: {"key": {type: "string"}, "value": {type: "string"}}}}},
          "booleanMap": {order: 50, schema: {type: "series", items: {type: "struct", items: {"key": {type: "boolean"}, "value": {type: "boolean"}}}}},
          "boxedFloat": {order: 9, schema: {type: "real"}},
          "boxedShort": {order: 11, schema: {type: "int"}},
          "doubleList": {order: 34, schema: {type: "series", items: {type: "real"}}},
          "floatArray": {order: 18, schema: {type: "series", items: {type: "real"}}},
          "shortArray": {order: 20, schema: {type: "series", items: {type: "int"}}},
          "stringList": {order: 42, schema: {type: "series", items: {type: "string"}}},
          "booleanList": {order: 41, schema: {type: "series", items: {type: "boolean"}}},
          "boxedDouble": {order: 8, schema: {type: "real"}},
          "doubleArray": {order: 17, schema: {type: "series", items: {type: "real"}}},
          "stringArray": {order: 25, schema: {type: "series", items: {type: "string"}}},
          "booleanArray": {order: 24, schema: {type: "series", items: {type: "boolean"}}},
          "boxedBoolean": {order: 15, schema: {type: "boolean"}},
          "primIntArray": {order: 30, schema: {type: "series", items: {type: "int"}}},
          "primitiveInt": {order: 4, schema: {type: "int"}},
          "testDuration": {order: 52, schema: {type: "duration"}},
          "primByteArray": {order: 28, schema: {type: "series", items: {type: "int"}}},
          "primCharArray": {order: 32, schema: {type: "series", items: {type: "string"}}},
          "primLongArray": {order: 31, schema: {type: "series", items: {type: "int"}}},
          "primitiveByte": {order: 2, schema: {type: "int"}},
          "primitiveChar": {order: 6, schema: {type: "string"}},
          "primitiveLong": {order: 5, schema: {type: "int"}},
          "primFloatArray": {order: 27, schema: {type: "series", items: {type: "real"}}},
          "primShortArray": {order: 29, schema: {type: "series", items: {type: "int"}}},
          "primitiveFloat": {order: 1, schema: {type: "real"}},
          "primitiveShort": {order: 3, schema: {type: "int"}},
          "primDoubleArray": {order: 26, schema: {type: "series", items: {type: "real"}}},
          "primitiveDouble": {order: 0, schema: {type: "real"}},
          "primBooleanArray": {order: 33, schema: {type: "series", items: {type: "boolean"}}},
          "primitiveBoolean": {order: 7, schema: {type: "boolean"}},
          "intListArrayArray": {order: 56, schema: {type: "series", items: {type: "series", items: {type: "series", items: {type: "int"}}}}},
          "doublePrimIntArray": {order: 55, schema: {type: "series", items: {type: "series", items: {type: "int"}}}},
        }
      },
      {
        name: "parent",
        parameters: { "label": {order: 0, schema: {type: "string"}}}
      },
      {
        name: "PeelBanana",
        parameters: { "peelDirection": {order: 0, schema: {type: "variant", variants: [{key: "fromStem", label: "fromStem"}, {key: "fromTip", label: "fromTip"}]}}}
      },
      {
        name: "PickBanana",
        parameters: { "quantity": {order: 0, schema: {type: "int"}}}
      },
      {
        name: "RipenBanana",
        parameters: {}
      },
      {
        name: "ThrowBanana",
        parameters: { "speed": {order: 0, schema: {type: "real"}}}
      },
    ];

    expect(activityTypes.length).toEqual(expectedActivityTypes.length)
    for (let i = 0; i < expectedActivityTypes.length; i++) {
      expect(JSON.stringify(activityTypes[i]) === JSON.stringify(expectedActivityTypes[i])).toBeTruthy();
    }
  });

  test('Delete mission model', async ({ request }) => {
    const deleted_mission_model_id = await req.deleteMissionModel(request, mission_model_id);
    expect(deleted_mission_model_id).toEqual(mission_model_id);
    expect(deleted_mission_model_id).not.toBeNull();
    expect(deleted_mission_model_id).toBeDefined();
    expect(typeof deleted_mission_model_id).toEqual('number');
  });
});
