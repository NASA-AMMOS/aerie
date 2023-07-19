import type { GraphQLClient } from 'graphql-request';
import { TimingTypes } from '../src/lib/codegen/CommandEDSLPreface.js';
import { FallibleStatus } from '../src/types.js';
import {
  convertActivityDirectiveIdToSimulatedActivityId,
  insertActivityDirective,
  removeActivityDirective,
} from './testUtils/ActivityDirective.js';
import { insertCommandDictionary, removeCommandDictionary } from './testUtils/CommandDictionary.js';
import {
  expand,
  insertExpansion,
  insertExpansionSet,
  removeExpansion,
  removeExpansionRun,
  removeExpansionSet,
} from './testUtils/Expansion.js';
import { removeMissionModel, uploadMissionModel } from './testUtils/MissionModel.js';
import { createPlan, removePlan } from './testUtils/Plan.js';
import {
  generateSequenceEDSL,
  generateSequenceEDSLBulk,
  getSequenceSeqJson,
  getSequenceSeqJsonBulk,
  insertSequence,
  linkActivityInstance,
  removeSequence,
} from './testUtils/Sequence.js';
import { executeSimulation, removeSimulationArtifacts, updateSimulationBounds } from './testUtils/Simulation.js';
import { getGraphQLClient } from './testUtils/testUtils.js';

let planId: number;
let graphqlClient: GraphQLClient;
let missionModelId: number;
let commandDictionaryId: number;

beforeAll(async () => {
  graphqlClient = await getGraphQLClient();
  commandDictionaryId = (await insertCommandDictionary(graphqlClient)).id;
});

beforeEach(async () => {
  missionModelId = await uploadMissionModel(graphqlClient);
  planId = await createPlan(graphqlClient, missionModelId);
  await updateSimulationBounds(graphqlClient, {
    plan_id: planId,
    simulation_start_time: '2020-001T00:00:00Z',
    simulation_end_time: '2020-002T00:00:00Z',
  });
});

afterAll(async () => {
  await removeCommandDictionary(graphqlClient, commandDictionaryId);
});

afterEach(async () => {
  await removePlan(graphqlClient, planId);
  await removeMissionModel(graphqlClient, missionModelId);
});

