import type { BatchLoader } from './index';
import { gql, GraphQLClient } from 'graphql-request';
import { ErrorWithStatusCode } from '../../utils/ErrorWithStatusCode.js';
import type { GraphQLActivitySchema } from './activitySchemaBatchLoader.js';
import fs from 'fs';

export const expansionSetBatchLoader: BatchLoader<
  { expansionSetId: number },
  ExpansionSetData,
  { graphqlClient: GraphQLClient }
> = opts => async keys => {
  const { expansion_set } = await opts.graphqlClient.request<ExpansionSets>(
    gql`
      query GetExpansionSet($expansionSetIds: [Int!]!) {
        expansion_set(where: { id: { _in: $expansionSetIds }, parcel: {} }) {
          id
          mission_model {
            id
            activity_types {
              name
              parameters
              computed_attributes_value_schema
            }
          }
          expansion_rules {
            id
            activity_type
            expansion_logic
          }
          parcel {
            id
            command_dictionary {
              path
              id
            }
            channel_dictionary {
              path
              id
            }
            parameter_dictionary {
              parameter_dictionary {
                path
                id
              }
            }
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
      const commandTypes = await fs.promises.readFile(expansionSet.parcel.command_dictionary.path, 'utf8');
      const channelTypes = JSON.parse(
        expansionSet.parcel.channel_dictionary
          ? await fs.promises.readFile(expansionSet.parcel.channel_dictionary.path, 'utf8')
          : '{}',
      );
      const paramerterTypes = await Promise.allSettled(
        expansionSet.parcel.parameter_dictionary.map(async param => {
          return {
            parameter_dictionary: {
              parsedJson: JSON.parse(await fs.promises.readFile(param.parameter_dictionary.path, 'utf8')),
              id: param.parameter_dictionary.id,
            },
          };
        }),
      );

      const paramerterResults: {
        parameter_dictionary: {
          parsedJson: string;
          id: number;
        };
      }[] = [];
      for (const result of paramerterTypes) {
        if (result.status === 'fulfilled') {
          paramerterResults.push(result.value);
        }
      }

      return {
        id: expansionSet.id,
        parcel: {
          id: expansionSet.parcel.id,
          command_dictionary: {
            id: expansionSet.parcel.command_dictionary.id,
            commandTypesTypeScript: commandTypes,
          },
          ...(expansionSet.parcel.channel_dictionary
            ? {
                channel_dictionary: {
                  id: expansionSet.parcel.channel_dictionary.id,
                  parsedJson: channelTypes,
                },
              }
            : {}),

          parameter_dictionary: paramerterResults,
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
      } as ExpansionSetData;
    }),
  );
};

export type ExpansionSets = {
  expansion_set: ExpansionSet[];
};
export interface ExpansionSet {
  id: number;
  mission_model: {
    id: number;
    activity_types: GraphQLActivitySchema[];
  };
  expansion_rules: {
    id: number;
    activity_type: string;
    expansion_logic: string;
  }[];
  parcel: {
    id: number;
    command_dictionary: {
      path: string;
      id: number;
    };
    parameter_dictionary: {
      parameter_dictionary: {
        path: string;
        id: number;
      };
    }[];
    channel_dictionary?: {
      path: string;
      id: number;
    };
  };
}

export interface ExpansionSetData {
  id: number;
  parcel: {
    id: number;
    command_dictionary: {
      commandTypesTypeScript: string;
      id: number;
    };
    parameter_dictionary: {
      parameter_dictionary: {
        id: number;
        parsedJson: string;
      };
    }[];
    channel_dictionary?: {
      id: number;
      parsedJson: string;
    };
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
