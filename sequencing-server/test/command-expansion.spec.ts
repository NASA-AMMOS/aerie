import { gql, GraphQLClient } from 'graphql-request';
import { TimingTypes } from '../src/lib/codegen/CommandEDSLPreface.js';
import {
  convertActivityDirectiveIdToSimulatedActivityId,
  insertActivityDirective,
  removeActivityDirective,
} from './testUtils/ActivityDirective.js';
import { insertCommandDictionary, removeCommandDictionary } from './testUtils/CommandDictionary.js';
import {
  expand,
  getExpandedSequence,
  getExpansionSet,
  insertExpansion,
  insertExpansionSet,
  removeExpansion,
  removeExpansionRun,
  removeExpansionSet,
} from './testUtils/Expansion.js';
import { removeMissionModel, uploadMissionModel } from './testUtils/MissionModel.js';
import { createPlan, removePlan } from './testUtils/Plan.js';
import { executeSimulation, removeSimulationArtifacts, updateSimulationBounds } from './testUtils/Simulation.js';
import { getGraphQLClient, waitMs } from './testUtils/testUtils';
import { insertSequence, linkActivityInstance } from './testUtils/Sequence.js';

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
  removeCommandDictionary(graphqlClient, commandDictionaryId);
});

afterEach(async () => {
  await removePlan(graphqlClient, planId);
  await removeMissionModel(graphqlClient, missionModelId);
});

