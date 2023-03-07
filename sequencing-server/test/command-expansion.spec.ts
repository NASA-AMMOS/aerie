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
  insertExpansion,
  insertExpansionSet,
  removeExpansion,
  removeExpansionRun,
  removeExpansionSet,
} from './testUtils/Expansion.js';
import { removeMissionModel, uploadMissionModel } from './testUtils/MissionModel.js';
import { createPlan, removePlan } from './testUtils/Plan.js';
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

describe('expansion', () => {
  let expansionId: number;

  beforeEach(async () => {
    expansionId = await insertExpansion(
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
  });

  afterEach(async () => {
    await removeExpansion(graphqlClient, expansionId);
  });

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
  });

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
  }, 10000);
});
