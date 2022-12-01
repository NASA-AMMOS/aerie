import { jest } from '@jest/globals';
import { gql, GraphQLClient } from 'graphql-request';
import perf from 'perf_hooks';

jest.setTimeout(10000);

export async function executeSimulation(
  graphqlClient: GraphQLClient,
  planId: number,
): Promise<{ simulationId: number; simulationDatasetId: number }> {
  const startTime = perf.performance.now();
  while (perf.performance.now() - startTime < 10000) {
    const simulationRes = await graphqlClient.request(
      gql`
        query SimulatePlan($planId: Int!) {
          simulate(planId: $planId) {
            status
            reason
          }
        }
      `,
      {
        planId,
      },
    );
    const status = simulationRes.simulate.status;
    if (status === 'failed') {
      throw new Error(simulationRes.simulate.reason.message);
    }
    if (status !== 'pending' && status !== 'incomplete') {
      const getSimulationRes = await graphqlClient.request(
        gql`
          query SimulationDatasetId($planId: Int!) {
            simulation_dataset(where: { simulation: { plan_id: { _eq: $planId } } }) {
              id
            }
            simulation(where: { plan_id: { _eq: $planId } }) {
              id
            }
          }
        `,
        {
          planId,
        },
      );
      return {
        simulationDatasetId: getSimulationRes.simulation_dataset[0].id,
        simulationId: getSimulationRes.simulation[0].id,
      };
    }
  }
  throw new Error('Simulation timed out');
}

export async function removeSimulationArtifacts(
  graphqlClient: GraphQLClient,
  simulationPk: { simulationId: number; simulationDatasetId: number },
): Promise<void> {
  /*
   * Remove a plan
   */

  await graphqlClient.request(
    gql`
      mutation DeleteSimulation($simulationId: Int!, $simulationDatasetId: Int!) {
        delete_simulation_by_pk(id: $simulationId) {
          id
        }
        delete_simulation_dataset_by_pk(id: $simulationDatasetId) {
          id
        }
      }
    `,
    {
      simulationId: simulationPk.simulationId,
      simulationDatasetId: simulationPk.simulationDatasetId,
    },
  );
}
