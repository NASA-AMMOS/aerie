export type AuthSessionVariables = {
  'x-hasura-role': string;
  'x-hasura-user-id': string;
};

export type Jwt = {
  camToken: string;
  'https://hasura.io/jwt/claims': AuthSessionVariables & { 'x-hasura-allowed-roles': string[] };
  username: string;
  iat: number;
  exp: number;
};
