import { BatchLoader } from './index';
import { gql, GraphQLClient } from 'graphql-request';
import { ErrorWithStatusCode } from '../../utils/ErrorWithStatusCode.js';

export const simulatedActivityInstanceBatchLoader: BatchLoader<
  { simulationDatasetId: number },
  SimulatedActivityInstance<{ [attributeName: string]: unknown }, { [attributeName: string]: unknown }>[],
  { graphqlClient: GraphQLClient }
> = opts => async keys => {
  const { simulation_dataset } = await opts.graphqlClient.request<{
    simulation_dataset: {
      id: number;
      dataset: {
        spans: SimulatedActivityInstance<{ [attributeName: string]: unknown }, { [attributeName: string]: unknown }>[];
      };
    }[];
  }>(
    gql`
      query GetSimulationDatasetActivities($simulationDatasetIds: [Int!]!) {
        simulation_dataset(where: { id: { _in: $simulationDatasetIds } }) {
          id
          dataset {
            spans {
              id
              attributes
              duration
              start_offset
              type
            }
          }
        }
      }
    `,
    {
      simulationDatasetIds: keys.map(key => key.simulationDatasetId),
    },
  );

  return Promise.all(
    keys.map(async ({ simulationDatasetId }) => {
      const simulationDataset = simulation_dataset.find(
        simulationDataset => simulationDataset.id === simulationDatasetId,
      );
      if (simulationDataset === undefined) {
        return new ErrorWithStatusCode(`No simulation_dataset with id: ${simulationDatasetId}`, 404);
      }
      return simulationDataset.dataset.spans;
    }),
  );
};

export interface ActivityAttributes<
  ActivityArguments extends { [attributeName: string]: unknown },
  ActivityComputedAttributes,
> {
  arguments: ActivityArguments;
  directiveId: number;
  computedAttributes: ActivityComputedAttributes;
}

export interface SimulatedActivityInstance<
  ActivityArguments extends { [attributeName: string]: unknown },
  ActivityComputedAttributes,
> {
  id: number;
  attributes: ActivityAttributes<ActivityArguments, ActivityComputedAttributes>;
  duration: string;
  start_offset: string;
  type: string;
}
