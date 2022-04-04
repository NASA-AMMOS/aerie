import type { APIRequestContext } from '@playwright/test';
import { sync as glob } from 'fast-glob';
import { createReadStream } from 'fs';
import { basename, resolve } from 'path';
import { GATEWAY_URL, HASURA_URL, UI_URL } from '../utilities/urls';
import gql from './gql';

/**
 * Aerie API request functions.
 */
const req = {
  async createMissionModel(request: APIRequestContext, model: MissionModelInsertInput): Promise<number> {
    const data = await req.hasura(request, gql.CREATE_MISSION_MODEL, { model });
    const { insert_mission_model_one } = data;
    const { id: mission_model_id } = insert_mission_model_one;

    return mission_model_id;
  },

  async deleteMissionModel(request: APIRequestContext, id: number): Promise<number> {
    const data = await req.hasura(request, gql.DELETE_MISSION_MODEL, { id });
    const { delete_mission_model_by_pk } = data;
    const { id: deleted_mission_model_id } = delete_mission_model_by_pk;

    return deleted_mission_model_id;
  },

  async hasura<T = any>(
    request: APIRequestContext,
    query: string,
    variables: Record<string, unknown> = {},
  ): Promise<T> {
    const options = { data: { query, variables } };
    const response = await request.post(`${HASURA_URL}/v1/graphql`, options);

    if (response.ok()) {
      const json = await response.json();

      if (json?.data) {
        const { data } = json;
        return data as T;
      } else if (json?.errors) {
        console.error(json.errors);
        const [{ message }] = json.errors;
        throw new Error(message);
      } else {
        throw new Error('An unexpected error ocurred');
      }
    } else {
      throw new Error(response.statusText());
    }
  },

  async healthGateway(request: APIRequestContext): Promise<boolean> {
    const response = await request.get(`${GATEWAY_URL}/health`);
    return response.ok();
  },

  async healthHasura(request: APIRequestContext): Promise<boolean> {
    const response = await request.get(`${HASURA_URL}/healthz`);
    return response.ok();
  },

  async healthUI(request: APIRequestContext): Promise<boolean> {
    const response = await request.get(`${UI_URL}/health`);
    return response.ok();
  },

  /**
   * @param searchPath is relative to the e2e-tests directory. Defaults to Banananation.
   */
  async uploadJarFile(
    request: APIRequestContext,
    searchPath: string = '../examples/banananation/build/libs/*',
  ): Promise<number> {
    const absoluteSearchPath = resolve(searchPath);
    const [jarPath = 'ERROR_JAR_NOT_FOUND'] = glob(absoluteSearchPath);

    const buffer = createReadStream(jarPath);
    const name = basename(jarPath);
    const mimeType = 'application/java-archive';
    const multipart = { buffer, mimeType, name };
    const response = await request.post(`${GATEWAY_URL}/file`, { multipart });

    if (response.ok()) {
      const json = await response.json();

      if (json?.id !== undefined) {
        const { id: jar_id } = json;
        return jar_id;
      } else if (json?.success === false) {
        console.error(json);
        throw new Error(json.message);
      } else {
        throw new Error('An unexpected error ocurred');
      }
    } else {
      throw new Error(response.statusText());
    }
  },
};

export default req;
