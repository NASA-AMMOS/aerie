import { jest } from '@jest/globals';
import { gql, GraphQLClient } from 'graphql-request';
import perf from 'perf_hooks';

jest.setTimeout(10000);

export async function updateSimulationBounds(
    graphqlClient: GraphQLClient,
    bounds: {plan_id: number, simulation_start_time: string, simulation_end_time: string}
): Promise<void> {
  await graphqlClient.request(
      gql`
      mutation updateSimulationBounds($plan_id: Int!, $simulation_start_time: timestamptz!, $simulation_end_time: timestamptz!) {
      update_simulation(where: {plan_id: {_eq: $plan_id}},
      _set: {
        simulation_start_time: $simulation_start_time,
        simulation_end_time: $simulation_end_time}) {
        affected_rows
       }
    }
    `,
      {
        plan_id: bounds.plan_id,
        simulation_start_time: bounds.simulation_start_time,
        simulation_end_time: bounds.simulation_end_time
      },
  );
}

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
      throw new Error(JSON.stringify(simulationRes.simulate.reason));
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
  simulationPk: { simulationDatasetId: number },
): Promise<void> {
  /*
   * Remove a plan
   */

  await graphqlClient.request(
    gql`
      mutation DeleteSimulation($simulationDatasetId: Int!) {
        delete_simulation_dataset_by_pk(id: $simulationDatasetId) {
          id
        }
      }
    `,
    {
      simulationDatasetId: simulationPk.simulationDatasetId,
    },
  );
}
