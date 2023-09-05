import type { BatchLoader } from './index';
import { gql, GraphQLClient } from 'graphql-request';
import { ErrorWithStatusCode } from '../../utils/ErrorWithStatusCode.js';
import type { GraphQLActivitySchema } from './activitySchemaBatchLoader.js';
import fs from 'fs';

export const expansionSetBatchLoader: BatchLoader<
  { expansionSetId: number },
  ExpansionSet,
  { graphqlClient: GraphQLClient }
> = opts => async keys => {
  const { expansion_set } = await opts.graphqlClient.request<{
    expansion_set: {
      id: number;
      command_dictionary: {
        id: number;
        command_types_typescript_path: string;
      };
      mission_model: {
        id: number;
        activity_types: GraphQLActivitySchema[];
      };
      expansion_rules: {
        id: number;
        activity_type: string;
        expansion_logic: string;
      }[];
    }[];
  }>(
    gql`
      query GetExpansionSet($expansionSetIds: [Int!]!) {
        expansion_set(where: { id: { _in: $expansionSetIds } }) {
          id
          command_dictionary {
            id
            command_types_typescript_path
          }
          mission_model {
            id
            activity_types {
              name
              parameter_definitions
              computed_attribute_definitions
            }
          }
          expansion_rules {
            id
            activity_type
            expansion_logic
          }
        }
      }
    `,
    {
      expansionSetIds: keys.map(key => key.expansionSetId),
    },
  );

  return Promise.all(
    keys.map(async ({ expansionSetId }) => {
      const expansionSet = expansion_set.find(expansionSet => expansionSet.id === expansionSetId);
      if (expansionSet === undefined) {
        return new ErrorWithStatusCode(`No expansion_set with id: ${expansionSetId}`, 404);
      }
      const commandTypes = await fs.promises.readFile(
        expansionSet.command_dictionary.command_types_typescript_path,
        'utf8',
      );
      return {
        id: expansionSet.id,
        commandDictionary: {
          id: expansionSet.command_dictionary.id,
          commandTypesTypeScript: commandTypes,
        },
        missionModel: {
          id: expansionSet.mission_model.id,
          activityTypes: expansionSet.mission_model.activity_types,
        },
        expansionRules: expansionSet.expansion_rules.map(expansionRule => ({
          id: expansionRule.id,
          activityType: expansionRule.activity_type,
          expansionLogic: expansionRule.expansion_logic,
        })),
      };
    }),
  );
};

export interface ExpansionSet {
  id: number;
  commandDictionary: {
    id: number;
    commandTypesTypeScript: string;
  };
  missionModel: {
    id: number;
    activityTypes: GraphQLActivitySchema[];
  };
  expansionRules: {
    id: number;
    activityType: string;
    expansionLogic: string;
  }[];
}
