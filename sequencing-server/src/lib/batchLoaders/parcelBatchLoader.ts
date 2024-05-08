import { ErrorWithStatusCode } from '../../utils/ErrorWithStatusCode.js';
import type { BatchLoader } from './index.js';
import { gql, GraphQLClient } from 'graphql-request';

export type Parcel = {
  id: number;
  command_dictionary: {
    id: number;
  };
  parameter_dictionary: {
    parameter_dictionary: {
      id: number;
    };
  }[];
  channel_dictionary?: {
    id: number;
  };
};

export type Parcels = {
  parcel: Parcel[];
};

export const parcelBatchLoader: BatchLoader<{ parcelId: number }, Parcel, { graphqlClient: GraphQLClient }> =
  opts => async keys => {
    const { parcel } = await opts.graphqlClient.request<Parcels>(
      gql`
        query GetParcels($parcelIds: [Int!]!) {
          parcel(where: { id: { _in: $parcelIds } }) {
            id
            parameter_dictionary(where: { parcel_id: { _in: $parcelIds } }) {
              parameter_dictionary {
                id
              }
            }
            command_dictionary {
              id
            }
            channel_dictionary {
              id
            }
          }
        }
      `,
      {
        parcelIds: keys.map(key => key.parcelId),
      },
    );

    return Promise.all(
      keys.map(async ({ parcelId }) => {
        const par = parcel.find(({ id }) => id.toString() === parcelId.toString());
        if (par === undefined) {
          return new ErrorWithStatusCode(`No parcel with id: ${parcelId}`, 404);
        }
        return par;
      }),
    );
  };
