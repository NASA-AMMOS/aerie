import { ErrorWithStatusCode } from '../../utils/ErrorWithStatusCode.js';
import type { BatchLoader } from './index.js';
import { gql, GraphQLClient } from 'graphql-request';

export const expansionBatchLoader: BatchLoader<
  { expansionId: number },
  {
    id: number;
    activityType: string;
    expansionLogic: string;
  },
  { graphqlClient: GraphQLClient }
> = opts => async keys => {
  const { expansion_rule } = await opts.graphqlClient.request<{
    expansion_rule: {
      id: number;
      activity_type: string;
      expansion_logic: string;
    }[];
  }>(
    gql`
      query GetExpansions($expansionIds: [Int!]!) {
        expansion_rule(where: { id: { _in: $expansionIds } }) {
          id
          activity_type
          expansion_logic
        }
      }
    `,
    {
      expansionIds: keys.map(key => key.expansionId),
    },
  );

  return Promise.all(
    keys.map(async ({ expansionId }) => {
      const expansion = expansion_rule.find(({ id }) => id.toString() === expansionId.toString());
      if (expansion === undefined) {
        return new ErrorWithStatusCode(`No expansion with id: ${expansionId}`, 404);
      }
      return {
        id: expansion.id,
        activityType: expansion.activity_type,
        expansionLogic: expansion.expansion_logic,
      };
    }),
  );
};
