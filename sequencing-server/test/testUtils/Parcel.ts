import { gql, GraphQLClient } from 'graphql-request';

export async function insertParcel(
  graphqlClient: GraphQLClient,
  dictionaryID: number,
  parcelName: string,
): Promise<{
  parcelId: number;
}> {
  const res = await graphqlClient.request<{
    insert_parcel: {
      returning: {
        id: number;
      }[];
    };
  }>(
    gql`
      mutation PutParcel($dictionaryID: Int!, $parcelName: String!) {
        insert_parcel(objects: { command_dictionary_id: $dictionaryID, name: $parcelName }) {
          returning {
            id
          }
        }
      }
    `,
    {
      // Generate a UUID for the command dictionary name and version to avoid conflicts when testing.
      dictionaryID,
      parcelName,
    },
  );

  return { parcelId: res.insert_parcel.returning[0]?.id ?? -1 };
}

export async function removeParcel(graphqlClient: GraphQLClient, parcelId: number): Promise<void> {
  return graphqlClient.request(
    gql`
      mutation RemoveParcel($parcelId: Int!) {
        delete_parcel_by_pk(id: $parcelId) {
          id
        }
      }
    `,
    {
      parcelId,
    },
  );
}
