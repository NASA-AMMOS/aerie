export type Env = {
  LOG_FILE: string;
  LOG_LEVEL: string;
  MERLIN_GRAPHQL_URL: string;
  PORT: string;
  POSTGRES_AERIE_EXPANSION_DB: string;
  POSTGRES_HOST: string;
  POSTGRES_PASSWORD: string;
  POSTGRES_PORT: string;
  POSTGRES_USER: string;
  STORAGE: string;
};

export const defaultEnv: Env = {
  LOG_FILE: 'console',
  LOG_LEVEL: 'info',
  MERLIN_GRAPHQL_URL: 'http://hasura:8080/v1/graphql',
  PORT: "3000",
  POSTGRES_AERIE_EXPANSION_DB: "aerie_commanding",
  POSTGRES_HOST: "localhost",
  POSTGRES_PASSWORD: "aerie",
  POSTGRES_PORT: "5432",
  POSTGRES_USER: "aerie",
  STORAGE: "commanding_file_store",
};

export function getEnv(): Env {
  const { env } = process;

  const LOG_FILE = env['LOG_FILE'] ?? defaultEnv.LOG_FILE;
  const LOG_LEVEL = env['LOG_LEVEL'] ?? defaultEnv.LOG_LEVEL;
  const MERLIN_GRAPHQL_URL = env.MERLIN_GRAPHQL_URL ?? defaultEnv.MERLIN_GRAPHQL_URL;
  const PORT = env.COMMANDING_SERVER_PORT ?? defaultEnv.PORT;
  const POSTGRES_AERIE_EXPANSION_DB = env.COMMANDING_DB ?? defaultEnv.POSTGRES_AERIE_EXPANSION_DB;
  const POSTGRES_HOST = env.COMMANDING_DB_SERVER ?? defaultEnv.POSTGRES_HOST;
  const POSTGRES_PASSWORD = env.COMMANDING_DB_PASSWORD ?? defaultEnv.POSTGRES_PASSWORD;
  const POSTGRES_PORT = env.COMMANDING_DB_PORT ?? defaultEnv.POSTGRES_PORT;
  const POSTGRES_USER = env.COMMANDING_DB_USER ?? defaultEnv.POSTGRES_USER;
  const STORAGE = env.COMMANDING_LOCAL_STORE ?? defaultEnv.STORAGE;
  return {
    LOG_FILE,
    LOG_LEVEL,
    MERLIN_GRAPHQL_URL,
    PORT,
    POSTGRES_AERIE_EXPANSION_DB,
    POSTGRES_HOST,
    POSTGRES_PASSWORD,
    POSTGRES_PORT,
    POSTGRES_USER,
    STORAGE,
  };
}
