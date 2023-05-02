import { gql, GraphQLClient } from 'graphql-request';

export async function insertExpansion(
  graphqlClient: GraphQLClient,
  activityTypeName: string,
  expansionLogic: string,
): Promise<number> {
  const res = await graphqlClient.request<{
    addCommandExpansionTypeScript: { id: number };
  }>(
    gql`
      mutation AddCommandExpansion($activityTypeName: String!, $expansionLogic: String!) {
        addCommandExpansionTypeScript(activityTypeName: $activityTypeName, expansionLogic: $expansionLogic) {
          id
        }
      }
    `,
    {
      activityTypeName,
      expansionLogic,
    },
  );
  return res.addCommandExpansionTypeScript.id;
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
  commandDictionaryId: number,
  missionModelId: number,
  expansionIds: number[],
): Promise<number> {
  const res = await graphqlClient.request<{
    createExpansionSet: { id: number };
  }>(
    gql`
      mutation AddExpansionSet($commandDictionaryId: Int!, $missionModelId: Int!, $expansionIds: [Int!]!) {
        createExpansionSet(
          commandDictionaryId: $commandDictionaryId
          missionModelId: $missionModelId
          expansionIds: $expansionIds
        ) {
          id
        }
      }
    `,
    {
      commandDictionaryId,
      missionModelId,
      expansionIds,
    },
  );
  return res.createExpansionSet.id;
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

  return { expandedSequence: result.expanded_sequences[0].expanded_sequence };
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
