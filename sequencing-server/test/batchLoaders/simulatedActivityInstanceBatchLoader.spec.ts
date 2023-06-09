import type { GraphQLClient } from 'graphql-request';
import {
  simulatedActivitiesBatchLoader,
  simulatedActivityInstanceBySimulatedActivityIdBatchLoader,
} from '../../src/lib/batchLoaders/simulatedActivityBatchLoader.js';
import { removeMissionModel, uploadMissionModel } from '../testUtils/MissionModel.js';
import { createPlan, removePlan } from '../testUtils/Plan';
import {
  convertActivityDirectiveIdToSimulatedActivityId,
  insertActivityDirective,
  removeActivityDirective,
} from '../testUtils/ActivityDirective';
import { executeSimulation, removeSimulationArtifacts, updateSimulationBounds } from '../testUtils/Simulation';
import DataLoader from 'dataloader';
import { activitySchemaBatchLoader } from '../../src/lib/batchLoaders/activitySchemaBatchLoader.js';
import { getGraphQLClient } from '../testUtils/testUtils.js';

let graphqlClient: GraphQLClient;
let missionModelId: number;
let planId: number;
let activityId: number;
let simulationArtifactIds: { simulationId: number; simulationDatasetId: number };

beforeAll(async () => {
  graphqlClient = await getGraphQLClient();
  missionModelId = await uploadMissionModel(graphqlClient);
  planId = await createPlan(graphqlClient, missionModelId);
  activityId = await insertActivityDirective(graphqlClient, planId, 'ParameterTest');
  await updateSimulationBounds(graphqlClient, {
    plan_id: planId,
    simulation_start_time: '2020-001T00:00:00Z',
    simulation_end_time: '2020-002T00:00:00Z',
  });
  simulationArtifactIds = await executeSimulation(graphqlClient, planId);
});

afterAll(async () => {
  await removeSimulationArtifacts(graphqlClient, { simulationDatasetId: simulationArtifactIds.simulationDatasetId });
  await removeActivityDirective(graphqlClient, activityId, planId);
  await removePlan(graphqlClient, planId);
  await removeMissionModel(graphqlClient, missionModelId);
});

it('should load simulated activity instances for simulation_dataset', async () => {
  const activitySchemaDataLoader = new DataLoader(activitySchemaBatchLoader({ graphqlClient }));
  const simulatedActivities = await simulatedActivitiesBatchLoader({ graphqlClient, activitySchemaDataLoader })([
    { simulationDatasetId: simulationArtifactIds.simulationDatasetId },
  ]);
  if (simulatedActivities[0] instanceof Error) {
    throw simulatedActivities[0];
  }
  expect(simulatedActivities[0]?.[0]?.activityTypeName).toBe('ParameterTest');
});

it('should load simulated activity instance for simulation_dataset and simulated activity id', async () => {
  const activitySchemaDataLoader = new DataLoader(activitySchemaBatchLoader({ graphqlClient }));

  const simulatedActivityId = await convertActivityDirectiveIdToSimulatedActivityId(
    graphqlClient,
    simulationArtifactIds.simulationDatasetId,
    activityId,
  );

  const activityInstances = await simulatedActivityInstanceBySimulatedActivityIdBatchLoader({
    graphqlClient,
    activitySchemaDataLoader,
  })([{ simulationDatasetId: simulationArtifactIds.simulationDatasetId, simulatedActivityId }]);
  if (activityInstances[0] instanceof Error) {
    throw activityInstances[0];
  }
  expect(activityInstances[0]?.activityTypeName).toBe('ParameterTest');
});
