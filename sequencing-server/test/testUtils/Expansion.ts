import { gql, GraphQLClient } from 'graphql-request';

export async function insertExpansion(
  graphqlClient: GraphQLClient,
  activityTypeName: string,
  expansionLogic: string,
  parcelId: number,
): Promise<number> {
  const res = await graphqlClient.request<{
    addCommandExpansionTypeScript: { id: number };
  }>(
    gql`
      mutation AddCommandExpansion($activityTypeName: String!, $expansionLogic: String!, $parcelId: Int!) {
        addCommandExpansionTypeScript(
          activityTypeName: $activityTypeName
          expansionLogic: $expansionLogic
          parcelId: $parcelId
        ) {
          id
        }
      }
    `,
    {
      activityTypeName,
      expansionLogic,
      parcelId,
    },
  );
  return res.addCommandExpansionTypeScript.id;
}

export async function getExpansion(
  graphqlClient: GraphQLClient,
  expansionId: number,
): Promise<{
  id: number;
  expansion_logic: string;
  name: string;
  parcel_id: number;
}> {
  const res = await graphqlClient.request<{
    expansion_rule_by_pk: any;
  }>(
    gql`
      query GetExpansion($expansionId: Int!) {
        expansion_rule_by_pk(id: $expansionId) {
          id
          expansion_logic
          name
          parcel_id
        }
      }
    `,
    {
      expansionId,
    },
  );
  return res.expansion_rule_by_pk;
}

export async function removeExpansion(graphqlClient: GraphQLClient, expansionId: number): Promise<void> {
  return graphqlClient.request(
    gql`
      mutation DeleteExpansionRule($expansionId: Int!) {
        delete_expansion_rule_by_pk(id: $expansionId) {
          id
        }
      }
    `,
    {
      expansionId,
    },
  );
}

export async function insertExpansionSet(
  graphqlClient: GraphQLClient,
  parcelId: number,
  missionModelId: number,
  expansionIds: number[],
  description?: string,
  name?: string,
): Promise<number> {
  const res = await graphqlClient.request<{
    createExpansionSet: { id: number };
  }>(
    gql`
      mutation AddExpansionSet(
        $parcelId: Int!
        $missionModelId: Int!
        $expansionIds: [Int!]!
        $description: String
        $name: String
      ) {
        createExpansionSet(
          parcelId: $parcelId
          missionModelId: $missionModelId
          expansionIds: $expansionIds
          description: $description
          name: $name
        ) {
          id
        }
      }
    `,
    {
      parcelId,
      missionModelId,
      expansionIds,
      description,
      name,
    },
  );
  return res.createExpansionSet.id;
}

export async function getExpansionSet(
  graphqlClient: GraphQLClient,
  expansionSetId: number,
): Promise<{
  id: number;
  name: string;
  description: string;
  mission_model_id: number;
  parcel_id: number;
  expansion_rules: { id: number }[];
} | null> {
  const res = await graphqlClient.request(
    gql`
      query GetExpansionSet($expansionSetId: Int!) {
        expansion_set_by_pk(id: $expansionSetId) {
          id
          name
          description
          mission_model_id
          parcel_id
          expansion_rules {
            id
          }
        }
      }
    `,
    {
      expansionSetId,
    },
  );

  return res.expansion_set_by_pk;
}

export async function removeExpansionSet(graphqlClient: GraphQLClient, expansionSetId: number): Promise<void> {
  return graphqlClient.request(
    gql`
      mutation DeleteExpansionSet($expansionSetId: Int!) {
        delete_expansion_set_by_pk(id: $expansionSetId) {
          id
        }
      }
    `,
    {
      expansionSetId,
    },
  );
}

export async function expand(
  graphqlClient: GraphQLClient,
  expansionSetId: number,
  simulationDatasetId: number,
): Promise<number> {
  const result = await graphqlClient.request<{
    expandAllActivities: {
      id: number;
    };
  }>(
    gql`
      mutation Expand($expansionSetId: Int!, $simulationDatasetId: Int!) {
        expandAllActivities(expansionSetId: $expansionSetId, simulationDatasetId: $simulationDatasetId) {
          id
        }
      }
    `,
    {
      expansionSetId,
      simulationDatasetId,
    },
  );

  return result.expandAllActivities.id;
}

export async function getExpandedSequence(
  graphqlClient: GraphQLClient,
  expansionRunId: number,
  seqId: string,
): Promise<{
  expandedSequence: Sequence;
}> {
  const result = await graphqlClient.request<{
    expanded_sequences: [
      {
        expanded_sequence: Sequence;
      },
    ];
  }>(
    gql`
      query GetExpandedSequence($expansionRunId: Int!, $seqId: String!) {
        expanded_sequences(where: { expansion_run_id: { _eq: $expansionRunId }, seq_id: { _eq: $seqId } }) {
          expanded_sequence
        }
      }
    `,
    {
      expansionRunId,
      seqId,
    },
  );

  return {
    expandedSequence: result.expanded_sequences[0].expanded_sequence,
  };
}

export async function removeExpansionRun(graphqlClient: GraphQLClient, expansionRunId: number): Promise<void> {
  await graphqlClient.request<{
    delete_expansion_run_by_pk: {
      id: number;
    };
  }>(
    gql`
      mutation deleteExpansionRun($expansionRunId: Int!) {
        delete_expansion_run_by_pk(id: $expansionRunId) {
          id
        }
      }
    `,
    {
      expansionRunId,
    },
  );

  return;
}
