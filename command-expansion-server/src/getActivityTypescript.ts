import { GraphQLClient, gql } from "graphql-request";
import { globalDeclaration, indent, interfaceDeclaration } from "./packages/lib/CodegenHelpers.js";
import { ErrorWithStatusCode } from "./utils/ErrorWithStatusCode.js";

enum SchemaTypes {
  Int = "int",
  Real = "real",
  Duration = "duration",
  Boolean = "boolean",
  String = "string",
  Series = "series",
  Struct = "struct",
  Variant = "variant",
}

type Schema =
  | IntSchema
  | RealSchema
  | DurationSchema
  | BooleanSchema
  | StringSchema
  | SeriesSchema
  | StructSchema
  | VariantSchema;

interface BaseSchema<T extends SchemaTypes | unknown> {
  type: T;
}

type StringSchema = BaseSchema<SchemaTypes.String>;
type BooleanSchema = BaseSchema<SchemaTypes.Boolean>;
type IntSchema = BaseSchema<SchemaTypes.Int>;
type RealSchema = BaseSchema<SchemaTypes.Real>;
type DurationSchema = BaseSchema<SchemaTypes.Duration>;

interface SeriesSchema extends BaseSchema<SchemaTypes.Series> {
  items: Schema;
}

interface StructSchema extends BaseSchema<SchemaTypes.Struct> {
  items: {
    [key: string]: Schema;
  };
}

interface VariantSchema extends BaseSchema<SchemaTypes.Variant> {
  variants: {
    key: string;
    label: string;
  }[];
}

interface GraphQLActivityParameter {
  order: number;
  schema: Schema;
}

interface GraphQLActivity {
  name: string;
  parameters: GraphQLActivityParameter[];
  requiredParameters: string[];
}

export async function getActivityTypescript(
  graphqlClient: GraphQLClient,
  missionModelId: number,
  activityTypeName: string
): Promise<string> {
  console.log(`query parameters from ${activityTypeName} with missionModelId: ${missionModelId}`);
  const response = await graphqlClient.request<{
    activity_type: GraphQLActivity[];
  }>(
    gql`
      query GetParameters($missionModelId: Int!, $activityTypeName: String!) {
        activity_type(where: { model_id: { _eq: $missionModelId }, name: { _eq: $activityTypeName } }) {
          name
          parameters
        }
      }
    `,
    { missionModelId, activityTypeName }
  );

  if (response.activity_type.length === 0) {
    throw new ErrorWithStatusCode(`Activity type ${activityTypeName} not found`, 404);
  }

  const activity = response.activity_type[0];

  return generateTypescriptForGraphQLActivitySchema(activity);
}

function generateTypescriptForGraphQLActivitySchema(activitySchema: GraphQLActivity): string {
  const propertyDeclarations = Object.entries(activitySchema.parameters)
    .map(([parameterName, parameterValue]) => `readonly ${parameterName}: ${convertSchemaType(parameterValue.schema)};`)
    .join("\n");
  const activityTypeAlias = `type ActivityType = ${activitySchema.name};`;

  return globalDeclaration(`${interfaceDeclaration(activitySchema.name, propertyDeclarations)}\n${activityTypeAlias}`);
}

function convertSchemaType(schema: Schema): string {
  const objectDeclaration = (content: string) => `{\n${indent(content)}\n}`;
  switch (schema.type) {
    case SchemaTypes.Int:
    case SchemaTypes.Real:
    case SchemaTypes.Duration:
      return "number";
    case SchemaTypes.Boolean:
      return "boolean";
    case SchemaTypes.String:
      return "string";
    case SchemaTypes.Series:
      return `${convertSchemaType(schema.items)}[]`;
    case SchemaTypes.Struct:
      return objectDeclaration(
        Object.entries(schema.items)
          .map(([key, value]) => `${key}: ${convertSchemaType(value)};`)
          .join("\n")
      );
    case SchemaTypes.Variant:
      return `(${schema.variants.map((variant) => `'${variant.label}'`).join(" | ")})`;
    default:
      return "any";
  }
}
