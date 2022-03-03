import util from 'util';

import { default as pg, Pool, PoolConfig} from 'pg';
import { getEnv } from '../env.js';

const { Pool: DbPool } = pg;

const {
  POSTGRES_AERIE_EXPANSION_DB,
  POSTGRES_HOST: host,
  POSTGRES_PASSWORD: password,
  POSTGRES_PORT: port,
  POSTGRES_USER: user,
} = getEnv();

export class DbExpansion {
  private static pool: Pool;

  static getDb(): Pool {
    return DbExpansion.pool;
  }

  static init() {
    try {
      const config: PoolConfig = {
        database: POSTGRES_AERIE_EXPANSION_DB,
        host,
        password,
        port: parseInt(port, 10),
        user,
      };

      console.log(`Postgres Config:\n${util.formatWithOptions({depth: Infinity}, config)}`);

      DbExpansion.pool = new DbPool(config);
    } catch (error) {
      console.log(error);
    }
  }
}