describe('sequence generation', () => {
  let expansionId1: number;
  let expansionId2: number;
  let expansionId3: number;

  beforeEach(async () => {
    expansionId1 = await insertExpansion(
      graphqlClient,
      'GrowBanana',
      `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        C.PREHEAT_OVEN({ temperature: 70 }),
        C.PREPARE_LOAF({ gluten_free: "FALSE", tb_sugar: 50 }),
        C.BAKE_BREAD,
      ];
    }
    `,
    );
    expansionId2 = await insertExpansion(
      graphqlClient,
      'PeelBanana',
      `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        C.PREHEAT_OVEN(70),
        C.BAKE_BREAD,
        C.PREPARE_LOAF({ tb_sugar: 50, gluten_free: "FALSE" }),
      ];
    }
    `,
    );
    expansionId3 = await insertExpansion(
      graphqlClient,
      'ThrowBanana',
      `
    export default function TimeDynamicCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        A\`2020-060T03:45:19\`.ADD_WATER,
        A(Temporal.Instant.from("2025-12-24T12:01:59Z")).PREHEAT_OVEN({ temperature: 360 }),
        R\`00:15:30\`.PREHEAT_OVEN({ temperature: 425 }),
        R(Temporal.Duration.from({ hours: 1, minutes: 15, seconds: 30 })).EAT_BANANA,
        E(Temporal.Duration.from({ days: -1, hours: -12, minutes: -16, seconds: -54 })).PREPARE_LOAF( 50,  "FALSE"),
        E\`04:56:54\`.EAT_BANANA,
        C.PACKAGE_BANANA({
          bundle: [
            {
              bundle_name: "Chiquita",
              number_of_bananas: 43
            },
            {
              number_of_bananas: 12,
              bundle_name: "Dole"
            }
          ],
          lot_number:  1093
        }),
        C.PACKAGE_BANANA({
          lot_number: 1093,
          bundle: [
            {
              bundle_name: "Chiquita",
              number_of_bananas: 43
            },
            {
              bundle_name: "Blue",
              number_of_bananas: 12
            }
          ]
        }),
        C.PACKAGE_BANANA(1034,[['companyA',100_000],['companyB',10_100]]),
      ];
    }
    `,
    );
  });

  afterEach(async () => {
    await removeExpansion(graphqlClient, expansionId1);
    await removeExpansion(graphqlClient, expansionId2);
    await removeExpansion(graphqlClient, expansionId3);
  });

  it('should return sequence seqjson', async () => {
    /** Begin Setup */
    // Create Expansion Set
    const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
      expansionId1,
      expansionId2,
      expansionId3,
    ]);

    // Create Activity Directives
    const [activityId1, activityId2, activityId3] = await Promise.all([
      insertActivityDirective(graphqlClient, planId, 'GrowBanana'),
      insertActivityDirective(graphqlClient, planId, 'PeelBanana', '30 minutes'),
      insertActivityDirective(graphqlClient, planId, 'ThrowBanana', '60 minutes'),
    ]);

    // Simulate Plan
    const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
    // Expand Plan to Sequence Fragments
    const expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
    // Create Sequence
    const sequencePk = await insertSequence(graphqlClient, {
      seqId: 'test00000',
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
    });
    // Link Activity Instances to Sequence
    await Promise.all([
      linkActivityInstance(graphqlClient, sequencePk, activityId1),
      linkActivityInstance(graphqlClient, sequencePk, activityId2),
      linkActivityInstance(graphqlClient, sequencePk, activityId3),
    ]);

    // Get the simulated activity ids
    const [simulatedActivityId1, simulatedActivityId2, simulatedActivityId3] = await Promise.all([
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId1,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId2,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId3,
      ),
    ]);
    /** End Setup */

    // Retrieve seqJson
    const getSequenceSeqJsonResponse = await getSequenceSeqJson(
      graphqlClient,
      'test00000',
      simulationArtifactPk.simulationDatasetId,
    );

    if (getSequenceSeqJsonResponse.status !== FallibleStatus.SUCCESS) {
      throw getSequenceSeqJsonResponse.errors;
    }

    expect(getSequenceSeqJsonResponse.seqJson.id).toBe('test00000');
    expect(getSequenceSeqJsonResponse.seqJson.metadata).toEqual({
      planId: planId,
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      timeSorted: false,
    });
    expect(getSequenceSeqJsonResponse.seqJson.steps).toEqual([
      {
        // expansion 1
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        // expansion 2
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        // expansion 4
        type: 'command',
        stem: 'ADD_WATER',
        time: {
          tag: '2020-060T03:45:19.000',
          type: 'ABSOLUTE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '2025-358T12:01:59.000',
          type: 'ABSOLUTE',
        },
        args: [{ value: 360, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '00:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [{ value: 425, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '01:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: {
          tag: '-001T12:16:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '04:56:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Dole',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Blue',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },{
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1034 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyA',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 100000,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyB',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 10100,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
    ]);

    /** Begin Cleanup */
    await removeSequence(graphqlClient, sequencePk);
    await removeExpansionRun(graphqlClient, expansionRunPk);
    await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
    await Promise.all([
      removeActivityDirective(graphqlClient, activityId1, planId),
      removeActivityDirective(graphqlClient, activityId2, planId),
      removeActivityDirective(graphqlClient, activityId3, planId),
    ]);
    await removeExpansionSet(graphqlClient, expansionSetId);
    /** End Cleanup */
  }, 30000);

  it('should return sequence seqjson in bulk', async () => {
    /** Begin Setup */
    // Create Expansion Set
    const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
      expansionId1,
      expansionId2,
      expansionId3,
    ]);

    // Create Activity Directives
    const [activityId1, activityId2, activityId3, activityId4, activityId5, activityId6] = await Promise.all([
      insertActivityDirective(graphqlClient, planId, 'GrowBanana'),
      insertActivityDirective(graphqlClient, planId, 'PeelBanana', '30 minutes'),
      insertActivityDirective(graphqlClient, planId, 'ThrowBanana', '60 minutes'),
      insertActivityDirective(graphqlClient, planId, 'GrowBanana', '90 minutes'),
      insertActivityDirective(graphqlClient, planId, 'PeelBanana', '120 minutes'),
      insertActivityDirective(graphqlClient, planId, 'ThrowBanana', '150 minutes'),
    ]);

    // Simulate Plan
    const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
    // Expand Plan to Sequence Fragments
    const expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
    // Create Sequence
    const [sequencePk1, sequencePk2] = await Promise.all([
      insertSequence(graphqlClient, {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      }),
      insertSequence(graphqlClient, {
        seqId: 'test00001',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      }),
    ]);
    // Link Activity Instances to Sequence
    await Promise.all([
      linkActivityInstance(graphqlClient, sequencePk1, activityId1),
      linkActivityInstance(graphqlClient, sequencePk1, activityId2),
      linkActivityInstance(graphqlClient, sequencePk1, activityId3),
      linkActivityInstance(graphqlClient, sequencePk2, activityId4),
      linkActivityInstance(graphqlClient, sequencePk2, activityId5),
      linkActivityInstance(graphqlClient, sequencePk2, activityId6),
    ]);

    // Get the simulated activity ids
    const [
      simulatedActivityId1,
      simulatedActivityId2,
      simulatedActivityId3,
      simulatedActivityId4,
      simulatedActivityId5,
      simulatedActivityId6,
    ] = await Promise.all([
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId1,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId2,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId3,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId4,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId5,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId6,
      ),
    ]);

    /** End Setup */

    // Retrieve seqJson
    const getSequenceSeqJsonBulkResponse = await getSequenceSeqJsonBulk(graphqlClient, [
      {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      },
      {
        seqId: 'test00001',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      },
    ]);

    const firstSequence = getSequenceSeqJsonBulkResponse[0]!;

    if (firstSequence.status === FallibleStatus.FAILURE) {
      throw firstSequence.errors;
    }

    expect(firstSequence.seqJson.id).toBe('test00000');
    expect(firstSequence.seqJson.metadata).toEqual({
      planId: planId,
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      timeSorted: false,
    });
    expect(firstSequence.seqJson.steps).toEqual([
      {
        // expansion 1
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        // expansion 2
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        // expansion 4
        type: 'command',
        stem: 'ADD_WATER',
        time: {
          tag: '2020-060T03:45:19.000',
          type: 'ABSOLUTE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '2025-358T12:01:59.000',
          type: 'ABSOLUTE',
        },
        args: [{ value: 360, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '00:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [{ value: 425, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '01:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: {
          tag: '-001T12:16:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '04:56:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Dole',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Blue',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1034 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyA',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 100000,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyB',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 10100,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
    ]);

    const secondSequence = getSequenceSeqJsonBulkResponse[1]!;

    if (secondSequence.status === FallibleStatus.FAILURE) {
      throw secondSequence.errors;
    }

    expect(secondSequence.seqJson.id).toBe('test00001');
    expect(secondSequence.seqJson.metadata).toEqual({
      planId: planId,
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      timeSorted: false,
    });
    expect(secondSequence.seqJson.steps).toEqual([
      {
        // expansion 1
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        // expansion 2
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId5 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId5 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId5 },
      },
      {
        // expansion 4
        type: 'command',
        stem: 'ADD_WATER',
        time: {
          tag: '2020-060T03:45:19.000',
          type: 'ABSOLUTE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '2025-358T12:01:59.000',
          type: 'ABSOLUTE',
        },
        args: [{ value: 360, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '00:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [{ value: 425, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '01:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: {
          tag: '-001T12:16:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '04:56:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Dole',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Blue',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1034 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyA',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 100000,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyB',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 10100,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
    ]);

    /** Begin Cleanup */
    await Promise.all([removeSequence(graphqlClient, sequencePk1), removeSequence(graphqlClient, sequencePk2)]);
    await removeExpansionRun(graphqlClient, expansionRunPk);
    await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
    await Promise.all([
      removeActivityDirective(graphqlClient, activityId1, planId),
      removeActivityDirective(graphqlClient, activityId2, planId),
      removeActivityDirective(graphqlClient, activityId3, planId),
      removeActivityDirective(graphqlClient, activityId4, planId),
      removeActivityDirective(graphqlClient, activityId5, planId),
      removeActivityDirective(graphqlClient, activityId6, planId),
    ]);
    await removeExpansionSet(graphqlClient, expansionSetId);
    /** End Cleanup */
  }, 30000);

  it('should work for throwing expansions', async () => {
    /** Begin Setup */
    // Add throwing expansion
    const expansionId4 = await insertExpansion(
      graphqlClient,
      'BiteBanana',
      `
      export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
        throw new Error('Unimplemented');
      }
      `,
    );

    // Create Expansion Set
    const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
      expansionId1,
      expansionId2,
      expansionId3,
      expansionId4,
    ]);

    // Create Activity Directives
    const [activityId1, activityId2, activityId3, activityId4] = await Promise.all([
      insertActivityDirective(graphqlClient, planId, 'GrowBanana'),
      insertActivityDirective(graphqlClient, planId, 'PeelBanana', '30 minutes'),
      insertActivityDirective(graphqlClient, planId, 'ThrowBanana', '60 minutes'),
      insertActivityDirective(graphqlClient, planId, 'BiteBanana', '90 minutes'),
    ]);

    // Simulate Plan
    const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
    // Expand Plan to Sequence Fragments
    const expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
    // Create Sequence
    const sequencePk = await insertSequence(graphqlClient, {
      seqId: 'test00000',
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
    });
    // Link Activity Instances to Sequence
    await Promise.all([
      linkActivityInstance(graphqlClient, sequencePk, activityId1),
      linkActivityInstance(graphqlClient, sequencePk, activityId2),
      linkActivityInstance(graphqlClient, sequencePk, activityId3),
      linkActivityInstance(graphqlClient, sequencePk, activityId4),
    ]);

    // Get the simulated activity ids
    const [simulatedActivityId1, simulatedActivityId2, simulatedActivityId3, simulatedActivityId4] = await Promise.all([
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId1,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId2,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId3,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId4,
      ),
    ]);

    /** End Setup */

    // Retrieve seqJson
    const getSequenceSeqJsonResponse = await getSequenceSeqJson(
      graphqlClient,
      'test00000',
      simulationArtifactPk.simulationDatasetId,
    );

    expect(getSequenceSeqJsonResponse.errors).toIncludeAllMembers([
      { message: 'Error: Unimplemented', stack: 'at SingleCommandExpansion(3:14)' },
    ]);

    expect(getSequenceSeqJsonResponse.seqJson?.id).toBe('test00000');
    expect(getSequenceSeqJsonResponse.seqJson?.metadata).toEqual({
      planId: planId,
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      timeSorted: false,
    });
    expect(getSequenceSeqJsonResponse.seqJson?.steps).toEqual([
      {
        // expansion 1
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        // expansion 2
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        // expansion 4
        type: 'command',
        stem: 'ADD_WATER',
        time: {
          tag: '2020-060T03:45:19.000',
          type: 'ABSOLUTE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '2025-358T12:01:59.000',
          type: 'ABSOLUTE',
        },
        args: [{ value: 360, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '00:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [{ value: 425, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '01:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: {
          tag: '-001T12:16:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '04:56:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Dole',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Blue',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1034 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyA',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 100000,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyB',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 10100,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: '$$ERROR$$',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ name: 'message', type: 'string', value: 'Error: Unimplemented' }],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
    ]);

    /** Begin Cleanup */
    await removeSequence(graphqlClient, sequencePk);
    await removeExpansionRun(graphqlClient, expansionRunPk);
    await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
    await Promise.all([
      removeActivityDirective(graphqlClient, activityId1, planId),
      removeActivityDirective(graphqlClient, activityId2, planId),
      removeActivityDirective(graphqlClient, activityId3, planId),
    ]);
    await removeExpansion(graphqlClient, expansionId4);
    await removeExpansionSet(graphqlClient, expansionSetId);
    /** End Cleanup */
  }, 30000);

  it('should work for throwing expansions in bulk', async () => {
    /** Begin Setup */
    const expansionId4 = await insertExpansion(
      graphqlClient,
      'BiteBanana',
      `
      export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
        throw new Error('Unimplemented');
      }
      `,
    );
    // Create Expansion Set
    const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
      expansionId1,
      expansionId2,
      expansionId3,
      expansionId4,
    ]);

    // Create Activity Directives
    const [activityId1, activityId2, activityId3, activityId4, activityId5, activityId6, activityId7, activityId8] =
      await Promise.all([
        insertActivityDirective(graphqlClient, planId, 'GrowBanana'),
        insertActivityDirective(graphqlClient, planId, 'PeelBanana', '30 minutes'),
        insertActivityDirective(graphqlClient, planId, 'ThrowBanana', '60 minutes'),
        insertActivityDirective(graphqlClient, planId, 'BiteBanana', '90 minutes'),
        insertActivityDirective(graphqlClient, planId, 'GrowBanana', '120 minutes'),
        insertActivityDirective(graphqlClient, planId, 'PeelBanana', '150 minutes'),
        insertActivityDirective(graphqlClient, planId, 'ThrowBanana', '180 minutes'),
        insertActivityDirective(graphqlClient, planId, 'BiteBanana', '210 minutes'),
      ]);

    // Simulate Plan
    const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
    // Expand Plan to Sequence Fragments
    const expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
    // Create Sequence
    const [sequencePk1, sequencePk2] = await Promise.all([
      insertSequence(graphqlClient, {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      }),
      insertSequence(graphqlClient, {
        seqId: 'test00001',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      }),
    ]);
    // Link Activity Instances to Sequence
    await Promise.all([
      linkActivityInstance(graphqlClient, sequencePk1, activityId1),
      linkActivityInstance(graphqlClient, sequencePk1, activityId2),
      linkActivityInstance(graphqlClient, sequencePk1, activityId3),
      linkActivityInstance(graphqlClient, sequencePk1, activityId4),
      linkActivityInstance(graphqlClient, sequencePk2, activityId5),
      linkActivityInstance(graphqlClient, sequencePk2, activityId6),
      linkActivityInstance(graphqlClient, sequencePk2, activityId7),
      linkActivityInstance(graphqlClient, sequencePk2, activityId8),
    ]);

    // Get the simulated activity ids
    const [
      simulatedActivityId1,
      simulatedActivityId2,
      simulatedActivityId3,
      simulatedActivityId4,
      simulatedActivityId5,
      simulatedActivityId6,
      simulatedActivityId7,
      simulatedActivityId8,
    ] = await Promise.all([
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId1,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId2,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId3,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId4,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId5,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId6,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId7,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId8,
      ),
    ]);

    /** End Setup */

    // Retrieve seqJson
    const getSequenceSeqJsonBulkResponse = await getSequenceSeqJsonBulk(graphqlClient, [
      {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      },
      {
        seqId: 'test00001',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      },
    ]);

    const firstSequence = getSequenceSeqJsonBulkResponse[0]!;

    expect(firstSequence.seqJson?.id).toBe('test00000');
    expect(firstSequence.seqJson?.metadata).toEqual({
      planId: planId,
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      timeSorted: false,
    });
    expect(firstSequence.seqJson?.steps).toEqual([
      {
        // expansion 1
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        // expansion 2
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        // expansion 4
        type: 'command',
        stem: 'ADD_WATER',
        time: {
          tag: '2020-060T03:45:19.000',
          type: 'ABSOLUTE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '2025-358T12:01:59.000',
          type: 'ABSOLUTE',
        },
        args: [{ value: 360, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '00:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [{ value: 425, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '01:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: {
          tag: '-001T12:16:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '04:56:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Dole',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Blue',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1034 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyA',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 100000,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyB',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 10100,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: '$$ERROR$$',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ name: 'message', type: 'string', value: 'Error: Unimplemented' }],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
    ]);

    const secondSequence = getSequenceSeqJsonBulkResponse[1]!;

    expect(secondSequence.seqJson?.id).toBe('test00001');
    expect(secondSequence.seqJson?.metadata).toEqual({
      planId: planId,
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      timeSorted: false,
    });
    expect(secondSequence.seqJson?.steps).toEqual([
      {
        // expansion 1
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId5 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId5 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId5 },
      },
      {
        // expansion 2
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        // expansion 4
        type: 'command',
        stem: 'ADD_WATER',
        time: {
          tag: '2020-060T03:45:19.000',
          type: 'ABSOLUTE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '2025-358T12:01:59.000',
          type: 'ABSOLUTE',
        },
        args: [{ value: 360, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '00:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [{ value: 425, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '01:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: {
          tag: '-001T12:16:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '04:56:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Dole',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Blue',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1034 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyA',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 100000,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyB',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 10100,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: '$$ERROR$$',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ name: 'message', type: 'string', value: 'Error: Unimplemented' }],
        metadata: { simulatedActivityId: simulatedActivityId8 },
      },
    ]);

    /** Begin Cleanup */
    await Promise.all([removeSequence(graphqlClient, sequencePk1), removeSequence(graphqlClient, sequencePk2)]);
    await removeExpansionRun(graphqlClient, expansionRunPk);
    await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
    await Promise.all([
      removeActivityDirective(graphqlClient, activityId1, planId),
      removeActivityDirective(graphqlClient, activityId2, planId),
      removeActivityDirective(graphqlClient, activityId3, planId),
      removeActivityDirective(graphqlClient, activityId4, planId),
      removeActivityDirective(graphqlClient, activityId5, planId),
      removeActivityDirective(graphqlClient, activityId6, planId),
      removeActivityDirective(graphqlClient, activityId7, planId),
      removeActivityDirective(graphqlClient, activityId8, planId),
    ]);
    await removeExpansion(graphqlClient, expansionId4);
    await removeExpansionSet(graphqlClient, expansionSetId);
    /** End Cleanup */
  }, 30000);

  it('should work for non-existent expansions', async () => {
    /** Begin Setup */
    // Create Expansion Set
    const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
      expansionId1,
      expansionId2,
      expansionId3,
    ]);

    // Create Activity Directives
    const [activityId1, activityId2, activityId3, activityId4] = await Promise.all([
      insertActivityDirective(graphqlClient, planId, 'GrowBanana'),
      insertActivityDirective(graphqlClient, planId, 'PeelBanana', '30 minutes'),
      insertActivityDirective(graphqlClient, planId, 'ThrowBanana', '60 minutes'),
      insertActivityDirective(graphqlClient, planId, 'BiteBanana', '90 minutes'), // non-existent expansion
    ]);

    // Simulate Plan
    const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
    // Expand Plan to Sequence Fragments
    const expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
    // Create Sequence
    const sequencePk = await insertSequence(graphqlClient, {
      seqId: 'test00000',
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
    });
    // Link Activity Instances to Sequence
    await Promise.all([
      linkActivityInstance(graphqlClient, sequencePk, activityId1),
      linkActivityInstance(graphqlClient, sequencePk, activityId2),
      linkActivityInstance(graphqlClient, sequencePk, activityId3),
      linkActivityInstance(graphqlClient, sequencePk, activityId4),
    ]);

    // Get the simulated activity ids
    const [
      simulatedActivityId1,
      simulatedActivityId2,
      simulatedActivityId3,
      _simulatedActivityId4, // No expansion, so no check required on this one
    ] = await Promise.all([
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId1,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId2,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId3,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId4,
      ),
    ]);

    /** End Setup */

    // Retrieve seqJson
    const getSequenceSeqJsonResponse = await getSequenceSeqJson(
      graphqlClient,
      'test00000',
      simulationArtifactPk.simulationDatasetId,
    );

    if (getSequenceSeqJsonResponse.status !== FallibleStatus.SUCCESS) {
      throw getSequenceSeqJsonResponse.errors;
    }

    expect(getSequenceSeqJsonResponse.seqJson.id).toBe('test00000');
    expect(getSequenceSeqJsonResponse.seqJson.metadata).toEqual({
      planId: planId,
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      timeSorted: false,
    });
    expect(getSequenceSeqJsonResponse.seqJson.steps).toEqual([
      {
        // expansion 1
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        // expansion 2
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        // expansion 4
        type: 'command',
        stem: 'ADD_WATER',
        time: {
          tag: '2020-060T03:45:19.000',
          type: 'ABSOLUTE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '2025-358T12:01:59.000',
          type: 'ABSOLUTE',
        },
        args: [{ value: 360, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '00:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [{ value: 425, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '01:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: {
          tag: '-001T12:16:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '04:56:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Dole',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Blue',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1034 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyA',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 100000,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyB',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 10100,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
    ]);

    /** Begin Cleanup */
    await removeSequence(graphqlClient, sequencePk);
    await removeExpansionRun(graphqlClient, expansionRunPk);
    await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
    await Promise.all([
      removeActivityDirective(graphqlClient, activityId1, planId),
      removeActivityDirective(graphqlClient, activityId2, planId),
      removeActivityDirective(graphqlClient, activityId3, planId),
    ]);
    await removeExpansionSet(graphqlClient, expansionSetId);
    /** End Cleanup */
  }, 30000);

  it('should work for non-existent expansions in bulk', async () => {
    /** Begin Setup */
    // Create Expansion Set
    const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
      expansionId1,
      expansionId2,
      expansionId3,
    ]);

    // Create Activity Directives
    const [activityId1, activityId2, activityId3, activityId4, activityId5, activityId6, activityId7, activityId8] =
      await Promise.all([
        insertActivityDirective(graphqlClient, planId, 'GrowBanana'),
        insertActivityDirective(graphqlClient, planId, 'PeelBanana', '30 minutes'),
        insertActivityDirective(graphqlClient, planId, 'ThrowBanana', '60 minutes'),
        insertActivityDirective(graphqlClient, planId, 'BiteBanana', '90 minutes'), // non-existent expansion
        insertActivityDirective(graphqlClient, planId, 'GrowBanana', '120 minutes'),
        insertActivityDirective(graphqlClient, planId, 'PeelBanana', '150 minutes'),
        insertActivityDirective(graphqlClient, planId, 'ThrowBanana', '180 minutes'),
        insertActivityDirective(graphqlClient, planId, 'BiteBanana', '210 minutes'), // non-existent expansion
      ]);

    // Simulate Plan
    const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
    // Expand Plan to Sequence Fragments
    const expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
    // Create Sequence
    const [sequencePk1, sequencePk2] = await Promise.all([
      insertSequence(graphqlClient, {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      }),
      insertSequence(graphqlClient, {
        seqId: 'test00001',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      }),
    ]);
    // Link Activity Instances to Sequence
    await Promise.all([
      linkActivityInstance(graphqlClient, sequencePk1, activityId1),
      linkActivityInstance(graphqlClient, sequencePk1, activityId2),
      linkActivityInstance(graphqlClient, sequencePk1, activityId3),
      linkActivityInstance(graphqlClient, sequencePk1, activityId4),
      linkActivityInstance(graphqlClient, sequencePk2, activityId5),
      linkActivityInstance(graphqlClient, sequencePk2, activityId6),
      linkActivityInstance(graphqlClient, sequencePk2, activityId7),
      linkActivityInstance(graphqlClient, sequencePk2, activityId8),
    ]);

    // Get the simulated activity ids
    const [
      simulatedActivityId1,
      simulatedActivityId2,
      simulatedActivityId3,
      _simulatedActivityId4, // No expansion, so no check required on this one
      simulatedActivityId5,
      simulatedActivityId6,
      simulatedActivityId7,
      _simulatedActivityId8, // No expansion, so no check required on this one
    ] = await Promise.all([
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId1,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId2,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId3,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId4,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId5,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId6,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId7,
      ),
      convertActivityDirectiveIdToSimulatedActivityId(
        graphqlClient,
        simulationArtifactPk.simulationDatasetId,
        activityId8,
      ),
    ]);

    /** End Setup */

    // Retrieve seqJson
    const getSequenceSeqJsonResponse = await getSequenceSeqJsonBulk(graphqlClient, [
      { seqId: 'test00000', simulationDatasetId: simulationArtifactPk.simulationDatasetId },
      { seqId: 'test00001', simulationDatasetId: simulationArtifactPk.simulationDatasetId },
    ]);

    const firstSequence = getSequenceSeqJsonResponse[0]!;

    if (firstSequence.status !== FallibleStatus.SUCCESS) {
      throw firstSequence.errors;
    }

    expect(firstSequence.seqJson.id).toBe('test00000');
    expect(firstSequence.seqJson.metadata).toEqual({
      planId: planId,
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      timeSorted: false,
    });
    expect(firstSequence.seqJson.steps).toEqual([
      {
        // expansion 1
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        // expansion 2
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        // expansion 4
        type: 'command',
        stem: 'ADD_WATER',
        time: {
          tag: '2020-060T03:45:19.000',
          type: 'ABSOLUTE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '2025-358T12:01:59.000',
          type: 'ABSOLUTE',
        },
        args: [{ value: 360, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '00:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [{ value: 425, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '01:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: {
          tag: '-001T12:16:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '04:56:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Dole',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Blue',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1034 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyA',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 100000,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyB',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 10100,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
    ]);

    const secondSequence = getSequenceSeqJsonResponse[1]!;

    if (secondSequence.status !== FallibleStatus.SUCCESS) {
      throw secondSequence.errors;
    }

    expect(secondSequence.seqJson.id).toBe('test00001');
    expect(secondSequence.seqJson.metadata).toEqual({
      planId: planId,
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      timeSorted: false,
    });
    expect(secondSequence.seqJson.steps).toEqual([
      {
        // expansion 1
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId5 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId5 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId5 },
      },
      {
        // expansion 2
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId6 },
      },
      {
        // expansion 4
        type: 'command',
        stem: 'ADD_WATER',
        time: {
          tag: '2020-060T03:45:19.000',
          type: 'ABSOLUTE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '2025-358T12:01:59.000',
          type: 'ABSOLUTE',
        },
        args: [{ value: 360, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '00:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [{ value: 425, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '01:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: {
          tag: '-001T12:16:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: 'FALSE', name: 'gluten_free', type: 'string' },
        ],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '04:56:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Dole',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Blue',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [
          { name: 'lot_number', type: 'number', value: 1034 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyA',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 100000,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'companyB',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 10100,
                },
              ],
            ],
          },
        ],
        metadata: { simulatedActivityId: simulatedActivityId7 },
      },
    ]);

    /** Begin Cleanup */
    await Promise.all([removeSequence(graphqlClient, sequencePk1), removeSequence(graphqlClient, sequencePk2)]);
    await removeExpansionRun(graphqlClient, expansionRunPk);
    await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
    await Promise.all([
      removeActivityDirective(graphqlClient, activityId1, planId),
      removeActivityDirective(graphqlClient, activityId2, planId),
      removeActivityDirective(graphqlClient, activityId3, planId),
      removeActivityDirective(graphqlClient, activityId4, planId),
      removeActivityDirective(graphqlClient, activityId5, planId),
      removeActivityDirective(graphqlClient, activityId6, planId),
      removeActivityDirective(graphqlClient, activityId7, planId),
      removeActivityDirective(graphqlClient, activityId8, planId),
    ]);
    await removeExpansionSet(graphqlClient, expansionSetId);
    /** End Cleanup */
  }, 30000);

  describe('step sorting', () => {
    it('should sort expansions correctly with relative and absolute times', async () => {
      /** Begin Setup */
      const expansionId = await insertExpansion(
        graphqlClient,
        'GrowBanana',
        `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        A\`2023-091T08:19:00.000\`.ADD_WATER,
        R\`04:00:00.000\`.PICK_BANANA,
        A\`2023-091T04:20:00.000\`.GROW_BANANA({ quantity: 10, durationSecs: 7200 })
      ];
    }
    `,
      );
      // Create Expansion Set
      const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
        expansionId,
      ]);

      // Create Activity Directives
      const [activityId1, activityId2] = await Promise.all([
        insertActivityDirective(graphqlClient, planId, 'GrowBanana'),
        insertActivityDirective(graphqlClient, planId, 'GrowBanana', '30 minutes'),
      ]);

      // Simulate Plan
      const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
      // Expand Plan to Sequence Fragments
      const expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
      // Create Sequence
      const sequencePk = await insertSequence(graphqlClient, {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      });
      // Link Activity Instances to Sequence
      await Promise.all([
        linkActivityInstance(graphqlClient, sequencePk, activityId1),
        linkActivityInstance(graphqlClient, sequencePk, activityId2),
      ]);

      // Get the simulated activity ids
      const [simulatedActivityId1, simulatedActivityId2] = await Promise.all([
        convertActivityDirectiveIdToSimulatedActivityId(
          graphqlClient,
          simulationArtifactPk.simulationDatasetId,
          activityId1,
        ),
        convertActivityDirectiveIdToSimulatedActivityId(
          graphqlClient,
          simulationArtifactPk.simulationDatasetId,
          activityId2,
        ),
      ]);
      /** End Setup */

      // Retrieve seqJson
      const getSequenceSeqJsonResponse = await getSequenceSeqJson(
        graphqlClient,
        'test00000',
        simulationArtifactPk.simulationDatasetId,
      );

      if (getSequenceSeqJsonResponse.status !== FallibleStatus.SUCCESS) {
        throw getSequenceSeqJsonResponse.errors;
      }

      expect(getSequenceSeqJsonResponse.seqJson.id).toBe('test00000');
      expect(getSequenceSeqJsonResponse.seqJson.metadata).toEqual({
        planId: planId,
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
        timeSorted: true,
      });

      expect(getSequenceSeqJsonResponse.seqJson.steps).toEqual([
        {
          type: 'command',
          stem: 'GROW_BANANA',
          time: { tag: '2023-091T04:20:00.000', type: TimingTypes.ABSOLUTE },
          args: [
            { value: 10, name: 'quantity', type: 'number' },
            { value: 7200, name: 'durationSecs', type: 'number' },
          ],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
        {
          type: 'command',
          stem: 'GROW_BANANA',
          time: { tag: '2023-091T04:20:00.000', type: TimingTypes.ABSOLUTE },
          args: [
            { value: 10, name: 'quantity', type: 'number' },
            { value: 7200, name: 'durationSecs', type: 'number' },
          ],
          metadata: { simulatedActivityId: simulatedActivityId2 },
        },
        {
          type: 'command',
          stem: 'ADD_WATER',
          time: { tag: '2023-091T08:19:00.000', type: TimingTypes.ABSOLUTE },
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
        {
          type: 'command',
          stem: 'ADD_WATER',
          time: { tag: '2023-091T08:19:00.000', type: TimingTypes.ABSOLUTE },
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId2 },
        },
        {
          type: 'command',
          stem: 'PICK_BANANA',
          time: { tag: '2023-091T12:19:00.000', type: TimingTypes.ABSOLUTE },
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
        {
          type: 'command',
          stem: 'PICK_BANANA',
          time: { tag: '2023-091T12:19:00.000', type: TimingTypes.ABSOLUTE },
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId2 },
        },
      ]);

      /** Begin Cleanup */
      await removeSequence(graphqlClient, sequencePk);
      await removeExpansionRun(graphqlClient, expansionRunPk);
      await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
      await Promise.all([
        removeActivityDirective(graphqlClient, activityId1, planId),
        removeActivityDirective(graphqlClient, activityId2, planId),
      ]);
      await removeExpansionSet(graphqlClient, expansionSetId);
      /** End Cleanup */
    }, 30000);

    it('should sort expanded commands correctly with relative and absolute times for multiple activities', async () => {
      /** Begin Setup */
      const expansionId1 = await insertExpansion(
        graphqlClient,
        'PickBanana',
        `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        A\`2023-091T08:00:00.000\`.ADD_WATER,
        R\`04:00:00.000\`.PICK_BANANA,
      ];
    }
    `,
      );

      const expansionId2 = await insertExpansion(
        graphqlClient,
        'GrowBanana',
        `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        A\`2023-091T10:00:00.000\`.ADD_WATER,
        R\`04:00:00.000\`.GROW_BANANA({ quantity: 10, durationSecs: 7200 })
      ];
    }
    `,
      );
      // Create Expansion Set
      const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
        expansionId1,
        expansionId2,
      ]);

      // Create Activity Directives
      const [activityId1, activityId2] = await Promise.all([
        insertActivityDirective(graphqlClient, planId, 'PickBanana'),
        insertActivityDirective(graphqlClient, planId, 'GrowBanana', '30 minutes'),
      ]);

      // Simulate Plan
      const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
      // Expand Plan to Sequence Fragments
      const expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
      // Create Sequence
      const sequencePk = await insertSequence(graphqlClient, {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      });
      // Link Activity Instances to Sequence
      await Promise.all([
        linkActivityInstance(graphqlClient, sequencePk, activityId1),
        linkActivityInstance(graphqlClient, sequencePk, activityId2),
      ]);

      // Get the simulated activity ids
      const [simulatedActivityId1, simulatedActivityId2] = await Promise.all([
        convertActivityDirectiveIdToSimulatedActivityId(
          graphqlClient,
          simulationArtifactPk.simulationDatasetId,
          activityId1,
        ),
        convertActivityDirectiveIdToSimulatedActivityId(
          graphqlClient,
          simulationArtifactPk.simulationDatasetId,
          activityId2,
        ),
      ]);
      /** End Setup */

      // Retrieve seqJson
      const getSequenceSeqJsonResponse = await getSequenceSeqJson(
        graphqlClient,
        'test00000',
        simulationArtifactPk.simulationDatasetId,
      );

      if (getSequenceSeqJsonResponse.status !== FallibleStatus.SUCCESS) {
        throw getSequenceSeqJsonResponse.errors;
      }

      expect(getSequenceSeqJsonResponse.seqJson.id).toBe('test00000');
      expect(getSequenceSeqJsonResponse.seqJson.metadata).toEqual({
        planId: planId,
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
        timeSorted: true,
      });

      expect(getSequenceSeqJsonResponse.seqJson.steps).toEqual([
        {
          type: 'command',
          stem: 'ADD_WATER',
          time: { tag: '2023-091T08:00:00.000', type: TimingTypes.ABSOLUTE },
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
        {
          type: 'command',
          stem: 'ADD_WATER',
          time: { tag: '2023-091T10:00:00.000', type: TimingTypes.ABSOLUTE },
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId2 },
        },
        {
          type: 'command',
          stem: 'PICK_BANANA',
          time: { tag: '2023-091T12:00:00.000', type: TimingTypes.ABSOLUTE },
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
        {
          type: 'command',
          stem: 'GROW_BANANA',
          time: { tag: '2023-091T14:00:00.000', type: TimingTypes.ABSOLUTE },
          args: [
            { value: 10, name: 'quantity', type: 'number' },
            { value: 7200, name: 'durationSecs', type: 'number' },
          ],
          metadata: { simulatedActivityId: simulatedActivityId2 },
        },
      ]);

      /** Begin Cleanup */
      await removeSequence(graphqlClient, sequencePk);
      await removeExpansionRun(graphqlClient, expansionRunPk);
      await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
      await Promise.all([
        removeActivityDirective(graphqlClient, activityId1, planId),
        removeActivityDirective(graphqlClient, activityId2, planId),
      ]);
      await removeExpansionSet(graphqlClient, expansionSetId);
      await removeExpansionRun(graphqlClient, expansionRunPk);
      await removeExpansion(graphqlClient, expansionId1);
      await removeExpansion(graphqlClient, expansionId2);
      /** End Cleanup */
    }, 30000);

    it('should not sort expansions if there is only one activity instance', async () => {
      /** Begin Setup */
      const expansionId = await insertExpansion(
        graphqlClient,
        'GrowBanana',
        `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        A\`2023-091T08:19:00.000\`.ADD_WATER,
        R\`04:00:00.000\`.PICK_BANANA,
        A\`2023-091T04:20:00.000\`.GROW_BANANA({ quantity: 10, durationSecs: 7200 })
      ];
    }
    `,
      );
      // Create Expansion Set
      const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
        expansionId,
      ]);

      // Create Activity Directives
      const [activityId1] = await Promise.all([insertActivityDirective(graphqlClient, planId, 'GrowBanana')]);

      // Simulate Plan
      const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
      // Expand Plan to Sequence Fragments
      const expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
      // Create Sequence
      const sequencePk = await insertSequence(graphqlClient, {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      });
      // Link Activity Instances to Sequence
      await Promise.all([linkActivityInstance(graphqlClient, sequencePk, activityId1)]);

      // Get the simulated activity ids
      const [simulatedActivityId1] = await Promise.all([
        convertActivityDirectiveIdToSimulatedActivityId(
          graphqlClient,
          simulationArtifactPk.simulationDatasetId,
          activityId1,
        ),
      ]);
      /** End Setup */

      // Retrieve seqJson
      const getSequenceSeqJsonResponse = await getSequenceSeqJson(
        graphqlClient,
        'test00000',
        simulationArtifactPk.simulationDatasetId,
      );

      if (getSequenceSeqJsonResponse.status !== FallibleStatus.SUCCESS) {
        throw getSequenceSeqJsonResponse.errors;
      }

      expect(getSequenceSeqJsonResponse.seqJson.id).toBe('test00000');
      expect(getSequenceSeqJsonResponse.seqJson.metadata).toEqual({
        planId: planId,
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
        timeSorted: false,
      });

      expect(getSequenceSeqJsonResponse.seqJson.steps).toEqual([
        {
          type: 'command',
          stem: 'ADD_WATER',
          time: { tag: '2023-091T08:19:00.000', type: TimingTypes.ABSOLUTE },
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
        {
          type: 'command',
          stem: 'PICK_BANANA',
          time: { tag: '04:00:00.000', type: TimingTypes.COMMAND_RELATIVE },
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
        {
          type: 'command',
          stem: 'GROW_BANANA',
          time: { tag: '2023-091T04:20:00.000', type: TimingTypes.ABSOLUTE },
          args: [
            { value: 10, name: 'quantity', type: 'number' },
            { value: 7200, name: 'durationSecs', type: 'number' },
          ],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
      ]);

      /** Begin Cleanup */
      await removeSequence(graphqlClient, sequencePk);
      await removeExpansionRun(graphqlClient, expansionRunPk);
      await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
      await Promise.all([removeActivityDirective(graphqlClient, activityId1, planId)]);
      await removeExpansionSet(graphqlClient, expansionSetId);
      /** End Cleanup */
    }, 30000);

    it('should not sort if the expansion contains complete commands', async () => {
      const expansionId = await insertExpansion(
        graphqlClient,
        'GrowBanana',
        `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        A\`2023-091T08:19:00.000\`.ADD_WATER,
        C.PICK_BANANA,
        A\`2023-091T04:20:00.000\`.GROW_BANANA({ quantity: 10, durationSecs: 7200 })
      ];
    }
    `,
      );
      /** Begin Setup */
      // Create Expansion Set
      const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
        expansionId,
      ]);

      // Create Activity Directives
      const [activityId1, activityId2, activityId3] = await Promise.all([
        insertActivityDirective(graphqlClient, planId, 'GrowBanana'),
        insertActivityDirective(graphqlClient, planId, 'PeelBanana', '30 minutes'),
        insertActivityDirective(graphqlClient, planId, 'ThrowBanana', '60 minutes'),
      ]);

      // Simulate Plan
      const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
      // Expand Plan to Sequence Fragments
      const expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
      // Create Sequence
      const sequencePk = await insertSequence(graphqlClient, {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      });
      // Link Activity Instances to Sequence
      await Promise.all([
        linkActivityInstance(graphqlClient, sequencePk, activityId1),
        linkActivityInstance(graphqlClient, sequencePk, activityId2),
        linkActivityInstance(graphqlClient, sequencePk, activityId3),
      ]);

      // Get the simulated activity ids
      const [simulatedActivityId1] = await Promise.all([
        convertActivityDirectiveIdToSimulatedActivityId(
          graphqlClient,
          simulationArtifactPk.simulationDatasetId,
          activityId1,
        ),
      ]);
      /** End Setup */

      // Retrieve seqJson
      const getSequenceSeqJsonResponse = await getSequenceSeqJson(
        graphqlClient,
        'test00000',
        simulationArtifactPk.simulationDatasetId,
      );

      if (getSequenceSeqJsonResponse.status !== FallibleStatus.SUCCESS) {
        throw getSequenceSeqJsonResponse.errors;
      }

      expect(getSequenceSeqJsonResponse.seqJson.id).toBe('test00000');
      expect(getSequenceSeqJsonResponse.seqJson.metadata).toEqual({
        planId: planId,
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
        timeSorted: false,
      });

      expect(getSequenceSeqJsonResponse.seqJson.steps).toEqual([
        {
          type: 'command',
          stem: 'ADD_WATER',
          time: { tag: '2023-091T08:19:00.000', type: TimingTypes.ABSOLUTE },
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
        {
          type: 'command',
          stem: 'PICK_BANANA',
          time: { type: TimingTypes.COMMAND_COMPLETE },
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
        {
          type: 'command',
          stem: 'GROW_BANANA',
          time: { tag: '2023-091T04:20:00.000', type: TimingTypes.ABSOLUTE },
          args: [
            { value: 10, name: 'quantity', type: 'number' },
            { value: 7200, name: 'durationSecs', type: 'number' },
          ],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
      ]);

      /** Begin Cleanup */
      await removeSequence(graphqlClient, sequencePk);
      await removeExpansionRun(graphqlClient, expansionRunPk);
      await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
      await Promise.all([
        removeActivityDirective(graphqlClient, activityId1, planId),
        removeActivityDirective(graphqlClient, activityId2, planId),
        removeActivityDirective(graphqlClient, activityId3, planId),
      ]);
      await removeExpansionSet(graphqlClient, expansionSetId);
      /** End Cleanup */
    }, 30000);

    it('should not sort if the expansion contains epoch relative commands', async () => {
      const expansionId = await insertExpansion(
        graphqlClient,
        'GrowBanana',
        `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        A\`2023-091T08:19:00.000\`.ADD_WATER,
        E\`000T04:00:00.000\`.PICK_BANANA,
        A\`2023-091T04:20:00.000\`.GROW_BANANA({ quantity: 10, durationSecs: 7200 })
      ];
    }
    `,
      );
      /** Begin Setup */
      // Create Expansion Set
      const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
        expansionId,
      ]);

      // Create Activity Directives
      const [activityId1, activityId2, activityId3] = await Promise.all([
        insertActivityDirective(graphqlClient, planId, 'GrowBanana'),
        insertActivityDirective(graphqlClient, planId, 'PeelBanana', '30 minutes'),
        insertActivityDirective(graphqlClient, planId, 'ThrowBanana', '60 minutes'),
      ]);

      // Simulate Plan
      const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
      // Expand Plan to Sequence Fragments
      const expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
      // Create Sequence
      const sequencePk = await insertSequence(graphqlClient, {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      });
      // Link Activity Instances to Sequence
      await Promise.all([
        linkActivityInstance(graphqlClient, sequencePk, activityId1),
        linkActivityInstance(graphqlClient, sequencePk, activityId2),
        linkActivityInstance(graphqlClient, sequencePk, activityId3),
      ]);

      // Get the simulated activity ids
      const [simulatedActivityId1] = await Promise.all([
        convertActivityDirectiveIdToSimulatedActivityId(
          graphqlClient,
          simulationArtifactPk.simulationDatasetId,
          activityId1,
        ),
      ]);
      /** End Setup */

      // Retrieve seqJson
      const getSequenceSeqJsonResponse = await getSequenceSeqJson(
        graphqlClient,
        'test00000',
        simulationArtifactPk.simulationDatasetId,
      );

      if (getSequenceSeqJsonResponse.status !== FallibleStatus.SUCCESS) {
        throw getSequenceSeqJsonResponse.errors;
      }

      expect(getSequenceSeqJsonResponse.seqJson.id).toBe('test00000');
      expect(getSequenceSeqJsonResponse.seqJson.metadata).toEqual({
        planId: planId,
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
        timeSorted: false,
      });

      expect(getSequenceSeqJsonResponse.seqJson.steps).toEqual([
        {
          type: 'command',
          stem: 'ADD_WATER',
          time: { tag: '2023-091T08:19:00.000', type: TimingTypes.ABSOLUTE },
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
        {
          type: 'command',
          stem: 'PICK_BANANA',
          time: { tag: '04:00:00.000', type: TimingTypes.EPOCH_RELATIVE },
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
        {
          type: 'command',
          stem: 'GROW_BANANA',
          time: { tag: '2023-091T04:20:00.000', type: TimingTypes.ABSOLUTE },
          args: [
            { value: 10, name: 'quantity', type: 'number' },
            { value: 7200, name: 'durationSecs', type: 'number' },
          ],
          metadata: { simulatedActivityId: simulatedActivityId1 },
        },
      ]);

      /** Begin Cleanup */
      await removeSequence(graphqlClient, sequencePk);
      await removeExpansionRun(graphqlClient, expansionRunPk);
      await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
      await Promise.all([
        removeActivityDirective(graphqlClient, activityId1, planId),
        removeActivityDirective(graphqlClient, activityId2, planId),
        removeActivityDirective(graphqlClient, activityId3, planId),
      ]);
      await removeExpansionSet(graphqlClient, expansionSetId);
      /** End Cleanup */
    }, 30000);
  });
});

it('should provide start, end, and computed attributes on activities', async () => {
  // Setup

  const activityId = await insertActivityDirective(graphqlClient, planId, 'BakeBananaBread', '1 hours', {
    tbSugar: 1,
    glutenFree: false,
    temperature: 350,
  });
  const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
  const expansionId = await insertExpansion(
    graphqlClient,
    'BakeBananaBread',
    `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        A(props.activityInstance.startTime).BAKE_BREAD,
        A(props.activityInstance.endTime).BAKE_BREAD,
        C.ECHO({ echo_string: "Computed attributes: " + props.activityInstance.attributes.computed }),
      ];
    }
    `,
  );

  const expansionSet0Id = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [expansionId]);
  await expand(graphqlClient, expansionSet0Id, simulationArtifactPk.simulationDatasetId);
  const sequencePk = await insertSequence(graphqlClient, {
    seqId: 'test00000',
    simulationDatasetId: simulationArtifactPk.simulationDatasetId,
  });
  await linkActivityInstance(graphqlClient, sequencePk, activityId);

  const simulatedActivityId3 = await convertActivityDirectiveIdToSimulatedActivityId(
    graphqlClient,
    simulationArtifactPk.simulationDatasetId,
    activityId,
  );

  const getSequenceSeqJsonResponse = await getSequenceSeqJson(
    graphqlClient,
    'test00000',
    simulationArtifactPk.simulationDatasetId,
  );

  if (getSequenceSeqJsonResponse.status !== FallibleStatus.SUCCESS) {
    throw getSequenceSeqJsonResponse.errors;
  }

  expect(getSequenceSeqJsonResponse.seqJson.id).toBe('test00000');
  expect(getSequenceSeqJsonResponse.seqJson.metadata).toEqual({
    planId: planId,
    simulationDatasetId: simulationArtifactPk.simulationDatasetId,
    timeSorted: false,
  });
  expect(getSequenceSeqJsonResponse.seqJson.steps).toEqual([
    {
      type: 'command',
      stem: 'BAKE_BREAD',
      time: { type: TimingTypes.ABSOLUTE, tag: '2020-001T01:00:00.000' },
      args: [],
      metadata: { simulatedActivityId: simulatedActivityId3 },
    },
    {
      type: 'command',
      stem: 'BAKE_BREAD',
      time: { type: TimingTypes.ABSOLUTE, tag: '2020-001T01:00:00.000' },
      args: [],
      metadata: { simulatedActivityId: simulatedActivityId3 },
    },
    {
      type: 'command',
      stem: 'ECHO',
      time: { type: 'COMMAND_COMPLETE' },
      args: [{ name: 'echo_string', type: 'string', value: 'Computed attributes: 198' }],
      metadata: { simulatedActivityId: simulatedActivityId3 },
    },
  ]);

  // Cleanup
  {
    await removeSequence(graphqlClient, sequencePk);
  }
}, 30000);

