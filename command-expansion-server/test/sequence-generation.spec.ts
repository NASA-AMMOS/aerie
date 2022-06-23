import { gql, GraphQLClient } from 'graphql-request';
import { removeMissionModel, uploadMissionModel } from './testUtils/MissionModel.js';
import { createPlan, removePlan } from './testUtils/Plan.js';
import { convertActivityIdToSimulatedActivityId, insertActivity, removeActivity } from './testUtils/Activity.js';
import { executeSimulation, removeSimulationArtifacts } from './testUtils/Simulation.js';
import {
  expand,
  insertExpansion,
  insertExpansionSet,
  removeExpansion,
  removeExpansionRun,
  removeExpansionSet,
} from './testUtils/Expansion.js';
import { insertCommandDictionary, removeCommandDictionary } from './testUtils/CommandDictionary.js';
import type { SequenceSeqJson } from '../src/lib/codegen/CommandEDSLPreface.js';
import { insertSequence, linkActivityInstance, removeSequence } from './testUtils/Sequence.js';

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
  let activityId1: number;
  let activityId2: number;
  let activityId3: number;
  let activityId4: number;
  let simulationArtifactPk: { simulationId: number; simulationDatasetId: number };

  let expansionId1: number;
  let expansionId2: number;
  let expansionId3: number;
  let expansionId4: number;
  let expansionSetId: number;
  let sequencePk: { seqId: string; simulationDatasetId: number };

  beforeEach(async () => {
    activityId1 = await insertActivity(graphqlClient, planId, 'GrowBanana');
    activityId2 = await insertActivity(graphqlClient, planId, 'PeelBanana', '30 minutes');
    activityId4 = await insertActivity(graphqlClient, planId, 'ThrowBanana', '60 minutes');
    commandDictionaryId = await insertCommandDictionary(graphqlClient);
    expansionId1 = await insertExpansion(
      graphqlClient,
      'GrowBanana',
      `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        PREHEAT_OVEN({temperature: 70}),
        PREPARE_LOAF(50, false),
        BAKE_BREAD,
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
        PREHEAT_OVEN({temperature: 70}),
        BAKE_BREAD,
        PREPARE_LOAF(50, false),
      ];
    }
    `,
    );
    expansionId4 = await insertExpansion(
      graphqlClient,
      'ThrowBanana',
      `
    export default function TimeDynamicCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        ADD_WATER.absoluteTiming(Temporal.Instant.from("2022-04-20T20:17:13Z")),
        ADD_WATER.absoluteTiming(Temporal.Instant.from("2020-02-29T03:45:19Z")),
        ADD_WATER.absoluteTiming(Temporal.Instant.from("2025-12-24T12:01:59Z")),
        EAT_BANANA.relativeTiming(Temporal.Duration.from({ minutes: 15, seconds: 30 })),
        EAT_BANANA.epochTiming(Temporal.Duration.from({ hours: 12, minutes: 6, seconds: 54 })),
        PACKAGE_BANANA({
          bundle_name1: "Chiquita",
          number_of_bananas1: 43,
          bundle_name2: "Dole",
          number_of_bananas2: 12,
          lot_number: 1093,
        }),
        PACKAGE_BANANA({
          bundle_name2: "Dole",
          number_of_bananas1: 43,
          bundle_name1: "Chiquita",
          lot_number: 1093,
          number_of_bananas2: 12
        }),
        PACKAGE_BANANA("Chiquita",43,"Dole",12,"Blue",1,1093)
      ];
    }
    `,
    );
    expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
      expansionId1,
      expansionId2,
      expansionId4,
    ]);
  });

  afterEach(async () => {
    await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
    await removeActivity(graphqlClient, activityId1);
    await removeMissionModel(graphqlClient, missionModelId);
    await removeExpansionSet(graphqlClient, expansionSetId);
    await removeExpansion(graphqlClient, expansionId1);
    await removeExpansion(graphqlClient, expansionId2);
    await removeExpansion(graphqlClient, expansionId4);
  });

  it('should return sequence seqjson', async () => {
    let expansionRunPk: number;
    // Setup
    {
      simulationArtifactPk = await executeSimulation(graphqlClient, planId);
      expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
      sequencePk = await insertSequence(graphqlClient, {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      });
      await linkActivityInstance(graphqlClient, sequencePk, activityId1);
      await linkActivityInstance(graphqlClient, sequencePk, activityId2);
      await linkActivityInstance(graphqlClient, sequencePk, activityId4);
    }

    const simulatedActivityId1 = await convertActivityIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId1,
    );
    const simulatedActivityId2 = await convertActivityIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId2,
    );
    const simulatedActivityId4 = await convertActivityIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId4,
    );

    const { getSequenceSeqJson } = await graphqlClient.request<{ getSequenceSeqJson: SequenceSeqJson }>(
      gql`
        query GetSeqJsonForSequence($seqId: String!, $simulationDatasetId: Int!) {
          getSequenceSeqJson(seqId: $seqId, simulationDatasetId: $simulationDatasetId) {
            id
            metadata
            steps {
              type
              stem
              time {
                type
                tag
              }
              args
              metadata
            }
          }
        }
      `,
      {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      },
    );

    expect(getSequenceSeqJson.id).toBe('test00000');
    expect(getSequenceSeqJson.metadata).toEqual({});
    expect(getSequenceSeqJson.steps).toEqual([
      {
        // expansion 1
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: 'COMMAND_COMPLETE' },
        args: [70],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: 'COMMAND_COMPLETE' },
        args: [50, false],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: 'COMMAND_COMPLETE' },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        // expansion 2
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: 'COMMAND_COMPLETE' },
        args: [70],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: 'COMMAND_COMPLETE' },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: 'COMMAND_COMPLETE' },
        args: [50, false],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        // expansion 4
        type: 'command',
        stem: 'ADD_WATER',
        time: {
          tag: '2022-110T20:17:13.000',
          type: 'ABSOLUTE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'ADD_WATER',
        time: {
          tag: '2020-060T03:45:19.000',
          type: 'ABSOLUTE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'ADD_WATER',
        time: {
          tag: '2025-358T12:01:59.000',
          type: 'ABSOLUTE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '00:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: 'COMMAND_COMPLETE' },
        args: ['Chiquita', 43, 'Dole', 12, 1093],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: 'COMMAND_COMPLETE' },
        args: ['Chiquita', 43, 'Dole', 12, 1093],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: 'COMMAND_COMPLETE' },
        args: ['Chiquita', 43, 'Dole', 12, 'Blue', 1, 1093],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
    ]);

    // Cleanup
    {
      await removeSequence(graphqlClient, sequencePk);
      await removeExpansionRun(graphqlClient, expansionRunPk);
    }
  }, 30000);

  it('should work for throwing expansions', async () => {
    let expansionRunPk: number;
    // Setup
    {
      expansionId3 = await insertExpansion(
        graphqlClient,
        'BiteBanana',
        `
      export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
        throw new Error('Unimplemented');
      }
      `,
      );
      expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
        expansionId1,
        expansionId2,
        expansionId3,
      ]);
      activityId3 = await insertActivity(graphqlClient, planId, 'BiteBanana', '1 hours');
      simulationArtifactPk = await executeSimulation(graphqlClient, planId);
      expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
      sequencePk = await insertSequence(graphqlClient, {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      });
      await linkActivityInstance(graphqlClient, sequencePk, activityId1);
      await linkActivityInstance(graphqlClient, sequencePk, activityId2);
      await linkActivityInstance(graphqlClient, sequencePk, activityId3);
    }

    const simulatedActivityId1 = await convertActivityIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId1,
    );
    const simulatedActivityId2 = await convertActivityIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId2,
    );
    const simulatedActivityId3 = await convertActivityIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId3,
    );

    const { getSequenceSeqJson } = await graphqlClient.request<{ getSequenceSeqJson: SequenceSeqJson }>(
      gql`
        query GetSeqJsonForSequence($seqId: String!, $simulationDatasetId: Int!) {
          getSequenceSeqJson(seqId: $seqId, simulationDatasetId: $simulationDatasetId) {
            id
            metadata
            steps {
              type
              stem
              time {
                type
                tag
              }
              args
              metadata
            }
          }
        }
      `,
      {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      },
    );

    expect(getSequenceSeqJson.id).toBe('test00000');
    expect(getSequenceSeqJson.metadata).toEqual({});
    expect(getSequenceSeqJson.steps).toEqual([
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: 'COMMAND_COMPLETE' },
        args: [70],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: 'COMMAND_COMPLETE' },
        args: [50, false],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: 'COMMAND_COMPLETE' },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: 'COMMAND_COMPLETE' },
        args: [70],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: 'COMMAND_COMPLETE' },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: 'COMMAND_COMPLETE' },
        args: [50, false],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: '$$ERROR$$',
        time: { type: 'COMMAND_COMPLETE' },
        args: ['Error: Unimplemented'],
        metadata: { simulatedActivityId: simulatedActivityId3 },
      },
    ]);

    // Cleanup
    {
      await removeSequence(graphqlClient, sequencePk);
      await removeExpansion(graphqlClient, expansionId3);
      await removeExpansionRun(graphqlClient, expansionRunPk);
    }
  }, 30000);

  it('should work for non-existent expansions', async () => {
    let activityId3: number;
    let expansionRunPk: number;
    // Setup
    {
      activityId3 = await insertActivity(graphqlClient, planId, 'BiteBanana', '1 hours');
      simulationArtifactPk = await executeSimulation(graphqlClient, planId);
      expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
      sequencePk = await insertSequence(graphqlClient, {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      });
      await linkActivityInstance(graphqlClient, sequencePk, activityId1);
      await linkActivityInstance(graphqlClient, sequencePk, activityId2);
      await linkActivityInstance(graphqlClient, sequencePk, activityId3);
    }

    const simulatedActivityId1 = await convertActivityIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId1,
    );
    const simulatedActivityId2 = await convertActivityIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId2,
    );

    const { getSequenceSeqJson } = await graphqlClient.request<{ getSequenceSeqJson: SequenceSeqJson }>(
      gql`
        query GetSeqJsonForSequence($seqId: String!, $simulationDatasetId: Int!) {
          getSequenceSeqJson(seqId: $seqId, simulationDatasetId: $simulationDatasetId) {
            id
            metadata
            steps {
              type
              stem
              time {
                type
                tag
              }
              args
              metadata
            }
          }
        }
      `,
      {
        seqId: 'test00000',
        simulationDatasetId: simulationArtifactPk.simulationDatasetId,
      },
    );

    expect(getSequenceSeqJson.id).toBe('test00000');
    expect(getSequenceSeqJson.metadata).toEqual({});
    expect(getSequenceSeqJson.steps).toEqual([
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: 'COMMAND_COMPLETE' },
        args: [70],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: 'COMMAND_COMPLETE' },
        args: [50, false],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: 'COMMAND_COMPLETE' },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: 'COMMAND_COMPLETE' },
        args: [70],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'BAKE_BREAD',
        time: { type: 'COMMAND_COMPLETE' },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: 'COMMAND_COMPLETE' },
        args: [50, false],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
    ]);

    // Cleanup
    {
      await removeSequence(graphqlClient, sequencePk);
      await removeExpansionRun(graphqlClient, expansionRunPk);
    }
  }, 30000);
});
