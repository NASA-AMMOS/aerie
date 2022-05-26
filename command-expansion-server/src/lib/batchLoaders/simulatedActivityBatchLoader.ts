import { BatchLoader, InferredDataloader } from './index.js';
import { gql, GraphQLClient } from 'graphql-request';
import { ErrorWithStatusCode } from '../../utils/ErrorWithStatusCode.js';
import parse from 'postgres-interval';
import { GraphQLActivitySchema, Schema, SchemaTypes } from './activitySchemaBatchLoader.js';
import { activitySchemaBatchLoader } from './activitySchemaBatchLoader.js';

export const simulatedActivitiesBatchLoader: BatchLoader<
  { simulationDatasetId: number },
  SimulatedActivity[],
  { graphqlClient: GraphQLClient; activitySchemaDataLoader: InferredDataloader<typeof activitySchemaBatchLoader> }
> = opts => async keys => {
  const result = await opts.graphqlClient.batchRequests<
    {
      data: {
        simulated_activity: GraphQLSimulatedActivityInstance[];
      };
    }[]
  >(
    keys.map(key => ({
      document: gql`
        query ($simulationDatasetId: Int!) {
          simulated_activity(where: { simulation_dataset_id: { _eq: $simulationDatasetId } }) {
            id
            simulation_dataset {
              id
              simulation {
                plan {
                  model_id
                }
              }
            }
            attributes
            start_offset
            duration
            activity_type_name
          }
        }
      `,
      variables: {
        simulationDatasetId: key.simulationDatasetId,
      },
    })),
  );

  return Promise.all(
    keys.map(async ({ simulationDatasetId }) => {
      const simulatedActivities = result.find(
        res => res.data.simulated_activity[0]?.simulation_dataset.id === simulationDatasetId,
      )?.data.simulated_activity;
      if (simulatedActivities === undefined) {
        return new ErrorWithStatusCode(`No simulation_dataset with id: ${simulationDatasetId}`, 404);
      }
      return Promise.all(
        simulatedActivities.map(async simulatedActivity =>
          mapGraphQLActivityInstance(
            simulatedActivity,
            await opts.activitySchemaDataLoader.load({
              missionModelId: simulatedActivity.simulation_dataset.simulation.plan.model_id,
              activityTypeName: simulatedActivity.activity_type_name,
            }),
          ),
        ),
      );
    }),
  );
};

export const simulatedActivityInstanceBySimulatedActivityIdBatchLoader: BatchLoader<
  { simulationDatasetId: number; simulatedActivityId: number },
  SimulatedActivity,
  { graphqlClient: GraphQLClient; activitySchemaDataLoader: InferredDataloader<typeof activitySchemaBatchLoader> }
> = opts => async keys => {
  const result = await opts.graphqlClient.batchRequests<
    {
      data: {
        simulated_activity: GraphQLSimulatedActivityInstance[];
      };
    }[]
  >(
    keys.map(key => ({
      document: gql`
        query ($simulationDatasetId: Int!, $simulatedActivityId: Int!) {
          simulated_activity(
            where: { simulation_dataset_id: { _eq: $simulationDatasetId }, id: { _eq: $simulatedActivityId } }
          ) {
            id
            simulation_dataset {
              id
              simulation {
                plan {
                  model_id
                }
              }
            }
            attributes
            start_offset
            duration
            activity_type_name
          }
        }
      `,
      variables: {
        simulationDatasetId: key.simulationDatasetId,
        simulatedActivityId: key.simulatedActivityId,
      },
    })),
  );

  return Promise.all(
    keys.map(async ({ simulationDatasetId, simulatedActivityId }) => {
      const simulatedActivity = result.find(
        res =>
          res.data.simulated_activity[0]?.simulation_dataset.id === simulationDatasetId &&
          res.data.simulated_activity[0]?.id === simulatedActivityId,
      )?.data.simulated_activity[0];
      if (simulatedActivity === undefined) {
        return new ErrorWithStatusCode(
          `No simulation_dataset with id: ${simulationDatasetId} and simulated activity id: ${simulatedActivityId}`,
          404,
        );
      }
      return mapGraphQLActivityInstance(
        simulatedActivity,
        await opts.activitySchemaDataLoader.load({
          missionModelId: simulatedActivity.simulation_dataset.simulation.plan.model_id,
          activityTypeName: simulatedActivity.activity_type_name,
        }),
      );
    }),
  );
};

