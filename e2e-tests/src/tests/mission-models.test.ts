import { expect, test } from '@playwright/test';
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
    await new Promise(resolve => setTimeout(resolve, 2500));
  });

  test('Mission model resource types were uploaded to database', async ({ request }) => {
    const resourceTypes = await req.getResourceTypes(request, mission_model_id);
    // resourceTypes is alphabetized by name
    const expectedResourceTypes: ResourceType[] = [
      {
        name: '/data/line_count',
        definition: { schema: { type: 'int' }, unit: '' },
      },
      {
        name: '/flag',
        definition: {
          schema: {
            type: 'variant',
            variants: [
              { key: 'A', label: 'A' },
              { key: 'B', label: 'B' },
            ],
          },
          unit: '',
        },
      },
      {
        name: '/flag/conflicted',
        definition: { schema: { type: 'boolean' }, unit: '' },
      },
      {
        name: '/fruit',
        definition: {
          schema: {
            type: 'struct',
            items: {
              rate: { type: 'real' },
              initial: { type: 'real' },
            },
          },
          unit: '',
        },
      },
      {
        name: '/peel',
        definition: { schema: { type: 'real' }, unit: '' },
      },
      {
        name: '/plant',
        definition: { schema: { type: 'int' }, unit: '' },
      },
      {
        name: '/producer',
        definition: { schema: { type: 'string' }, unit: '' },
      },
    ];

    expect(resourceTypes.length).toEqual(expectedResourceTypes.length);
    for (let i = 0; i < expectedResourceTypes.length; i++) {
      expect(JSON.stringify(resourceTypes[i]) === JSON.stringify(expectedResourceTypes[i])).toBeTruthy();
    }
  });

  test('Mission model activity types were uploaded to database', async ({ request }) => {
    const activityTypes = await req.getActivityTypes(request, mission_model_id);
    // activityTypes is alphabetized by name
    const expectedActivityTypes: ActivityType[] = [
      {
        name: 'BakeBananaBread',
        parameter_definitions: {
          tbSugar: { order: 1, schema: { type: 'int' }, unit: '' },
          glutenFree: { order: 2, schema: { type: 'boolean' }, unit: '' },
          temperature: { order: 0, schema: { type: 'real' }, unit: '' },
        },
      },
      {
        name: 'BananaNap',
        parameter_definitions: {},
      },
      {
        name: 'BiteBanana',
        parameter_definitions: { biteSize: { order: 0, schema: { type: 'real' }, unit: '' } },
      },
      {
        name: 'ChangeProducer',
        parameter_definitions: { producer: { order: 0, schema: { type: 'string' }, unit: '' } },
      },
      {
        name: 'child',
        parameter_definitions: { counter: { order: 0, schema: { type: 'int' }, unit: '' } },
      },
      {
        name: 'ControllableDurationActivity',
        parameter_definitions: { duration: { order: 0, schema: { type: 'duration' }, unit: '' } },
      },
      {
        name: 'DecomposingSpawnChild',
        parameter_definitions: { counter: { order: 0, schema: { type: 'int' }, unit: '' } },
      },
      {
        name: 'DecomposingSpawnParent',
        parameter_definitions: { label: { order: 0, schema: { type: 'string' }, unit: '' } },
      },
      {
        name: 'DownloadBanana',
        parameter_definitions: {
          connection: {
            order: 0,
            schema: {
              type: 'variant',
              variants: [
                { key: 'DSL', label: 'DSL' },
                { key: 'FiberOptic', label: 'FiberOptic' },
                { key: 'DietaryFiberOptic', label: 'DietaryFiberOptic' },
              ],
            },
            unit: '',
          },
        },
      },
      {
        name: 'DurationParameterActivity',
        parameter_definitions: { duration: { order: 0, schema: { type: 'duration' }, unit: '' } },
      },
      {
        name: 'grandchild',
        parameter_definitions: { counter: { order: 0, schema: { type: 'int' }, unit: '' } },
      },
      {
        name: 'GrowBanana',
        parameter_definitions: {
          quantity: { order: 0, schema: { type: 'int' }, unit: '' },
          growingDuration: { order: 1, schema: { type: 'duration' }, unit: '' },
        },
      },
      {
        name: 'LineCount',
        parameter_definitions: { path: { order: 0, schema: { type: 'path' }, unit: '' } },
      },
      {
        name: 'ParameterTest',
        parameter_definitions: {
          intMap: {
            order: 47,
            schema: {
              type: 'series',
              items: { type: 'struct', items: { key: { type: 'int' }, value: { type: 'int' } } },
            },
            unit: '',
          },
          record: {
            order: 58,
            schema: {
              type: 'struct',
              items: {
                intMap: {
                  type: 'series',
                  items: { type: 'struct', items: { key: { type: 'int' }, value: { type: 'int' } } },
                },
                nested: {
                  type: 'struct',
                  items: {
                    a: { type: 'string' },
                    b: {
                      type: 'series',
                      items: { type: 'struct', items: { key: { type: 'int' }, value: { type: 'string' } } },
                    },
                  },
                },
                string: { type: 'string' },
                byteMap: {
                  type: 'series',
                  items: { type: 'struct', items: { key: { type: 'int' }, value: { type: 'int' } } },
                },
                charMap: {
                  type: 'series',
                  items: { type: 'struct', items: { key: { type: 'string' }, value: { type: 'string' } } },
                },
                intList: { type: 'series', items: { type: 'int' } },
                longMap: {
                  type: 'series',
                  items: { type: 'struct', items: { key: { type: 'int' }, value: { type: 'int' } } },
                },
                boxedInt: { type: 'int' },
                byteList: { type: 'series', items: { type: 'int' } },
                charList: { type: 'series', items: { type: 'string' } },
                floatMap: {
                  type: 'series',
                  items: { type: 'struct', items: { key: { type: 'real' }, value: { type: 'real' } } },
                },
                intArray: { type: 'series', items: { type: 'int' } },
                longList: { type: 'series', items: { type: 'int' } },
                mappyBoi: {
                  type: 'series',
                  items: {
                    type: 'struct',
                    items: { key: { type: 'int' }, value: { type: 'series', items: { type: 'string' } } },
                  },
                },
                shortMap: {
                  type: 'series',
                  items: { type: 'struct', items: { key: { type: 'int' }, value: { type: 'int' } } },
                },
                testEnum: {
                  type: 'variant',
                  variants: [
                    { key: 'A', label: 'A' },
                    { key: 'B', label: 'B' },
                    { key: 'C', label: 'C' },
                  ],
                },
                boxedByte: { type: 'int' },
                boxedChar: { type: 'string' },
                boxedLong: { type: 'int' },
                byteArray: { type: 'series', items: { type: 'int' } },
                charArray: { type: 'series', items: { type: 'string' } },
                doubleMap: {
                  type: 'series',
                  items: { type: 'struct', items: { key: { type: 'real' }, value: { type: 'real' } } },
                },
                floatList: { type: 'series', items: { type: 'real' } },
                longArray: { type: 'series', items: { type: 'int' } },
                obnoxious: {
                  type: 'series',
                  items: {
                    type: 'series',
                    items: {
                      type: 'struct',
                      items: {
                        key: { type: 'series', items: { type: 'series', items: { type: 'string' } } },
                        value: {
                          type: 'series',
                          items: {
                            type: 'struct',
                            items: {
                              key: { type: 'int' },
                              value: { type: 'series', items: { type: 'series', items: { type: 'real' } } },
                            },
                          },
                        },
                      },
                    },
                  },
                },
                shortList: { type: 'series', items: { type: 'int' } },
                stringMap: {
                  type: 'series',
                  items: { type: 'struct', items: { key: { type: 'string' }, value: { type: 'string' } } },
                },
                booleanMap: {
                  type: 'series',
                  items: { type: 'struct', items: { key: { type: 'boolean' }, value: { type: 'boolean' } } },
                },
                boxedFloat: { type: 'real' },
                boxedShort: { type: 'int' },
                doubleList: { type: 'series', items: { type: 'real' } },
                floatArray: { type: 'series', items: { type: 'real' } },
                shortArray: { type: 'series', items: { type: 'int' } },
                stringList: { type: 'series', items: { type: 'string' } },
                booleanList: { type: 'series', items: { type: 'boolean' } },
                boxedDouble: { type: 'real' },
                doubleArray: { type: 'series', items: { type: 'real' } },
                stringArray: { type: 'series', items: { type: 'string' } },
                booleanArray: { type: 'series', items: { type: 'boolean' } },
                boxedBoolean: { type: 'boolean' },
                primIntArray: { type: 'series', items: { type: 'int' } },
                primitiveInt: { type: 'int' },
                testDuration: { type: 'duration' },
                primByteArray: { type: 'series', items: { type: 'int' } },
                primCharArray: { type: 'series', items: { type: 'string' } },
                primLongArray: { type: 'series', items: { type: 'int' } },
                primitiveByte: { type: 'int' },
                primitiveChar: { type: 'string' },
                primitiveLong: { type: 'int' },
                primFloatArray: { type: 'series', items: { type: 'real' } },
                primShortArray: { type: 'series', items: { type: 'int' } },
                primitiveFloat: { type: 'real' },
                primitiveShort: { type: 'int' },
                primDoubleArray: { type: 'series', items: { type: 'real' } },
                primitiveDouble: { type: 'real' },
                genericParameter: { type: 'series', items: { type: 'string' } },
                primBooleanArray: { type: 'series', items: { type: 'boolean' } },
                primitiveBoolean: { type: 'boolean' },
                intListArrayArray: {
                  type: 'series',
                  items: { type: 'series', items: { type: 'series', items: { type: 'int' } } },
                },
                doublePrimIntArray: { type: 'series', items: { type: 'series', items: { type: 'int' } } },
              },
            },
            unit: '',
          },
          string: { order: 16, schema: { type: 'string' }, unit: '' },
          byteMap: {
            order: 45,
            schema: {
              type: 'series',
              items: { type: 'struct', items: { key: { type: 'int' }, value: { type: 'int' } } },
            },
            unit: '',
          },
          charMap: {
            order: 49,
            schema: {
              type: 'series',
              items: { type: 'struct', items: { key: { type: 'string' }, value: { type: 'string' } } },
            },
            unit: '',
          },
          intList: { order: 38, schema: { type: 'series', items: { type: 'int' } }, unit: '' },
          longMap: {
            order: 48,
            schema: {
              type: 'series',
              items: { type: 'struct', items: { key: { type: 'int' }, value: { type: 'int' } } },
            },
            unit: '',
          },
          boxedInt: { order: 12, schema: { type: 'int' }, unit: '' },
          byteList: { order: 36, schema: { type: 'series', items: { type: 'int' } }, unit: '' },
          charList: { order: 40, schema: { type: 'series', items: { type: 'string' } }, unit: '' },
          floatMap: {
            order: 44,
            schema: {
              type: 'series',
              items: { type: 'struct', items: { key: { type: 'real' }, value: { type: 'real' } } },
            },
            unit: '',
          },
          intArray: { order: 21, schema: { type: 'series', items: { type: 'int' } }, unit: '' },
          longList: { order: 39, schema: { type: 'series', items: { type: 'int' } }, unit: '' },
          mappyBoi: {
            order: 54,
            schema: {
              type: 'series',
              items: {
                type: 'struct',
                items: { key: { type: 'int' }, value: { type: 'series', items: { type: 'string' } } },
              },
            },
            unit: '',
          },
          shortMap: {
            order: 46,
            schema: {
              type: 'series',
              items: { type: 'struct', items: { key: { type: 'int' }, value: { type: 'int' } } },
            },
            unit: '',
          },
          testEnum: {
            order: 53,
            schema: {
              type: 'variant',
              variants: [
                { key: 'A', label: 'A' },
                { key: 'B', label: 'B' },
                { key: 'C', label: 'C' },
              ],
            },
            unit: '',
          },
          boxedByte: { order: 10, schema: { type: 'int' }, unit: '' },
          boxedChar: { order: 14, schema: { type: 'string' }, unit: '' },
          boxedLong: { order: 13, schema: { type: 'int' }, unit: '' },
          byteArray: { order: 19, schema: { type: 'series', items: { type: 'int' } }, unit: '' },
          charArray: { order: 23, schema: { type: 'series', items: { type: 'string' } }, unit: '' },
          doubleMap: {
            order: 43,
            schema: {
              type: 'series',
              items: { type: 'struct', items: { key: { type: 'real' }, value: { type: 'real' } } },
            },
            unit: '',
          },
          floatList: { order: 35, schema: { type: 'series', items: { type: 'real' } }, unit: '' },
          longArray: { order: 22, schema: { type: 'series', items: { type: 'int' } }, unit: '' },
          obnoxious: {
            order: 57,
            schema: {
              type: 'series',
              items: {
                type: 'series',
                items: {
                  type: 'struct',
                  items: {
                    key: { type: 'series', items: { type: 'series', items: { type: 'string' } } },
                    value: {
                      type: 'series',
                      items: {
                        type: 'struct',
                        items: {
                          key: { type: 'int' },
                          value: { type: 'series', items: { type: 'series', items: { type: 'real' } } },
                        },
                      },
                    },
                  },
                },
              },
            },
            unit: '',
          },
          shortList: { order: 37, schema: { type: 'series', items: { type: 'int' } }, unit: '' },
          stringMap: {
            order: 51,
            schema: {
              type: 'series',
              items: { type: 'struct', items: { key: { type: 'string' }, value: { type: 'string' } } },
            },
            unit: '',
          },
          booleanMap: {
            order: 50,
            schema: {
              type: 'series',
              items: { type: 'struct', items: { key: { type: 'boolean' }, value: { type: 'boolean' } } },
            },
            unit: '',
          },
          boxedFloat: { order: 9, schema: { type: 'real' }, unit: '' },
          boxedShort: { order: 11, schema: { type: 'int' }, unit: '' },
          doubleList: { order: 34, schema: { type: 'series', items: { type: 'real' } }, unit: '' },
          floatArray: { order: 18, schema: { type: 'series', items: { type: 'real' } }, unit: '' },
          shortArray: { order: 20, schema: { type: 'series', items: { type: 'int' } }, unit: '' },
          stringList: { order: 42, schema: { type: 'series', items: { type: 'string' } }, unit: '' },
          booleanList: { order: 41, schema: { type: 'series', items: { type: 'boolean' } }, unit: '' },
          boxedDouble: { order: 8, schema: { type: 'real' }, unit: '' },
          doubleArray: { order: 17, schema: { type: 'series', items: { type: 'real' } }, unit: '' },
          stringArray: { order: 25, schema: { type: 'series', items: { type: 'string' } }, unit: '' },
          booleanArray: { order: 24, schema: { type: 'series', items: { type: 'boolean' } }, unit: '' },
          boxedBoolean: { order: 15, schema: { type: 'boolean' }, unit: '' },
          primIntArray: { order: 30, schema: { type: 'series', items: { type: 'int' } }, unit: '' },
          primitiveInt: { order: 4, schema: { type: 'int' }, unit: '' },
          testDuration: { order: 52, schema: { type: 'duration' }, unit: '' },
          primByteArray: { order: 28, schema: { type: 'series', items: { type: 'int' } }, unit: '' },
          primCharArray: { order: 32, schema: { type: 'series', items: { type: 'string' } }, unit: '' },
          primLongArray: { order: 31, schema: { type: 'series', items: { type: 'int' } }, unit: '' },
          primitiveByte: { order: 2, schema: { type: 'int' }, unit: '' },
          primitiveChar: { order: 6, schema: { type: 'string' }, unit: '' },
          primitiveLong: { order: 5, schema: { type: 'int' }, unit: '' },
          primFloatArray: { order: 27, schema: { type: 'series', items: { type: 'real' } }, unit: '' },
          primShortArray: { order: 29, schema: { type: 'series', items: { type: 'int' } }, unit: '' },
          primitiveFloat: { order: 1, schema: { type: 'real' }, unit: '' },
          primitiveShort: { order: 3, schema: { type: 'int' }, unit: '' },
          primDoubleArray: { order: 26, schema: { type: 'series', items: { type: 'real' } }, unit: '' },
          primitiveDouble: { order: 0, schema: { type: 'real' }, unit: '' },
          primBooleanArray: { order: 33, schema: { type: 'series', items: { type: 'boolean' } }, unit: '' },
          primitiveBoolean: { order: 7, schema: { type: 'boolean' }, unit: '' },
          intListArrayArray: {
            order: 56,
            schema: { type: 'series', items: { type: 'series', items: { type: 'series', items: { type: 'int' } } } },
            unit: '',
          },
          doublePrimIntArray: {
            order: 55,
            schema: { type: 'series', items: { type: 'series', items: { type: 'int' } } },
            unit: '',
          },
        },
      },
      {
        name: 'parent',
        parameter_definitions: { label: { order: 0, schema: { type: 'string' }, unit: '' } },
      },
      {
        name: 'PeelBanana',
        parameter_definitions: {
          peelDirection: {
            order: 0,
            schema: {
              type: 'variant',
              variants: [
                { key: 'fromStem', label: 'fromStem' },
                { key: 'fromTip', label: 'fromTip' },
              ],
            },
            unit: '',
          },
        },
      },
      {
        name: 'PickBanana',
        parameter_definitions: { quantity: { order: 0, schema: { type: 'int' }, unit: '' } },
      },
      {
        name: 'RipenBanana',
        parameter_definitions: {},
      },
      {
        name: 'ThrowBanana',
        parameter_definitions: { speed: { order: 0, schema: { type: 'real' }, unit: '' } },
      },
    ];

    expect(activityTypes.length).toEqual(expectedActivityTypes.length);
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
