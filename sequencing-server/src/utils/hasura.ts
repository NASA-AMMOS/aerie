import jwtDecode from 'jwt-decode';

import type { AuthSessionVariables, Jwt } from '../types/jwtAuthentication';
import { gql, type GraphQLClient } from 'graphql-request';

/**
 * Mapping of Sequencing endpoints to their DB action check. Any new endpoints need
 * to be evaulated and added here if they need fine-grained permissions.
 */
const ENDPOINTS_TO_ACTION_KEY: Record<string, string> = {
  '/put-dictionary': 'insert_command_dict',
  '/command-expansion/put-expansion-set': 'create_expansion_set',
  '/command-expansion/put-expansion': 'create_expansion_rule',
  '/command-expansion/expand-all-activity-instances': 'expand_all_activities',
  '/seqjson/get-seqjson-for-seqid-and-simulation-dataset': 'sequence_seq_json',
  '/seqjson/bulk-get-seqjson-for-seqid-and-simulation-dataset': 'sequence_seq_json_bulk',
  '/seqjson/get-seqjson-for-sequence-standalone': 'user_sequence_seq_json',
  '/seqjson/bulk-get-seqjson-for-sequence-standalone': 'user_sequence_seq_json_bulk',
  '/get-command-typescript': 'get_command_dict_ts',
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
 * by a user with enough permissions. To check this we need to call the db function
 * `metadata.get_action_permissions()` with the action key and the user session.
 *
 * Some endpoints don't need to be checked which is why we have the mappings of endpoints
 * to actions to check.
 *
 * @param url The endpoint that we're checking the permissions for.
 * @param db A handle to the db so we can execute the permission check.
 * @returns True if the user is able to call the given action, false otherwise.
 */
export async function canUserPerformAction(
  url: string,
  graphqlClient: GraphQLClient,
  hasuraSession: AuthSessionVariables,
): Promise<boolean> {
  if (url in ENDPOINTS_TO_ACTION_KEY) {
    const permissionCheckQuery = await graphqlClient.request(
      gql`
        query checkUserPermissions($role: user_roles_enum!, $key: String!) {
          user_role_permission_by_pk(role: $role) {
            action_permissions(path: $key)
          }
        }
      `,
      {
        role: hasuraSession['x-hasura-role'],
        key: ENDPOINTS_TO_ACTION_KEY[url],
      },
    );

    if (permissionCheckQuery.user_role_permission_by_pk.action_permissions === null) {
      return false;
    }
  }

  return true;
}
