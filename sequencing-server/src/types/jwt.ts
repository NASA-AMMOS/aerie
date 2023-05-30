import type { AuthSessionVariables } from './authSessionVariables';

export type Jwt = {
  camToken: string;
  'https://hasura.io/jwt/claims': AuthSessionVariables & { 'x-hasura-allowed-roles': string[] };
  username: string;
  iat: number;
  exp: number;
};
