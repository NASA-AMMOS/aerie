import jwtDecode from 'jwt-decode';

import type { AuthSessionVariables, Jwt } from '../types/jwtAuthentication';

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
