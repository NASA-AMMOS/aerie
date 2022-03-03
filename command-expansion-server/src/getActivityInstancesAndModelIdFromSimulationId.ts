import {gql, GraphQLClient} from 'graphql-request';
import {assertOne} from './utils/assertions.js';

interface ActivityInstance {
  type: string,
  arguments: {
    [key: string]: any,
  },
  id: number,
}

export async function getActivityInstancesAndModelIdFromSimulationId(graphqlClient: GraphQLClient, simulationId: number): Promise<{
  missionModelId: number,
  activityInstances: ActivityInstance[],
}> {
	const response = await graphqlClient.request<{
    simulation: {
      plan: {
        model_id: number,
        id: number,
        activities: {
          type: string,
          arguments: any,
          id: number,
        }[],
      },
    }[],
  }>(gql`
      query GetSimulationActivities(
          $simulationId: Int
      ) {
        simulation(where: {id: {_eq: $simulationId}}) {
          plan {
            model_id
            id
            activities {
              type
              arguments
              id
            }
          }
        }
      }
  `, {
    simulationId,
  });
  const simulation = assertOne(response.simulation);
  return {
    missionModelId: simulation.plan.model_id,
    activityInstances: simulation.plan.activities,
  };
}