describe('user sequence to seqjson', () => {
  it('generate sequence seqjson from static sequence', async () => {
    var results = await generateSequenceEDSL(
      graphqlClient,
      commandDictionaryId,
      `
      export default () =>
      Sequence.new({
        seqId: "test00001",
        metadata: {},
        steps: [
            C.BAKE_BREAD,
            A\`2020-060T03:45:19\`.PREHEAT_OVEN({ temperature: 100 }),
            E(Temporal.Duration.from({ hours: -12, minutes: -1, seconds: -54 })).PACKAGE_BANANA({
              lot_number: 1093,
              bundle: [
                {
                  bundle_name: "Chiquita",
                  number_of_bananas: 43
                },
                {
                  bundle_name: "Dole",
                  number_of_bananas: 12
                }
              ]
            }),
        ],
      });
    `,
    );

    expect(results.id).toBe('test00001');
    expect(results.metadata).toEqual({});
    expect(results.steps).toEqual([
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '2020-060T03:45:19.000',
          type: 'ABSOLUTE',
        },
        args: [{ name: 'temperature', type: 'number', value: 100 }],
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: {
          tag: '-12:01:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Dole',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
      },
    ]);
  }, 30000);

  it('generate sequence seqjson from static sequence in bulk', async () => {
    var results = await generateSequenceEDSLBulk(graphqlClient, [
      {
        commandDictionaryId,
        edslBody: `
            export default () =>
            Sequence.new({
              seqId: "test00001",
              metadata: {},
              steps: [
                  C.BAKE_BREAD.DESCRIPTION("Bake bread"),
                  A\`2020-060T03:45:19\`.PREHEAT_OVEN({ temperature: 100 }),
                  E(Temporal.Duration.from({ hours: 12, minutes: 6, seconds: 54 })).PACKAGE_BANANA({
                    lot_number: 1093,
                    bundle: [
                      {
                        bundle_name: "Chiquita",
                        number_of_bananas: 43
                      },
                      {
                        bundle_name: "Dole",
                        number_of_bananas: 12
                      }
                    ]
                }),
                R\`02:02:00.000\`.GROUND_BLOCK('GroundBlock2')
                .ARGUMENTS([
                  {
                    name: 'intStartTime',
                    type: 'number',
                    value: 10,
                  }
                ])
                .DESCRIPTION('Set the ground block time')
                .METADATA({
                  author: 'Ryan',
                })
                .MODELS([
                  {
                    offset: '00:10:00.001',
                    value: true,
                    variable: 'model_var_boolean',
                  }
                ])
              ],
            });
          `,
      },
      {
        commandDictionaryId,
        edslBody: `
            export default () =>
            Sequence.new({
              seqId: "test00002",
              metadata: {},
              steps: [
                  C.BAKE_BREAD,
                  A\`2020-061T03:45:19\`.PREHEAT_OVEN({ temperature: 100 }),
                  E(Temporal.Duration.from({ hours: 12, minutes: 6, seconds: 54 })).PACKAGE_BANANA({
                    lot_number: 1093,
                    bundle: [
                      {
                        bundle_name: "Chiquita",
                        number_of_bananas: 43
                      },
                      {
                        bundle_name: "Dole",
                        number_of_bananas: 12
                      }
                    ]
                  }).MODELS([{
                    offset: '00:00:00.000',
                    value: 1.234,
                    variable: 'model_var_float',
                  },{
                    offset: '00:00:00.001',
                    value: '-1234',
                    variable: 'model_var_int',
                  },{
                    offset: '00:10:00.001',
                    value: true,
                    variable: 'model_var_boolean',
                  }]),
              ],
              locals: [
                {
                  allowable_ranges: [
                    {
                      max: 3600,
                      min: 1,
                    },
                  ],
                  name: 'duration',
                  type: 'UINT',
                }
              ],
              parameters: [
                {
                  allowable_ranges: [
                    {
                      max: 3600,
                      min: 1,
                    },
                  ],
                  name: 'duration',
                  type: 'UINT',
                }
              ],
              hardware_commands: [
                HDW_BLENDER_DUMP
                .DESCRIPTION("FIRE THE PYROS")
                .METADATA({author: 'rrgoetz'})
              ],
              immediate_commands: [
                PEEL_BANANA({peelDirection: 'fromStem'})
              ],
              requests: [
                A\`2020-173T20:00:00.000\`.REQUEST(
                  'test_request1',
                  R\`04:39:22.000\`.PREHEAT_OVEN({
                    temperature: 360,
                    }),
                    C.ADD_WATER,
                )
                  .DESCRIPTION('Absolute-timed request object with all possible fields'),
                REQUEST(
                  'test_request1',
                  {
                    delta: '+00:30:00',
                    name: 'test_ground_epoch',
                  },
                  C.ADD_WATER
                )
                  .DESCRIPTION('Ground-epoch timed request object with all possible fields')
                  .METADATA({
                    "author": "rrgoet"
                   })
              ],
            });
          `,
      },
    ]);

    expect(results[0]!.id).toBe('test00001');
    expect(results[0]!.metadata).toEqual({});
    expect(results[0]!.steps).toEqual([
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        description: 'Bake bread',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '2020-060T03:45:19.000',
          type: 'ABSOLUTE',
        },
        args: [{ value: 100, name: 'temperature', type: 'number' }],
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: {
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Dole',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
      },
      {
        args: [
          {
            name: 'intStartTime',
            type: 'number',
            value: 10,
          },
        ],
        description: 'Set the ground block time',
        metadata: {
          author: 'Ryan',
        },
        models: [
          {
            offset: '00:10:00.001',
            value: true,
            variable: 'model_var_boolean',
          },
        ],
        name: 'GroundBlock2',
        time: {
          tag: '02:02:00.000',
          type: 'COMMAND_RELATIVE',
        },
        type: 'ground_block',
      },
    ]);

    expect(results[1]!.id).toBe('test00002');
    expect(results[1]!.metadata).toEqual({});
    expect(results[1]!.steps).toEqual([
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [],
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '2020-061T03:45:19.000',
          type: 'ABSOLUTE',
        },
        args: [{ value: 100, name: 'temperature', type: 'number' }],
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: {
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Chiquita',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 43,
                },
              ],
              [
                {
                  name: 'bundle_name',
                  type: 'string',
                  value: 'Dole',
                },
                {
                  name: 'number_of_bananas',
                  type: 'number',
                  value: 12,
                },
              ],
            ],
          },
        ],
        models: [
          {
            offset: '00:00:00.000',
            value: 1.234,
            variable: 'model_var_float',
          },
          {
            offset: '00:00:00.001',
            value: '-1234',
            variable: 'model_var_int',
          },
          {
            offset: '00:10:00.001',
            value: true,
            variable: 'model_var_boolean',
          },
        ],
      },
    ]);
    expect(results[1]!.locals).toEqual([
      {
        allowable_ranges: [
          {
            max: 3600,
            min: 1,
          },
        ],
        name: 'duration',
        type: 'UINT',
      },
    ]);
    expect(results[1]!.parameters).toEqual([
      {
        allowable_ranges: [
          {
            max: 3600,
            min: 1,
          },
        ],
        name: 'duration',
        type: 'UINT',
      },
    ]);
    expect(results[1]!.hardware_commands).toEqual([
      {
        description: 'FIRE THE PYROS',
        metadata: {
          author: 'rrgoetz',
        },
        stem: 'HDW_BLENDER_DUMP',
      },
    ]);
    expect(results[1]!.immediate_commands).toEqual([
      {
        args: [
          {
            name: 'peelDirection',
            type: 'string',
            value: 'fromStem',
          },
        ],
        stem: 'PEEL_BANANA',
      },
    ]);
    expect(results[1]!.requests).toEqual([
      {
        description: "Absolute-timed request object with all possible fields",
        name: 'test_request1',
        steps: [
          {
            args: [
              {
                name: 'temperature',
                type: 'number',
                value: 360,
              },
            ],
            stem: 'PREHEAT_OVEN',
            time: {
              tag: '04:39:22.000',
              type: 'COMMAND_RELATIVE',
            },
            type: 'command',
          },
          {
            args: [],
            stem: 'ADD_WATER',
            time: {
              type: 'COMMAND_COMPLETE',
            },
            type: 'command',
          },
        ],
        time: {
          "tag": "2020-173T20:00:00.000",
          "type": "ABSOLUTE"
        },
        type: 'request',
      },
      {
        description: 'Ground-epoch timed request object with all possible fields',
        ground_epoch: {
          "delta": "+00:30:00",
          "name": "test_ground_epoch"
        },
        metadata: {
          author: 'rrgoet',
        },
        name: 'test_request1',
        steps: [
          {
            args: [],
            stem: 'ADD_WATER',
            time: { type: 'COMMAND_COMPLETE' },
            type: 'command',
          },
        ],
        type: 'request',
      },
    ]);
  }, 30000);
});
