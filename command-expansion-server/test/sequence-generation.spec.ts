import { gql, GraphQLClient } from 'graphql-request';
import { removeMissionModel, uploadMissionModel } from './utils/MissionModel.js';
import { createPlan, removePlan } from './utils/Plan.js';
import { convertActivityIdToSimulatedActivityId, insertActivity, removeActivity } from './utils/Activity.js';
import { executeSimulation, removeSimulationArtifacts } from './utils/Simulation.js';
import { expand, insertExpansion, insertExpansionSet, removeExpansion, removeExpansionSet } from './utils/Expansion.js';
import { insertCommandDictionary, removeCommandDictionary } from './utils/CommandDictionary.js';
import type { SequenceSeqJson } from '../src/lib/codegen/CommandEDSLPreface.js';
import { insertSequence, linkActivityInstance, removeSequence } from './utils/Sequence.js';

let graphqlClient: GraphQLClient;
let missionModelId: number;
let planId: number;
let activityId1: number;
let activityId2: number;
let simulationArtifactPk: { simulationId: number; simulationDatasetId: number };
let commandDictionaryId: number;
let expansionId1: number;
let expansionId2: number;
let expansionSetId: number;
let sequencePk: { seqId: string; simulationDatasetId: number };

beforeEach(async () => {
  graphqlClient = new GraphQLClient(process.env.MERLIN_GRAPHQL_URL as string);
  missionModelId = await uploadMissionModel(graphqlClient);
  planId = await createPlan(graphqlClient, missionModelId);
  activityId1 = await insertActivity(graphqlClient, planId, 'GrowBanana');
  activityId2 = await insertActivity(graphqlClient, planId, 'PeelBanana', '30 minutes');
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
  expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [
    expansionId1,
    expansionId2,
  ]);
});

afterEach(async () => {
  await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
  await removeActivity(graphqlClient, activityId1);
  await removePlan(graphqlClient, planId);
  await removeMissionModel(graphqlClient, missionModelId);
  await removeExpansionSet(graphqlClient, expansionSetId);
  await removeExpansion(graphqlClient, expansionId1);
  await removeExpansion(graphqlClient, expansionId2);
  await removeCommandDictionary(graphqlClient, commandDictionaryId);
});

it('should return sequence seqjson', async () => {
  // Setup
  {
    simulationArtifactPk = await executeSimulation(graphqlClient, planId);
    await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
    sequencePk = await insertSequence(graphqlClient, {
      seqId: 'test00000',
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
    });
    await linkActivityInstance(graphqlClient, sequencePk, activityId1);
    await linkActivityInstance(graphqlClient, sequencePk, activityId2);
  }

  const simulatedActivityId1 = await convertActivityIdToSimulatedActivityId(graphqlClient, simulationArtifactPk.simulationDatasetId, activityId1);
  const simulatedActivityId2 = await convertActivityIdToSimulatedActivityId(graphqlClient, simulationArtifactPk.simulationDatasetId, activityId2);

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
    { type: 'command', stem: 'PREHEAT_OVEN', time: { type: 'COMMAND_COMPLETE' }, args: [70], metadata: { simulatedActivityId: simulatedActivityId1 } },
    { type: 'command', stem: 'PREPARE_LOAF', time: { type: 'COMMAND_COMPLETE' }, args: [50, false], metadata: { simulatedActivityId: simulatedActivityId1 } },
    { type: 'command', stem: 'BAKE_BREAD', time: { type: 'COMMAND_COMPLETE' }, args: [], metadata: { simulatedActivityId: simulatedActivityId1 } },
    { type: 'command', stem: 'PREHEAT_OVEN', time: { type: 'COMMAND_COMPLETE' }, args: [70], metadata: { simulatedActivityId: simulatedActivityId2 } },
    { type: 'command', stem: 'BAKE_BREAD', time: { type: 'COMMAND_COMPLETE' }, args: [], metadata: { simulatedActivityId: simulatedActivityId2 } },
    { type: 'command', stem: 'PREPARE_LOAF', time: { type: 'COMMAND_COMPLETE' }, args: [50, false], metadata: { simulatedActivityId: simulatedActivityId2 } },
  ]);

  // Cleanup
  {
    await removeSequence(graphqlClient, sequencePk);
  }
}, 10000);

