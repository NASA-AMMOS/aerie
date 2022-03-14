import { globalDeclaration, indent, interfaceDeclaration } from './CodegenHelpers.js';
import {GraphQLActivitySchema, Schema, SchemaTypes} from '../batchLoaders/activitySchemaBatchLoader.js';

export function generateTypescriptForGraphQLActivitySchema(activitySchema: GraphQLActivitySchema): string {
  const propertyDeclarations = Object.entries(activitySchema.parameters)
    .map(([parameterName, parameterValue]) => `readonly ${parameterName}: ${convertSchemaType(parameterValue.schema)};`)
    .join('\n');
  const activityTypeAlias = `type ActivityType = ${activitySchema.name};`;

  return globalDeclaration(`${interfaceDeclaration(activitySchema.name, propertyDeclarations)}\n${activityTypeAlias}`);
}

function convertSchemaType(schema: Schema): string {
  const objectDeclaration = (content: string) => `{\n${indent(content)}\n}`;
  switch (schema.type) {
    case SchemaTypes.Int:
    case SchemaTypes.Real:
    case SchemaTypes.Duration:
      return 'number';
    case SchemaTypes.Boolean:
      return 'boolean';
    case SchemaTypes.String:
      return 'string';
    case SchemaTypes.Series:
      return `${convertSchemaType(schema.items)}[]`;
    case SchemaTypes.Struct:
      return objectDeclaration(
        Object.entries(schema.items)
          .map(([key, value]) => `${key}: ${convertSchemaType(value)};`)
          .join('\n')
      );
    case SchemaTypes.Variant:
      return `(${schema.variants.map((variant) => `'${variant.label}'`).join(' | ')})`;
    default:
      return 'unknown';
  }
}
