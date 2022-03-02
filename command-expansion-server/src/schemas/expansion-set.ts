import {JSONSchemaType} from "ajv";

export const expansionSetSchema: JSONSchemaType<{
  commandDictionaryId: string,
  missionModelId: string,
  expansionIds: string[],
}> = {
  type: 'object',
  properties: {
    commandDictionaryId: {
      type: 'string',
    },
    missionModelId: {
      type: 'string',
    },
    expansionIds: {
      type: 'array',
      items: {
        type: 'string',
      },
      minItems: 1,
    },
  },
  required: ['commandDictionaryId', 'expansionIds', 'missionModelId'],
  additionalProperties: false,
};
