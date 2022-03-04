import {JSONSchemaType} from "ajv";

export const expansionSetSchema: JSONSchemaType<{
  commandDictionaryId: number,
  missionModelId: number,
  expansionIds: number[],
}> = {
  type: 'object',
  properties: {
    commandDictionaryId: {
      type: 'integer',
    },
    missionModelId: {
      type: 'integer',
    },
    expansionIds: {
      type: 'array',
      items: {
        type: 'integer',
      },
      minItems: 1,
    },
  },
  required: ['commandDictionaryId', 'expansionIds', 'missionModelId'],
  additionalProperties: false,
};
