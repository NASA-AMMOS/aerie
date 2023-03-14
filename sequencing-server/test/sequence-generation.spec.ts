import { gql, GraphQLClient } from 'graphql-request';
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
import { executeSimulation, removeSimulationArtifacts } from './testUtils/Simulation.js';

let planId: number;
let graphqlClient: GraphQLClient;
let missionModelId: number;
let commandDictionaryId: number;

beforeEach(async () => {
  graphqlClient = new GraphQLClient(process.env['MERLIN_GRAPHQL_URL'] as string);
  missionModelId = await uploadMissionModel(graphqlClient);
  planId = await createPlan(graphqlClient, missionModelId);
  commandDictionaryId = await insertCommandDictionary(graphqlClient);
});

afterEach(async () => {
  await removePlan(graphqlClient, planId);
  await removeMissionModel(graphqlClient, missionModelId);
  await removeCommandDictionary(graphqlClient, commandDictionaryId);
});

describe('sequence generation', () => {
  let expansionId1: number;
  let expansionId2: number;
  let expansionId3: number;
  let expansionId4: number;

  beforeEach(async () => {
    expansionId1 = await insertExpansion(
      graphqlClient,
      'GrowBanana',
      `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        C.PREHEAT_OVEN({ temperature: 70 }),
        C.PREPARE_LOAF({ tb_sugar: 50, gluten_free: false }),
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
        C.PREHEAT_OVEN({ temperature: 70 }),
        C.BAKE_BREAD,
        C.PREPARE_LOAF({ tb_sugar: 50, gluten_free: false }),
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
        E(Temporal.Duration.from({ hours: 12, minutes: 6, seconds: 54 })).PREPARE_LOAF({ tb_sugar: 50, gluten_free: false }),
        E\`04:56:54\`.EAT_BANANA,
        C.PACKAGE_BANANA({
          lot_number:  1093,
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
        })
      ];
    }
    `,
    );
    expansionId4 = await insertExpansion(
      graphqlClient,
      'GrowBanana',
      `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        C.GrowBanana({ quantity: 1, durationSecs: 10 })
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

  it('should allow an activity type and command to have the same name', async () => {
    const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [expansionId4]);

    await insertActivityDirective(graphqlClient, planId, 'GrowBanana');

    // Simulate Plan
    const simulationArtifactPk = await executeSimulation(graphqlClient, planId);

    // Expand Plan
    const expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);

    expect(expansionSetId).toBeGreaterThan(0);
    expect(expansionRunPk).toBeGreaterThan(0);

    await removeExpansionRun(graphqlClient, expansionRunPk);
    await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
    await removeExpansionSet(graphqlClient, expansionSetId);
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          { value: false, name: 'gluten_free', type: 'boolean' },
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
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { value: 50, name: 'tb_sugar', type: 'number' },
          { value: false, name: 'gluten_free', type: 'boolean' },
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
});

describe('expansion', () => {
  it('should throw an error if an activity instance goes beyond the plan duration', async () => {
    /** Begin Setup*/
    const activityId = await insertActivityDirective(graphqlClient, planId, 'GrowBanana', '1 days');
    const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
    const expansionId = await insertExpansion(
      graphqlClient,
      'GrowBanana',
      `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        R(props.activityInstance.startOffset).PREHEAT_OVEN({temperature: 70}),
        R(props.activityInstance.duration).PREHEAT_OVEN({temperature: 70}),
      ];
    }
    `,
    );
    const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [expansionId]);
    const expansionRunId = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);

    const simulatedActivityId = await convertActivityDirectiveIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId,
    );
    /** End Setup*/

    const { activity_instance_commands } = await graphqlClient.request<{
      activity_instance_commands: { commands: ReturnType<CommandStem['toSeqJson']>; errors: string[] }[];
    }>(
      gql`
        query getExpandedCommands($expansionRunId: Int!, $simulatedActivityId: Int!) {
          activity_instance_commands(
            where: {
              _and: { expansion_run_id: { _eq: $expansionRunId }, activity_instance_id: { _eq: $simulatedActivityId } }
            }
          ) {
            commands
            errors
          }
        }
      `,
      {
        expansionRunId,
        simulatedActivityId,
      },
    );

    expect(activity_instance_commands.length).toBe(1);
    expect(activity_instance_commands[0]?.errors).toEqual([
      {
        message: 'Duration is null',
      },
    ]);

    // Cleanup
    await removeActivityDirective(graphqlClient, activityId, planId);
    await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
    await removeExpansion(graphqlClient, expansionId);
    await removeExpansionSet(graphqlClient, expansionSetId);
    await removeExpansionRun(graphqlClient, expansionRunId);
  });

  test('start_offset undefined regression', async () => {
    /** Begin Setup*/
    const activityId = await insertActivityDirective(graphqlClient, planId, 'GrowBanana', '1 hours');
    const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
    const expansionId = await insertExpansion(
      graphqlClient,
      'GrowBanana',
      `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        R(props.activityInstance.startOffset).PREHEAT_OVEN({temperature: 70}),
        R(props.activityInstance.duration).PREHEAT_OVEN({temperature: 70}),
      ];
    }
    `,
    );
    const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [expansionId]);
    const expansionRunId = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);

    const simulatedActivityId = await convertActivityDirectiveIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId,
    );
    /** End Setup*/

    const { activity_instance_commands } = await graphqlClient.request<{
      activity_instance_commands: { commands: ReturnType<CommandStem['toSeqJson']>; errors: string[] }[];
    }>(
      gql`
        query getExpandedCommands($expansionRunId: Int!, $simulatedActivityId: Int!) {
          activity_instance_commands(
            where: {
              _and: { expansion_run_id: { _eq: $expansionRunId }, activity_instance_id: { _eq: $simulatedActivityId } }
            }
          ) {
            commands
            errors
          }
        }
      `,
      {
        expansionRunId,
        simulatedActivityId,
      },
    );

    expect(activity_instance_commands.length).toBe(1);
    if (activity_instance_commands[0]?.errors.length !== 0) {
      throw new Error(activity_instance_commands[0]?.errors.join('\n'));
    }
    expect(activity_instance_commands[0]?.commands).toEqual([
      {
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId },
        stem: 'PREHEAT_OVEN',
        time: { tag: '01:00:00.000', type: TimingTypes.COMMAND_RELATIVE },
        type: 'command',
      },
      {
        args: [{ value: 70, name: 'temperature', type: 'number' }],
        metadata: { simulatedActivityId },
        stem: 'PREHEAT_OVEN',
        time: { tag: '01:00:00.000', type: TimingTypes.COMMAND_RELATIVE },
        type: 'command',
      },
    ]);

    // Cleanup
    await removeActivityDirective(graphqlClient, activityId, planId);
    await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
    await removeExpansion(graphqlClient, expansionId);
    await removeExpansionSet(graphqlClient, expansionSetId);
    await removeExpansionRun(graphqlClient, expansionRunId);
  }, 10000);
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
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [
          { name: 'lot_number', type: 'number', value: 1093 },
          {
            name: 'bundle',
            type: 'repeat',
            value: [
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
  }, 30000);
});
