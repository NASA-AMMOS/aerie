import jwtDecode from 'jwt-decode';

import type { AuthSessionVariables, Jwt } from '../types/jwtAuthentication';
import { gql, type GraphQLClient } from 'graphql-request';

enum HasuraPermissions {
  NO_CHECK = 'NO_CHECK',
  MISSION_MODEL_OWNER = 'MISSION_MODEL_OWNER',
  OWNER = 'OWNER',
  PLAN_OWNER = 'PLAN_OWNER',
  PLAN_COLLABORATOR = 'PLAN_COLLABORATOR',
  PLAN_OWNER_COLLABORATOR = 'PLAN_OWNER_COLLABORATOR',
}

/**
 * Endpoints that don't need any permission checking.
 */
export const ENDPOINTS_WHITELIST = new Set([
  '/health',
  '/get-command-typescript',
  '/get-activity-typescript',
  '/put-dictionary',
  '/seqjson/bulk-get-seqjson-for-sequence-standalone',
  '/seqjson/bulk-get-seqjson-for-seqid-and-simulation-dataset',
  '/seqjson/get-seqjson-for-sequence-standalone',
  '/seqjson/get-seqjson-for-seqid-and-simulation-dataset',
]);

/**
 * Mapping of Sequencing endpoints to their DB action check. Any new endpoints need
 * to be evaulated and added here if they need fine-grained permissions.
 */
const ENDPOINTS_TO_ACTION_KEY: Record<string, string> = {
  '/command-expansion/put-expansion-set': 'create_expansion_set',
  '/command-expansion/put-expansion': 'create_expansion_rule',
  '/command-expansion/expand-all-activity-instances': 'expand_all_activities',
};

/**
 * A helper function to get the Hasura session from the user who sent the request.
 *
 * @param authSessionVariables If the request has gone through a hasura action this object will be populated.
 * @param authHeader If the user sends a POST request directly to the endpoint with a bearer token this will be populated.
 * @returns The AuthSessionVeriables of the user sending the request.
 */
export function getHasuraSession(
  authSessionVariables: AuthSessionVariables | undefined,
  authHeader: string | undefined,
): AuthSessionVariables {
  if (authSessionVariables !== undefined) {
    return authSessionVariables;
  } else if (authHeader !== undefined) {
    return (jwtDecode(authHeader) as Jwt)['https://hasura.io/jwt/claims'];
  }

  throw new Error('Could not determine the user sending the request');
}

/**
 * A helper function to get the username of the user who sent the request.
 *
 * @param authSessionVariables If the request has gone through a hasura action this object will be populated.
 * @param authHeader If the user sends a POST request directly to the endpoint with a bearer token this will be populated.
 * @returns The username of the user sending the request.
 */
export function getUsername(
  authSessionVariables: AuthSessionVariables | undefined,
  authHeader: string | undefined,
): string {
  if (authSessionVariables !== undefined) {
    return authSessionVariables['x-hasura-user-id'];
  } else if (authHeader !== undefined) {
    return (jwtDecode(authHeader) as Jwt).username;
  }

  throw new Error('Could not determine the user sending the request');
}

/**
 * This function checks to see if request coming from the Hasura action is being executed
 * by a user with enough permissions. To check this we need to call a GQL query with the
 * action key and the user session. If we ever merge the databases this should be changed
 * to call the db function that does the permission checking directly.
 *
 * Some endpoints don't need to be checked which is why we have the mappings of endpoints
 * to actions to check.
 *
 * @param url The endpoint that we're checking the permissions for.
 * @param graphqlClient The GQL Client we're using to make the permission check.
 * @returns True if the user is able to call the given action, false otherwise.
 */
export async function canUserPerformAction(
  url: string,
  graphqlClient: GraphQLClient,
  hasuraSession: AuthSessionVariables,
  body: any,
): Promise<boolean> {
  const role = hasuraSession['x-hasura-role'];
  const user = hasuraSession['x-hasura-user-id'];

  // The aerie_admin role always has NO_CHECK permissions on all actions.
  if (role === 'aerie_admin') {
    return true;
  }

  const permissionCheckQuery = await graphqlClient.request(
    gql`
      query checkUserPermissions($role: user_roles_enum!, $key: String!) {
        user_role_permission_by_pk(role: $role) {
          action_permissions(path: $key)
        }
      }
    `,
    {
      role,
      key: ENDPOINTS_TO_ACTION_KEY[url],
    },
  );

  const permission = permissionCheckQuery.user_role_permission_by_pk.action_permissions as HasuraPermissions | null;

  if (permission === null) {
    return false;
  }

  const missionModelId = body.input.missionModelId as number;
  const planId = body.input.planId as number;

  switch (permission) {
    case HasuraPermissions.NO_CHECK:
      return true;
    case HasuraPermissions.MISSION_MODEL_OWNER:
      return missionModelId !== null && (await isMissionModelOwner(graphqlClient, missionModelId, user));
    case HasuraPermissions.OWNER:
      return planId !== null && (await isPlanOwner(graphqlClient, planId, user));
    case HasuraPermissions.PLAN_OWNER:
      return planId !== null && (await isPlanOwner(graphqlClient, planId, user));
    case HasuraPermissions.PLAN_COLLABORATOR:
      return planId !== null && (await isPlanCollaborator(graphqlClient, planId, user));
    case HasuraPermissions.PLAN_OWNER_COLLABORATOR:
      return (
        (planId !== null && (await isPlanOwner(graphqlClient, planId, user))) ||
        (await isPlanCollaborator(graphqlClient, planId, user))
      );
  }
}

async function isMissionModelOwner(
  graphqlClient: GraphQLClient,
  missionModelId: number,
  user: string,
): Promise<boolean> {
  const missionModelOwner = await graphqlClient.request<{
    mission_model: { owner: string }[];
  }>(
    gql`
      query missionModelOwner($missionModelId: Int!) {
        mission_model(where: { id: { _eq: $missionModelId } }) {
          owner
        }
      }
    `,
    { missionModelId },
  );

  return user === missionModelOwner.mission_model[0]?.owner;
}

async function isPlanOwner(graphqlClient: GraphQLClient, planId: number, user: string): Promise<boolean> {
  const planOwner = await graphqlClient.request<{
    plan: { owner: string }[];
  }>(
    gql`
      query planOwner($planId: Int!) {
        plan(where: { id: { _eq: $planId } }) {
          owner
        }
      }
    `,
    { planId },
  );

  return user === planOwner.plan[0]?.owner;
}

async function isPlanCollaborator(graphqlClient: GraphQLClient, planId: number, user: string): Promise<boolean> {
  const planCollaboratorRes = await graphqlClient.request<{
    plan_collaborators: { collaborator: string }[];
  }>(
    gql`
      query planCollaborator($planId: Int!) {
        plan_collaborators(where: { id: { _eq: $planId } }) {
          collaborator
        }
      }
    `,
    { planId },
  );

  for (const planCollaborator of planCollaboratorRes.plan_collaborators) {
    if (user === planCollaborator.collaborator) {
      return true;
    }
  }

  return false;
}