describe('expansion', () => {
  let expansionId: number;
  let groundEventExpansion: number;
  let groundBlockExpansion: number;

  beforeEach(async () => {
    expansionId = await insertExpansion(
      graphqlClient,
      'GrowBanana',
      `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        C.PREHEAT_OVEN({ temperature: 70 }),
        C.PREPARE_LOAF({ tb_sugar: 50, gluten_free: "FALSE" }),
        C.BAKE_BREAD,
      ];
    }
    `,
    );

    groundEventExpansion = await insertExpansion(
      graphqlClient,
      'GrowBanana',
      `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        C.GROUND_EVENT("test")
      ];
    }
    `,
    );

    groundBlockExpansion = await insertExpansion(
      graphqlClient,
      'GrowBanana',
      `
    export default function SingleCommandExpansion(props: { activityInstance: ActivityType }): ExpansionReturn {
      return [
        C.GROUND_BLOCK("test")
      ];
    }
    `,
    );
  });

  afterEach(async () => {
    await removeExpansion(graphqlClient, expansionId);
    await removeExpansion(graphqlClient, groundEventExpansion);
    await removeExpansion(graphqlClient, groundBlockExpansion);
  });

  it('should fail when the user creates an expansion set with a ground block', async () => {
    try {
      expect(
        await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [groundBlockExpansion]),
      ).toThrow();
    } catch (e) {}
  });

  it('should fail when the user creates an expansion set with a ground event', async () => {
    try {
      expect(
        await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [groundEventExpansion]),
      ).toThrow();
    } catch (e) {}
  }, 30000);

  it('should allow an activity type and command to have the same name', async () => {
    const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [expansionId]);

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
  }, 30000);

  it('should throw an error if an activity instance goes beyond the plan duration', async () => {
    /** Begin Setup*/
    const activityId = await insertActivityDirective(graphqlClient, planId, 'GrowBanana', '1 days');
    const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
    // Wait 2s to allow the dataset to finish uploading
    // This is a bandaid method and should be fixed with a proper subscribe (or by also excluding the "uploading" state/changing it to wait for "success" state once 730 is complete)
    await waitMs(2000);
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
  }, 30000);

  it('start_offset undefined regression', async () => {
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
  }, 30000);

  it('should save the expanded sequence on successful expansion', async () => {
    const expansionId = await insertExpansion(
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
    const expansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [expansionId]);

    // Create Activity Directives
    const [activityId] = await Promise.all([insertActivityDirective(graphqlClient, planId, 'GrowBanana')]);

    // Simulate Plan
    const simulationArtifactPk = await executeSimulation(graphqlClient, planId);
    // Create Sequence
    const sequencePk = await insertSequence(graphqlClient, {
      seqId: 'test00000',
      simulationDatasetId: simulationArtifactPk.simulationDatasetId,
    });
    // Link Activity Instances to Sequence
    await Promise.all([linkActivityInstance(graphqlClient, sequencePk, activityId)]);

    // Get the simulated activity ids
    const simulatedActivityId = await convertActivityDirectiveIdToSimulatedActivityId(
      graphqlClient,
      simulationArtifactPk.simulationDatasetId,
      activityId,
    );

    // Expand Plan to Sequence Fragments
    const expansionRunPk = await expand(graphqlClient, expansionSetId, simulationArtifactPk.simulationDatasetId);
    /** End Setup */

    const { expandedSequence, edslString } = await getExpandedSequence(graphqlClient, expansionRunPk, sequencePk.seqId);

    expect(expandedSequence).toEqual({
      id: 'test00000',
      metadata: { planId: planId, simulationDatasetId: simulationArtifactPk.simulationDatasetId, timeSorted: false },
      steps: [
        {
          args: [],
          metadata: { simulatedActivityId: simulatedActivityId },
          stem: 'ADD_WATER',
          time: { tag: '2023-091T10:00:00.000', type: 'ABSOLUTE' },
          type: 'command',
        },
        {
          args: [
            { name: 'quantity', type: 'number', value: 10 },
            { name: 'durationSecs', type: 'number', value: 7200 },
          ],
          metadata: { simulatedActivityId: simulatedActivityId },
          stem: 'GROW_BANANA',
          time: { tag: '04:00:00.000', type: 'COMMAND_RELATIVE' },
          type: 'command',
        },
      ],
    });

    expect(edslString).toEqual(`export default () =>
  Sequence.new({
    seqId: 'test00000',
    metadata: {
      planId: ${planId},
      simulationDatasetId: ${simulationArtifactPk.simulationDatasetId},
      timeSorted: false,
    },
    steps: ({ locals, parameters }) => ([
      A\`2023-091T10:00:00.000\`.ADD_WATER
        .METADATA({
          simulatedActivityId: ${simulatedActivityId},
        }),
      R\`04:00:00.000\`.GROW_BANANA(10,7200)
        .METADATA({
          simulatedActivityId: ${simulatedActivityId},
        }),
    ]),
  });`);

    // Cleanup
    await removeActivityDirective(graphqlClient, activityId, planId);
    await removeSimulationArtifacts(graphqlClient, simulationArtifactPk);
    await removeExpansion(graphqlClient, expansionId);
    await removeExpansionSet(graphqlClient, expansionSetId);
    await removeExpansionRun(graphqlClient, expansionRunPk);
  }, 30000);

  it('should handle optional name and descripton for expansion sets', async () => {
    const expansionId = await insertExpansion(
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
    const name = "test name";
    const description = "test desc";

    const testProvidedExpansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [expansionId], description, name);
    expect(testProvidedExpansionSetId).not.toBeNull();
    expect(testProvidedExpansionSetId).toBeDefined();
    expect(testProvidedExpansionSetId).toBeNumber();

    const testProvidedResp = await getExpansionSet(graphqlClient, testProvidedExpansionSetId);
    expect(testProvidedResp.expansion_set_by_pk.name).toBe(name);
    expect(testProvidedResp.expansion_set_by_pk.description).toBe(description);

    const testDefaultExpansionSetId = await insertExpansionSet(graphqlClient, commandDictionaryId, missionModelId, [expansionId]);
    expect(testDefaultExpansionSetId).not.toBeNull();
    expect(testDefaultExpansionSetId).toBeDefined();
    expect(testDefaultExpansionSetId).toBeNumber();

    const testDefaultResp = await getExpansionSet(graphqlClient, testDefaultExpansionSetId);
    expect(testDefaultResp.expansion_set_by_pk.name).toBe("");
    expect(testDefaultResp.expansion_set_by_pk.description).toBe("");

    // Cleanup
    await removeExpansion(graphqlClient, expansionId);
    await removeExpansionSet(graphqlClient, testProvidedExpansionSetId);
    await removeExpansionSet(graphqlClient, testProvidedExpansionSetId);
  });
});
