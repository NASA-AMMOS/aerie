import {GraphQLClient} from 'graphql-request';

export async function getActivityTypes(graphqlClient: GraphQLClient, missionModelId: number, activityTypeName: string): Promise<string> {
  // Stubbed out here
  const activityTypes = `
    declare global {
      type Activity = {
        radioUnit: 'RADIO_PRIME' | 'RADIO_BACKUP' | 'RADIO_A' | 'RADIO_B',
      }
    }
  `;

  return activityTypes;
}
