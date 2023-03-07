import { globalDeclaration, indent, interfaceDeclaration } from './CodegenHelpers.js';
import { GraphQLActivitySchema, Schema, SchemaTypes } from '../batchLoaders/activitySchemaBatchLoader.js';

const commonProperties = `readonly duration: Temporal.Duration;
readonly startOffset: Temporal.Duration;
readonly startTime: Temporal.Instant;
readonly endTime: Temporal.Instant;`;

export function generateTypescriptForGraphQLActivitySchema(activitySchema: GraphQLActivitySchema): string {
  const activityTypeAlias = `type ActivityType = ActivityTypes.${activitySchema.name};`;

  const activityTypeDeclaration = `readonly type: '${activitySchema.name}';`;

  const argumentDeclarations = Object.entries(activitySchema.parameters)
    .map(([parameterName, parameterValue]) => `readonly ${parameterName}: ${convertSchemaType(parameterValue.schema)};`)
    .join('\n');

  const argumentsDeclaration = `readonly arguments: {\n${indent(argumentDeclarations)}\n}`;
  const computedAttributesDeclaration = `readonly computed: ${convertSchemaType(
    activitySchema.computed_attributes_value_schema,
  )};`;

  const attributesDeclaration = `readonly attributes: {\n${indent(
    [argumentsDeclaration, computedAttributesDeclaration].join('\n'),
  )}\n}`;

  const propertyDeclarations = [activityTypeDeclaration, commonProperties, attributesDeclaration].join('\n');

  return (
    globalDeclaration(
      `namespace ActivityTypes {\n${interfaceDeclaration(
        activitySchema.name,
        propertyDeclarations,
      )}\n}\n${activityTypeAlias}`,
    ) + '\n\nexport {};'
  );
}

function convertSchemaType(schema: Schema): string {
  const objectDeclaration = (content: string) => `{\n${indent(content)}\n}`;
  switch (schema.type) {
    case SchemaTypes.Int:
    case SchemaTypes.Real:
      return 'number';
    case SchemaTypes.Duration:
      return 'Temporal.Duration';
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
          .join('\n'),
      );
    case SchemaTypes.Variant:
      if (schema.variants.length === 1 && schema.variants[0]?.key === 'VOID') {
        return 'null';
      }
      return `(${schema.variants.map(variant => `'${variant.label}'`).join(' | ')})`;
    default:
      return 'unknown';
  }
}
