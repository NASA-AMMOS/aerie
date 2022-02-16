export type Env = {
  PORT: string;
  POSTGRES_AERIE_EXPANSION_DB: string;
  POSTGRES_HOST: string;
  POSTGRES_PASSWORD: string;
  POSTGRES_PORT: string;
  POSTGRES_USER: string;
};

export const defaultEnv: Env = {
  PORT: "3000",
  POSTGRES_AERIE_EXPANSION_DB: "aerie_expansion",
  POSTGRES_HOST: "localhost",
  POSTGRES_PASSWORD: "aerie",
  POSTGRES_PORT: "5432",
  POSTGRES_USER: "aerie",
};

export function getEnv(): Env {
  const { env } = process;

  const PORT = env["COMMANDING_SERVER_PORT"] ?? defaultEnv.PORT;
  const POSTGRES_AERIE_EXPANSION_DB = env["COMMANDING_DB"] ?? defaultEnv.POSTGRES_AERIE_EXPANSION_DB;
  const POSTGRES_HOST = env["POSTGRES_HOST"] ?? defaultEnv.POSTGRES_HOST;
  const POSTGRES_PASSWORD = env["COMMANDING_DB_PASSWORD"] ?? defaultEnv.POSTGRES_PASSWORD;
  const POSTGRES_PORT = env["COMMANDING_DB_PORT"] ?? defaultEnv.POSTGRES_PORT;
  const POSTGRES_USER = env["COMMANDING_DB_USER"] ?? defaultEnv.POSTGRES_USER;

  return {
    PORT,
    POSTGRES_AERIE_EXPANSION_DB,
    POSTGRES_HOST,
    POSTGRES_PASSWORD,
    POSTGRES_PORT,
    POSTGRES_USER,
  };
}
