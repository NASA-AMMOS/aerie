import { gql, GraphQLClient } from 'graphql-request';
import { removeMissionModel, uploadMissionModel } from './testUtils/MissionModel.js';
import { createPlan, removePlan } from './testUtils/Plan.js';
import { convertActivityDirectiveIdToSimulatedActivityId, insertActivityDirective, removeActivityDirective } from './testUtils/ActivityDirective.js';
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
import { insertSequence, generateSequenceEDSL, linkActivityInstance, removeSequence } from './testUtils/Sequence.js';
import { TimingTypes } from '../src/lib/codegen/CommandEDSLPreface.js';

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
    activityId1 = await insertActivityDirective(graphqlClient, planId, 'GrowBanana');
    activityId2 = await insertActivityDirective(graphqlClient, planId, 'PeelBanana', '30 minutes');
    activityId4 = await insertActivityDirective(graphqlClient, planId, 'ThrowBanana', '60 minutes');
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
        A\`2020-060T03:45:19\`.ADD_WATER,
        A(Temporal.Instant.from("2025-12-24T12:01:59Z")).PREHEAT_OVEN({temperature: 360}),
        R\`00:15:30\`.PREHEAT_OVEN({temperature: 425}),
        R(Temporal.Duration.from({ hours: 1, minutes: 15, seconds: 30 })).EAT_BANANA,
        E(Temporal.Duration.from({ hours: 12, minutes: 6, seconds: 54 })).PREPARE_LOAF(50, false),
        E\`04:56:54\`.EAT_BANANA,
        C.PACKAGE_BANANA({
          bundle_name1: "Chiquita",
          number_of_bananas1: 43,
          bundle_name2: "Dole",
          number_of_bananas2: 12,
          lot_number: 1093,
        }),
        C.PACKAGE_BANANA({
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
    await removeActivityDirective(graphqlClient, activityId1);
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

    const simulatedActivityId1 = await convertActivityDirectiveIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId1,
    );
    const simulatedActivityId2 = await convertActivityDirectiveIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId2,
    );
    const simulatedActivityId4 = await convertActivityDirectiveIdToSimulatedActivityId(
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
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [70],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [50, false],
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
        args: [70],
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
        args: [50, false],
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
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '2025-358T12:01:59.000',
          type: 'ABSOLUTE',
        },
        args: [360],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: {
          tag: '00:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [425],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '01:15:30.000',
          type: 'COMMAND_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: {
          tag: '12:06:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [50, false],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'EAT_BANANA',
        time: {
          tag: '04:56:54.000',
          type: 'EPOCH_RELATIVE',
        },
        args: [],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: ['Chiquita', 43, 'Dole', 12, 1093],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: ['Chiquita', 43, 'Dole', 12, 1093],
        metadata: { simulatedActivityId: simulatedActivityId4 },
      },
      {
        type: 'command',
        stem: 'PACKAGE_BANANA',
        time: { type: TimingTypes.COMMAND_COMPLETE },
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
      activityId3 = await insertActivityDirective(graphqlClient, planId, 'BiteBanana', '1 hours');
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

    const simulatedActivityId1 = await convertActivityDirectiveIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId1,
    );
    const simulatedActivityId2 = await convertActivityDirectiveIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId2,
    );
    const simulatedActivityId3 = await convertActivityDirectiveIdToSimulatedActivityId(
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
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [70],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [50, false],
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
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [70],
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
        args: [50, false],
        metadata: { simulatedActivityId: simulatedActivityId2 },
      },
      {
        type: 'command',
        stem: '$$ERROR$$',
        time: { type: TimingTypes.COMMAND_COMPLETE },
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
      activityId3 = await insertActivityDirective(graphqlClient, planId, 'BiteBanana', '1 hours');
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

    const simulatedActivityId1 = await convertActivityDirectiveIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId1,
    );
    const simulatedActivityId2 = await convertActivityDirectiveIdToSimulatedActivityId(
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
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [70],
        metadata: { simulatedActivityId: simulatedActivityId1 },
      },
      {
        type: 'command',
        stem: 'PREPARE_LOAF',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [50, false],
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
        type: 'command',
        stem: 'PREHEAT_OVEN',
        time: { type: TimingTypes.COMMAND_COMPLETE },
        args: [70],
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

describe('expansion regressions', () => {
  test('start_offset undefined regression', async () => {
    // Setup
    const activityId = await insertActivityDirective(graphqlClient, planId, 'GrowBanana', '1 hours');
    const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
    const expansionId = await insertExpansion(
      graphqlClient,
      'GrowBanana',
      `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        PREHEAT_OVEN({temperature: 70}).relativeTiming(props.activityInstance.startOffset),
        PREHEAT_OVEN({temperature: 70}).relativeTiming(props.activityInstance.duration),
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

    const { activity_instance_commands } = await graphqlClient.request<{
      activity_instance_commands: { commands: ReturnType<Command['toSeqJson']>; errors: string[] }[];
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
        args: [70],
        metadata: { simulatedActivityId },
        stem: 'PREHEAT_OVEN',
        time: { tag: '01:00:00.000', type: TimingTypes.COMMAND_RELATIVE },
        type: 'command',
      },
      {
        args: [70],
        metadata: { simulatedActivityId },
        stem: 'PREHEAT_OVEN',
        time: { tag: '01:00:00.000', type: TimingTypes.COMMAND_RELATIVE },
        type: 'command',
      },
    ]);

    // Cleanup
    await removeActivityDirective(graphqlClient, activityId);
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
        C.ECHO("Computed attributes: " + props.activityInstance.attributes.computed),
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
      args: ['Computed attributes: 198'],
      metadata: { simulatedActivityId: simulatedActivityId3 },
    },
  ]);

  // Cleanup
  {
    await removeSequence(graphqlClient, sequencePk);
  }
}, 30000);

it('generate sequence seqjson from static sequence', async () => {
  var results = await generateSequenceEDSL(
    graphqlClient,
    commandDictionaryId,
    `
    export default () =>
    Sequence.new({
      seqId: "test00001",
      metadata: {},
      commands: [
          C.BAKE_BREAD,
          A\`2020-060T03:45:19\`.PREHEAT_OVEN(100),
          E(Temporal.Duration.from({ hours: 12, minutes: 6, seconds: 54 })).PACKAGE_BANANA({
            bundle_name2: "Dole",
            number_of_bananas1: 43,
            bundle_name1: "Chiquita",
            lot_number: 1093,
            number_of_bananas2: 12
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
      metadata: {},
    },
    {
      type: 'command',
      stem: 'PREHEAT_OVEN',
      time: {
        tag: '2020-060T03:45:19.000',
        type: 'ABSOLUTE',
      },
      args: [100],
      metadata: {},
    },
    {
      type: 'command',
      stem: 'PACKAGE_BANANA',
      time: {
        tag: '12:06:54.000',
        type: 'EPOCH_RELATIVE',
      },
      args: ['Chiquita', 43, 'Dole', 12, 1093],
      metadata: {},
    },
  ]);
}, 30000);
