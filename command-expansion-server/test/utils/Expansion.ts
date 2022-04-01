import { gql, GraphQLClient } from 'graphql-request';

export async function insertExpansion(graphqlClient: GraphQLClient): Promise<number> {
  const res = await graphqlClient.request<{
    addCommandExpansionTypeScript: { id: number}
  }>(gql`
    mutation AddCommandExpansion(
      $activityTypeName: String!,
      $expansionLogic: String!
    ) {
      addCommandExpansionTypeScript(
        activityTypeName: $activityTypeName,
        expansionLogic: $expansionLogic
      ) {
        id
      }
    }
  `, {
    activityTypeName: "PeelBanana",
    expansionLogic: "ZXhwb3J0IGRlZmF1bHQgZnVuY3Rpb24gU2luZ2xlQ29tbWFuZEV4cGFuc2lvbihwcm9wczogeyBhY3Rpdml0eUluc3RhbmNlOiBBY3Rpdml0eVR5cGUgfSk6IEV4cGFuc2lvblJldHVybiB7CiAgcmV0dXJuIFsKICAgIFBSRUhFQVRfT1ZFTih7dGVtcGVyYXR1cmU6IDcwfSksCiAgICBQUkVQQVJFX0xPQUYoNTAsIGZhbHNlKSwKICAgIEJBS0VfQlJFQUQsCiAgXTsKfQ=="
  });
  return res.addCommandExpansionTypeScript.id;
}



export async function insertErrorExpansion(graphqlClient: GraphQLClient): Promise<number> {
  const res = await graphqlClient.request<{
    addCommandExpansionTypeScript: { id: number}
  }>(gql`
    mutation AddCommandExpansion(
      $activityTypeName: String!,
      $expansionLogic: String!
    ) {
      addCommandExpansionTypeScript(
        activityTypeName: $activityTypeName,
        expansionLogic: $expansionLogic
      ) {
        id
      }
    }
  `, {
    activityTypeName: "BiteBanana",
    expansionLogic: "ZXhwb3J0IGRlZmF1bHQgZnVuY3Rpb24gRXJyb3JFeHBhbnNpb24ocHJvcHM6IHsgYWN0aXZpdHlJbnN0YW5jZTogQWN0aXZpdHlUeXBlIH0pOiBFeHBhbnNpb25SZXR1cm4gewogIHRocm93IG5ldyBFcnJvcignTm90IGltcGxlbWVudGVkJyk7Cn0="
  });
  return res.addCommandExpansionTypeScript.id;
}

export async function removeExpansion(graphqlClient: GraphQLClient, $expansionId: number): Promise<void> {
  return graphqlClient.request(gql`
    mutation DeleteExpansionRule($expansionId: Int!) {
      delete_expansion_rule_by_pk(id: $activityId) {
        id
      }
    }
  `, {
    $expansionId,
  });
}


export async function insertExpansionSet(graphqlClient: GraphQLClient, commandDictionaryId: number, missionModelId: number, expansionIds: number[]): Promise<number> {
  const res = await graphqlClient.request<{
    createExpansionSet: { id: number}
  }>(gql`
    mutation AddExpansionSet(
      $commandDictionaryId: Int!
      $missionModelId: Int!
      $expansionIds: [Int!]!
    ) {
      createExpansionSet(
        commandDictionaryId: $commandDictionaryId,
        missionModelId: $missionModelId,
        expansionIds: $expansionIds
      ) {
        id
      }
    }
  `, {
    commandDictionaryId,
    missionModelId,
    expansionIds,
  });
  return res.createExpansionSet.id;
}

export async function removeExpansionSet(graphqlClient: GraphQLClient, expansionSetId: number): Promise<void> {
  return graphqlClient.request(gql`
    mutation DeleteExpansionSet($expansionSetId: Int!) {
      delete_expansion_set_by_pk(id: $expansionSetId) {
        id
      }
    }
  `, {
    expansionSetId,
  });
}
