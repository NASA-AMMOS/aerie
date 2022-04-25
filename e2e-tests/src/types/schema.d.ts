type ValueSchemaBoolean = {
  type: 'boolean';
};

type ValueSchemaDuration = {
  type: 'duration';
};

type ValueSchemaInt = {
  type: 'int';
};

type ValueSchemaPath = {
  type: 'path';
};

type ValueSchemaReal = {
  type: 'real';
};

type ValueSchemaSeries = {
  type: 'series';
  items: ValueSchema;
};

type ValueSchemaString = {
  type: 'string';
};

type ValueSchemaStruct = {
  items: Record<string, ValueSchema>;
  type: 'struct';
};

type ValueSchemaVariant = {
  variants: Variant[];
  type: 'variant';
};

type ValueSchema =
  | ValueSchemaBoolean
  | ValueSchemaDuration
  | ValueSchemaInt
  | ValueSchemaPath
  | ValueSchemaReal
  | ValueSchemaSeries
  | ValueSchemaString
  | ValueSchemaStruct
  | ValueSchemaVariant;

type Variant = {
  key: string;
  label: string;
};
