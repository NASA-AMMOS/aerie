import FormData from 'form-data';
import fs from 'fs';
import fetch from 'node-fetch';
import { gql, GraphQLClient } from 'graphql-request';
import { randomUUID } from 'crypto';

export async function uploadMissionModel(graphqlClient: GraphQLClient): Promise<number> {
  /*
   * Insert a mission model
   */
  const bananaNationMissionModelUrl = new URL('../../../examples/banananation/build/libs/banananation-0.10.0-SNAPSHOT.jar', import.meta.url);

  const formData = new FormData();
  formData.append('file', fs.createReadStream(bananaNationMissionModelUrl.pathname));

  const uploadRes = await fetch(`${process.env.MERLIN_GATEWAY_URL}/file`, {
    method: 'POST',
    body: formData,
    headers: { 'x-auth-sso-token': process.env.SSO_TOKEN as string },
  });
  if (!uploadRes.ok) {
    throw new Error(`Failed to upload mission model: ${uploadRes.statusText}`);
  }

  const missionModelFileId = ((await uploadRes.json()) as {id: number}).id;

  const res = await graphqlClient.request(gql`
    mutation InsertTestMissionModel($model: mission_model_insert_input!) {
      insert_mission_model_one(object: $model) {
        id
      }
    }
  `, {
    model: {
      mission: 'banananation',
      name: 'banananation' + randomUUID(),
      version: '0.0.0',
      jar_id: missionModelFileId,
    }
  });
  await new Promise((resolve) => setTimeout(resolve, 3000));
  return ((res.insert_mission_model_one as {id: number}) as {id: number}).id;
}

export async function removeMissionModel(graphqlClient: GraphQLClient, missionModelId: number): Promise<void> {
  /*
   * Remove a mission model
   */
  await graphqlClient.request(gql`
    mutation DeleteMissionModel($missionModelId: Int!) {
      deleteModel: delete_mission_model_by_pk(id: $missionModelId) {
        id
      }
    }
  `, {
    missionModelId,
  });
}
