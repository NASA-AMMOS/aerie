export type Env = {
  HASURA_GRAPHQL_ADMIN_SECRET: string;
  LOG_FILE: string;
  LOG_LEVEL: string;
  MERLIN_GRAPHQL_URL: string;
  PORT: string;
  POSTGRES_AERIE_SEQUENCING_DB: string;
  POSTGRES_HOST: string;
  POSTGRES_PASSWORD: string;
  POSTGRES_PORT: string;
  POSTGRES_USER: string;
  STORAGE: string;
};

export const defaultEnv: Env = {
  HASURA_GRAPHQL_ADMIN_SECRET: '',
  LOG_FILE: 'console',
  LOG_LEVEL: 'info',
  MERLIN_GRAPHQL_URL: 'http://hasura:8080/v1/graphql',
  PORT: '27184',
  POSTGRES_AERIE_SEQUENCING_DB: 'aerie_sequencing',
  POSTGRES_HOST: 'localhost',
  POSTGRES_PASSWORD: '',
  POSTGRES_PORT: '5432',
  POSTGRES_USER: '',
  STORAGE: 'sequencing_file_store',
};

export function getEnv(): Env {
  const { env } = process;

  const HASURA_GRAPHQL_ADMIN_SECRET = env['HASURA_GRAPHQL_ADMIN_SECRET'] ?? defaultEnv.HASURA_GRAPHQL_ADMIN_SECRET;
  const LOG_FILE = env['LOG_FILE'] ?? defaultEnv.LOG_FILE;
  const LOG_LEVEL = env['LOG_LEVEL'] ?? defaultEnv.LOG_LEVEL;
  const MERLIN_GRAPHQL_URL = env['MERLIN_GRAPHQL_URL'] ?? defaultEnv.MERLIN_GRAPHQL_URL;
  const PORT = env['SEQUENCING_SERVER_PORT'] ?? defaultEnv.PORT;
  const POSTGRES_AERIE_SEQUENCING_DB = env['SEQUENCING_DB'] ?? defaultEnv.POSTGRES_AERIE_SEQUENCING_DB;
  const POSTGRES_HOST = env['SEQUENCING_DB_SERVER'] ?? defaultEnv.POSTGRES_HOST;
  const POSTGRES_PASSWORD = env['SEQUENCING_DB_PASSWORD'] ?? defaultEnv.POSTGRES_PASSWORD;
  const POSTGRES_PORT = env['SEQUENCING_DB_PORT'] ?? defaultEnv.POSTGRES_PORT;
  const POSTGRES_USER = env['SEQUENCING_DB_USER'] ?? defaultEnv.POSTGRES_USER;
  const STORAGE = env['SEQUENCING_LOCAL_STORE'] ?? defaultEnv.STORAGE;
  return {
    HASURA_GRAPHQL_ADMIN_SECRET,
    LOG_FILE,
    LOG_LEVEL,
    MERLIN_GRAPHQL_URL,
    PORT,
    POSTGRES_AERIE_SEQUENCING_DB,
    POSTGRES_HOST,
    POSTGRES_PASSWORD,
    POSTGRES_PORT,
    POSTGRES_USER,
    STORAGE,
  };
}
