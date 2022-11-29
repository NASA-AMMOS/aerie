import { gql, GraphQLClient } from 'graphql-request';
import type { SequenceSeqJson } from '../../src/lib/codegen/CommandEDSLPreface.js';
import { FallibleStatus } from '../../src/types.js';
import { convertActivityDirectiveIdToSimulatedActivityId } from './ActivityDirective.js';

export async function insertSequence(
  graphqlClient: GraphQLClient,
  sequencePk: { simulationDatasetId: number; seqId: string },
): Promise<{ seqId: string; simulationDatasetId: number }> {
  /*
   * Create a sequence
   */
  const res = await graphqlClient.request<{
    insert_sequence_one: {
      seq_id: string;
      simulation_dataset_id: number;
    };
  }>(
    gql`
      mutation InsertSequence($seqId: String!, $simulationDatasetId: Int!) {
        insert_sequence_one(object: { simulation_dataset_id: $simulationDatasetId, seq_id: $seqId, metadata: {} }) {
          seq_id
          simulation_dataset_id
        }
      }
    `,
    {
      seqId: sequencePk.seqId,
      simulationDatasetId: sequencePk.simulationDatasetId,
    },
  );
  return { seqId: res.insert_sequence_one.seq_id, simulationDatasetId: res.insert_sequence_one.simulation_dataset_id };
}

export async function getSequenceSeqJson(
  graphqlClient: GraphQLClient,
  seqId: string,
  simulationDatasetId: number,
) {
  const { getSequenceSeqJson } = await graphqlClient.request<{
    getSequenceSeqJson: {
      status: FallibleStatus.FAILURE,
      seqJson?: SequenceSeqJson,
      errors: { message: string, stack: string}[]
    } | {
      status: FallibleStatus.SUCCESS,
      seqJson: SequenceSeqJson,
      errors: { message: string, stack: string}[]
    }
  }>(
      gql`
        query GetSeqJsonForSequence($seqId: String!, $simulationDatasetId: Int!) {
          getSequenceSeqJson(seqId: $seqId, simulationDatasetId: $simulationDatasetId) {
            status
            errors {
              message
              stack
            }
            seqJson {
              id
              metadata
              steps {
                type
                stem
                time {
                  type
                  tag
                }
                args
                metadata
              }
            }
          }
        }
      `,
      {
        seqId,
        simulationDatasetId,
      },
  );

  return getSequenceSeqJson;
}

export async function generateSequenceEDSL(
  graphqlClient: GraphQLClient,
  commandDictionaryID: number,
  edslBody: string,
): Promise<SequenceSeqJson> {
  const res = await graphqlClient.request<{
    getUserSequenceSeqJson: {
      status: FallibleStatus.FAILURE,
      seqJson?: SequenceSeqJson,
      errors: { message: string, stack: string}[]
    } | {
      status: FallibleStatus.SUCCESS,
      seqJson: SequenceSeqJson,
      errors: { message: string, stack: string}[]
    }
  }>(
    gql`
      query generateSequence($commandDictionaryID: Int!, $edslBody: String!) {
        getUserSequenceSeqJson(commandDictionaryID: $commandDictionaryID, edslBody: $edslBody) {
          status
          errors {
            message
            stack
          }
          seqJson {
            id
            metadata
            steps {
              args
              metadata
              stem
              time {
                tag
                type
              }
              type
            }
          }
        }
      }
    `,
    {
      commandDictionaryID: commandDictionaryID,
      edslBody: edslBody,
    },
  );

  if (res.getUserSequenceSeqJson.status === FallibleStatus.FAILURE) {
    throw res.getUserSequenceSeqJson.errors;
  }

  return res.getUserSequenceSeqJson.seqJson;
}

export async function generateSequenceEDSLBulk(
  graphqlClient: GraphQLClient,
  inputs: {
    commandDictionaryId: number,
    edslBody: string
  }[],
): Promise<SequenceSeqJson[]> {
  const res = await graphqlClient.request<{
    getUserSequenceSeqJsonBulk: ({
      status: FallibleStatus.FAILURE,
      seqJson?: SequenceSeqJson,
      errors: { message: string, stack: string}[]
    } | {
      status: FallibleStatus.SUCCESS,
      seqJson: SequenceSeqJson,
      errors: { message: string, stack: string}[]
    })[]
  }>(
    gql`
      query generateSequence($inputs: [GetUserSequenceSeqJsonBulkInput!]!) {
        getUserSequenceSeqJsonBulk(inputs: $inputs) {
          status
          errors {
            message
            stack
          }
          seqJson {
            id
            metadata
            steps {
              args
              metadata
              stem
              time {
                tag
                type
              }
              type
            }
          }
        }
      }
    `,
    {
      inputs,
    },
  );

  const errors = res.getUserSequenceSeqJsonBulk.filter((res) => res.status === FallibleStatus.FAILURE).flatMap((res) => res.errors);

  if (errors.length > 0) {
    throw errors;
  }

  return res.getUserSequenceSeqJsonBulk.map((res) => res.seqJson!);
}

export async function removeSequence(
  graphqlClient: GraphQLClient,
  sequencePk: { simulationDatasetId: number; seqId: string },
): Promise<void> {
  /*
   * Remove a sequence
   */

  await graphqlClient.request(
    gql`
      mutation RemoveSequence($seqId: String!, $simulationDatasetId: Int!) {
        delete_sequence_by_pk(simulation_dataset_id: $simulationDatasetId, seq_id: $seqId) {
          seq_id
          simulation_dataset_id
        }
      }
    `,
    {
      seqId: sequencePk.seqId,
      simulationDatasetId: sequencePk.simulationDatasetId,
    },
  );
}

export async function linkActivityInstance(
  graphqlClient: GraphQLClient,
  sequencePk: { simulationDatasetId: number; seqId: string },
  activityInstanceId: number,
): Promise<void> {
  const simulatedActivityId = await convertActivityDirectiveIdToSimulatedActivityId(
    graphqlClient,
    sequencePk.simulationDatasetId,
    activityInstanceId,
  );

  await graphqlClient.request(
    gql`
      mutation LinkSimulatedActivityToSequence(
        $seqId: String!
        $simulationDatasetId: Int!
        $simulatedActivityId: Int!
      ) {
        insert_sequence_to_simulated_activity_one(
          object: {
            seq_id: $seqId
            simulated_activity_id: $simulatedActivityId
            simulation_dataset_id: $simulationDatasetId
          }
        ) {
          seq_id
        }
      }
    `,
    {
      seqId: sequencePk.seqId,
      simulationDatasetId: sequencePk.simulationDatasetId,
      simulatedActivityId,
    },
  );
  return;
}

export async function unlinkActivityInstance(
  graphqlClient: GraphQLClient,
  simulationDatasetId: number,
  activityInstanceId: number,
): Promise<void> {
  await graphqlClient.request(
    gql`
      mutation UnlinkActivityToSequence($simulationDatasetId: Int!, $activityInstanceId: Int!) {
        delete_sequence_to_activity_instance_one(
          activity_instance_id: $activityInstanceId
          simulation_dataset_id: $simulationDatasetId
        ) {
          activity_instance_id
        }
      }
    `,
    {
      simulationDatasetId,
      activityInstanceId,
    },
  );
  return;
}
