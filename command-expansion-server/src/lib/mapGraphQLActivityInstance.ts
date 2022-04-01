import '../polyfills.js';
import parse from 'postgres-interval';
import { SimulatedActivityInstance } from './batchLoaders/simulatedActivityInstanceBatchLoader.js';
import { GraphQLActivitySchema, Schema, SchemaTypes } from './batchLoaders/activitySchemaBatchLoader.js';

export function mapGraphQLActivityInstance(
  activityInstance: SimulatedActivityInstance<any, any>,
  activitySchema: GraphQLActivitySchema,
): ActivityInstance {
  return {
    id: activityInstance.id,
    duration: Temporal.Duration.from(parse(activityInstance.duration).toISOString()),
    startOffset: Temporal.Duration.from(parse(activityInstance.start_offset).toISOString()),
    type: activityInstance.type,
    attributes: {
      arguments: Object.entries(activityInstance.attributes.arguments).reduce((acc, [key, value]) => {
        acc[key] = convertType(value, activitySchema.parameters[key].schema);
        return acc;
      }, {} as { [attributeName: string]: any }),
      computed:
        activityInstance.attributes.computedAttributes === 'VOID'
          ? null
          : () => {
              throw new Error(`Computed attributes are not supported yet`);
            },
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

export interface ActivityInstance {
  id: number;
  duration: Temporal.Duration;
  startOffset: Temporal.Duration;
  type: string;
  attributes: {
    arguments: {
      [key: string]: any;
    };
    computed: {
      [key: string]: any;
    } | null;
  };
}