it('should work for throwing expansions', async () => {
  let activityId3: number;
  let expansionId3: number;
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
    await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
    sequencePk = await insertSequence(graphqlClient, {
      seqId: 'test00000',
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
    });
    await linkActivityInstance(graphqlClient, sequencePk, activityId1);
    await linkActivityInstance(graphqlClient, sequencePk, activityId2);
    await linkActivityInstance(graphqlClient, sequencePk, activityId3);
  }

  const simulatedActivityId1 = await convertActivityIdToSimulatedActivityId(graphqlClient, simulationArtifactPk.simulationDatasetId, activityId1);
  const simulatedActivityId2 = await convertActivityIdToSimulatedActivityId(graphqlClient, simulationArtifactPk.simulationDatasetId, activityId2);
  const simulatedActivityId3 = await convertActivityIdToSimulatedActivityId(graphqlClient, simulationArtifactPk.simulationDatasetId, activityId3);

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
    { type: 'command', stem: 'PREHEAT_OVEN', time: { type: 'COMMAND_COMPLETE' }, args: [70], metadata: { simulatedActivityId: simulatedActivityId1 } },
    { type: 'command', stem: 'PREPARE_LOAF', time: { type: 'COMMAND_COMPLETE' }, args: [50, false], metadata: { simulatedActivityId: simulatedActivityId1 } },
    { type: 'command', stem: 'BAKE_BREAD', time: { type: 'COMMAND_COMPLETE' }, args: [], metadata: { simulatedActivityId: simulatedActivityId1 } },
    { type: 'command', stem: 'PREHEAT_OVEN', time: { type: 'COMMAND_COMPLETE' }, args: [70], metadata: { simulatedActivityId: simulatedActivityId2 } },
    { type: 'command', stem: 'BAKE_BREAD', time: { type: 'COMMAND_COMPLETE' }, args: [], metadata: { simulatedActivityId: simulatedActivityId2 } },
    { type: 'command', stem: 'PREPARE_LOAF', time: { type: 'COMMAND_COMPLETE' }, args: [50, false], metadata: { simulatedActivityId: simulatedActivityId2 } },
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
  }
}, 10000);

it('should work for non-existent expansions', async () => {
  let activityId3: number;
  // Setup
  {
    activityId3 = await insertActivity(graphqlClient, planId, 'BiteBanana', '1 hours');
    simulationArtifactPk = await executeSimulation(graphqlClient, planId);
    await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
    sequencePk = await insertSequence(graphqlClient, {
      seqId: 'test00000',
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
    });
    await linkActivityInstance(graphqlClient, sequencePk, activityId1);
    await linkActivityInstance(graphqlClient, sequencePk, activityId2);
    await linkActivityInstance(graphqlClient, sequencePk, activityId3);
  }

  const simulatedActivityId1 = await convertActivityIdToSimulatedActivityId(graphqlClient, simulationArtifactPk.simulationDatasetId, activityId1);
  const simulatedActivityId2 = await convertActivityIdToSimulatedActivityId(graphqlClient, simulationArtifactPk.simulationDatasetId, activityId2);

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
    { type: 'command', stem: 'PREHEAT_OVEN', time: { type: 'COMMAND_COMPLETE' }, args: [70], metadata: { simulatedActivityId: simulatedActivityId1 } },
    { type: 'command', stem: 'PREPARE_LOAF', time: { type: 'COMMAND_COMPLETE' }, args: [50, false], metadata: { simulatedActivityId: simulatedActivityId1 } },
    { type: 'command', stem: 'BAKE_BREAD', time: { type: 'COMMAND_COMPLETE' }, args: [], metadata: { simulatedActivityId: simulatedActivityId1 } },
    { type: 'command', stem: 'PREHEAT_OVEN', time: { type: 'COMMAND_COMPLETE' }, args: [70], metadata: { simulatedActivityId: simulatedActivityId2 } },
    { type: 'command', stem: 'BAKE_BREAD', time: { type: 'COMMAND_COMPLETE' }, args: [], metadata: { simulatedActivityId: simulatedActivityId2 } },
    { type: 'command', stem: 'PREPARE_LOAF', time: { type: 'COMMAND_COMPLETE' }, args: [50, false], metadata: { simulatedActivityId: simulatedActivityId2 } },
  ]);

  // Cleanup
  {
    await removeSequence(graphqlClient, sequencePk);
  }
}, 10000);