export interface SimulatedActivityAttributes<
  ActivityArguments extends Record<string, unknown> = Record<string, unknown>,
  ActivityComputedAttributes extends Record<string, unknown> = Record<string, unknown>,
> {
  arguments: ActivityArguments;
  directiveId?: number;
  computedAttributes?: ActivityComputedAttributes;
}

export interface SimulatedActivity<
  ActivityArguments extends Record<string, unknown> = Record<string, unknown>,
  ActivityComputedAttributes extends Record<string, unknown> = Record<string, unknown>,
> {
  id: number;
  simulationDatasetId: number;
  attributes: SimulatedActivityAttributes<ActivityArguments, ActivityComputedAttributes>;
  duration: Temporal.Duration;
  startOffset: Temporal.Duration;
  activityTypeName: string;
}

export interface GraphQLSimulatedActivityInstance<
  ActivityArguments extends Record<string, unknown> = Record<string, unknown>,
  ActivityComputedAttributes extends Record<string, unknown> = Record<string, unknown>,
> {
  id: number;
  simulation_dataset: {
    id: number;
    simulation: {
      plan: {
        model_id: number;
      };
    };
  };
  attributes: SimulatedActivityAttributes<ActivityArguments, ActivityComputedAttributes>;
  duration: string;
  start_offset: string;
  activity_type_name: string;
}

export function mapGraphQLActivityInstance(
  activityInstance: GraphQLSimulatedActivityInstance<any, any>,
  activitySchema: GraphQLActivitySchema,
): SimulatedActivity {
  return {
    id: activityInstance.id,
    duration: Temporal.Duration.from(parse(activityInstance.duration).toISOString()),
    startOffset: Temporal.Duration.from(parse(activityInstance.start_offset).toISOString()),
    simulationDatasetId: activityInstance.simulation_dataset.id,
    activityTypeName: activityInstance.activity_type_name,
    attributes: {
      arguments: Object.entries(activityInstance.attributes.arguments).reduce((acc, [key, value]) => {
        acc[key] = convertType(value, activitySchema.parameters[key].schema);
        return acc;
      }, {} as { [attributeName: string]: any }),
      directiveId: activityInstance.attributes.directiveId,
    },
  };
}

function convertType(value: any, schema: Schema): any {
  switch (schema.type) {
    case SchemaTypes.Int:
      if (value > Number.MAX_SAFE_INTEGER || value < Number.MIN_SAFE_INTEGER) {
        return value.toString();
      }
      return parseInt(value, 10);
    case SchemaTypes.Real:
      return value;
    case SchemaTypes.Duration:
      return Temporal.Duration.from(parse(value).toISOString());
    case SchemaTypes.Boolean:
      return value;
    case SchemaTypes.String:
      return value;
    case SchemaTypes.Series:
      return value.map((value: any) => convertType(value, schema.items));
    case SchemaTypes.Struct:
      const struct: { [attributeName: string]: any } = {};
      for (const [attributeKey, attributeSchema] of Object.entries(schema.items)) {
        struct[attributeKey] = convertType(value[attributeKey], attributeSchema);
      }
      return struct;
    case SchemaTypes.Variant:
      if (schema.variants.length === 1 && schema.variants[0].key === 'VOID') {
        return null;
      }
      return value;
    default:
      throw new Error(`Unknown schema type: ${(schema as any).type}`);
  }
}
