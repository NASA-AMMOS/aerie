import { GraphQLClient } from 'graphql-request';
import { simulatedActivityInstanceBatchLoader } from '../../src/lib/batchLoaders/simulatedActivityInstanceBatchLoader.js';
import { removeMissionModel, uploadMissionModel } from '../utils/MissionModel.js';
import { createPlan, removePlan } from '../utils/Plan';
import { insertActivity, removeActivity } from '../utils/Activity';
import { executeSimulation, removeSimulationArtifacts } from '../utils/Simulation';

let graphqlClient: GraphQLClient;
let missionModelId: number;
let planId: number;
let activityId: number;
let simulationArtifactIds: { simulationId: number, simulationDatasetId: number };

beforeAll(async () => {
  graphqlClient = new GraphQLClient(process.env.MERLIN_GRAPHQL_URL as string);
  missionModelId = await uploadMissionModel(graphqlClient);
  planId = await createPlan(graphqlClient, missionModelId);
  activityId = await insertActivity(graphqlClient, planId);
  simulationArtifactIds = await executeSimulation(graphqlClient, planId);
});

afterAll(async () => {
  await removeSimulationArtifacts(graphqlClient, simulationArtifactIds);
  await removeActivity(graphqlClient, activityId);
  await removePlan(graphqlClient, planId);
  await removeMissionModel(graphqlClient, missionModelId);
});

it('should load simulated activity instance', async () => {
  const activityInstances = await simulatedActivityInstanceBatchLoader({graphqlClient})([
    { simulationDatasetId: simulationArtifactIds.simulationDatasetId },
  ]);
  if (activityInstances[0] instanceof Error) {
    throw activityInstances[0];
  }
  expect(activityInstances[0][0].type).toBe('ParameterTest');
});
