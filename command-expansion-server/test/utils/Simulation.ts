import { gql, GraphQLClient } from 'graphql-request';
import perf from 'perf_hooks';
import { jest } from '@jest/globals';

jest.setTimeout(10000);

export async function executeSimulation(graphqlClient: GraphQLClient, planId: number): Promise<number> {
  const startTime = perf.performance.now();
  while (perf.performance.now() - startTime < 10000) {
    const simulationRes = await graphqlClient.request(
      gql`
        query SimulatePlan($planId: Int!) {
          simulate(planId: $planId) {
            status
            results
            reason
          }
        }
      `,
      {
        planId,
      },
    );
    const status = simulationRes.simulate.status;
    if (status !== 'incomplete') {
      return simulationRes.simulate.results;
    }
  }
  throw new Error('Simulation timed out');
}

export async function removeSimulation(graphqlClient: GraphQLClient, simulationId: number): Promise<void> {
  /*
   * Remove a plan
   */

  await graphqlClient.request(
    gql`
      mutation DeleteSimulation($simulationId: Int!) {
        delete_simulation_by_pk(id: $simulationId) {
          id
        }
      }
    `,
    {
      simulationId,
    },
  );
}
