import * as ampcs from '@nasa-jpl/aerie-ampcs';
import bodyParser from 'body-parser';
import DataLoader from 'dataloader';
import express, { Application, NextFunction, Request, Response } from 'express';
import { GraphQLClient } from 'graphql-request';
import fs from 'node:fs';
import Piscina from 'piscina';
import { Status } from './common.js';
import { DbExpansion } from './db.js';
import { getEnv } from './env.js';
import { activitySchemaBatchLoader } from './lib/batchLoaders/activitySchemaBatchLoader.js';
import { commandDictionaryTypescriptBatchLoader } from './lib/batchLoaders/commandDictionaryTypescriptBatchLoader.js';
import { expansionBatchLoader } from './lib/batchLoaders/expansionBatchLoader.js';
import { expansionSetBatchLoader } from './lib/batchLoaders/expansionSetBatchLoader.js';
import { InferredDataloader, objectCacheKeyFunction } from './lib/batchLoaders/index.js';
import {
  simulatedActivitiesBatchLoader,
  simulatedActivityInstanceBySimulatedActivityIdBatchLoader,
} from './lib/batchLoaders/simulatedActivityBatchLoader.js';
import { generateTypescriptForGraphQLActivitySchema } from './lib/codegen/ActivityTypescriptCodegen.js';
import { processDictionary } from './lib/codegen/CommandTypeCodegen.js';
import './polyfills.js';
import getLogger from './utils/logger.js';
import { commandExpansionRouter } from './routes/command-expansion.js';
import { seqjsonRouter } from './routes/seqjson.js';

const logger = getLogger('app');

const PORT: number = parseInt(getEnv().PORT, 10) ?? 27184;

const app: Application = express();
// WARNING: bodyParser.json() is vulnerable to a string too long issue. Iff that occurs,
// we should switch to a stream parser like https://www.npmjs.com/package/stream-json
app.use(bodyParser.json({ limit: '100mb' }));

DbExpansion.init();
export const db = DbExpansion.getDb();

export const piscina = new Piscina({ filename: new URL('worker.js', import.meta.url).pathname });
const temporalPolyfillTypes = fs.readFileSync(new URL('TemporalPolyfillTypes.ts', import.meta.url).pathname, 'utf-8');

export type Context = {
  commandTypescriptDataLoader: InferredDataloader<typeof commandDictionaryTypescriptBatchLoader>;
  activitySchemaDataLoader: InferredDataloader<typeof activitySchemaBatchLoader>;
  simulatedActivitiesDataLoader: InferredDataloader<typeof simulatedActivitiesBatchLoader>;
  simulatedActivityInstanceBySimulatedActivityIdDataLoader: InferredDataloader<
    typeof simulatedActivityInstanceBySimulatedActivityIdBatchLoader
  >;
  expansionSetDataLoader: InferredDataloader<typeof expansionSetBatchLoader>;
  expansionDataLoader: InferredDataloader<typeof expansionBatchLoader>;
};

app.use(async (_: Request, res: Response, next: NextFunction) => {
  const graphqlClient = new GraphQLClient(getEnv().MERLIN_GRAPHQL_URL);

  const activitySchemaDataLoader = new DataLoader(activitySchemaBatchLoader({ graphqlClient }), {
    cacheKeyFn: objectCacheKeyFunction,
    name: null,
  });

  res.locals['context'] = {
    commandTypescriptDataLoader: new DataLoader(commandDictionaryTypescriptBatchLoader({ graphqlClient }), {
      cacheKeyFn: objectCacheKeyFunction,
      name: null,
    }),
    activitySchemaDataLoader,
    simulatedActivitiesDataLoader: new DataLoader(
      simulatedActivitiesBatchLoader({
        graphqlClient,
        activitySchemaDataLoader,
      }),
      {
        cacheKeyFn: objectCacheKeyFunction,
        name: null,
      },
    ),
    simulatedActivityInstanceBySimulatedActivityIdDataLoader: new DataLoader(
      simulatedActivityInstanceBySimulatedActivityIdBatchLoader({
        graphqlClient,
        activitySchemaDataLoader,
      }),
      {
        cacheKeyFn: objectCacheKeyFunction,
        name: null,
      },
    ),
    expansionSetDataLoader: new DataLoader(expansionSetBatchLoader({ graphqlClient }), {
      cacheKeyFn: objectCacheKeyFunction,
      name: null,
    }),
    expansionDataLoader: new DataLoader(expansionBatchLoader({ graphqlClient }), {
      cacheKeyFn: objectCacheKeyFunction,
      name: null,
    }),
  } as Context;
  return next();
});

app.use('/command-expansion', commandExpansionRouter);
app.use('/seqjson', seqjsonRouter);

app.get('/', (_: Request, res: Response) => {
  res.send('Aerie Command Service');
});

app.get('/health', (_: Request, res: Response) => {
  res.status(200).send();
});

app.post('/put-dictionary', async (req, res, next) => {
  const dictionary = req.body.input.dictionary as string;
  logger.info(`Dictionary received`);

  const parsedDictionary = ampcs.parse(dictionary);
  logger.info(
    `Dictionary parsed - version: ${parsedDictionary.header.version}, mission: ${parsedDictionary.header.mission_name}`,
  );

  const commandDictionaryPath = await processDictionary(parsedDictionary);
  logger.info(`command-lib generated - path: ${commandDictionaryPath}`);

  const sqlExpression = `
    insert into command_dictionary (command_types_typescript_path, mission, version)
    values ($1, $2, $3)
    on conflict (mission, version) do update
      set command_types_typescript_path = $1
    returning id, command_types_typescript_path, mission, version, created_at, updated_at;
  `;

  const { rows } = await db.query(sqlExpression, [
    commandDictionaryPath,
    parsedDictionary.header.mission_name,
    parsedDictionary.header.version,
  ]);

  if (rows.length < 1) {
    throw new Error(`POST /dictionary: No command dictionary was updated in the database`);
  }
  const [row] = rows;
  res.status(200).json(row);
  return next();
});

app.post('/get-command-typescript', async (req, res, next) => {
  const context: Context = res.locals['context'];

  const commandDictionaryId = req.body.input.commandDictionaryId as number;

  try {
    const commandTypescript = await context.commandTypescriptDataLoader.load({ dictionaryId: commandDictionaryId });

    res.status(200).json({
      status: Status.SUCCESS,
      typescriptFiles: [
        {
          filePath: 'command-types.ts',
          content: commandTypescript,
        },
        {
          filePath: 'TemporalPolyfillTypes.ts',
          content: temporalPolyfillTypes,
        },
      ],
      reason: null,
    });
  } catch (e) {
    res.status(200).json({
      status: Status.FAILURE,
      typescriptFiles: null,
      reason: (e as Error).message,
    });
  }
  return next();
});

app.post('/get-activity-typescript', async (req, res, next) => {
  const context: Context = res.locals['context'];

  const missionModelId = req.body.input.missionModelId as number;
  const activityTypeName = req.body.input.activityTypeName as string;

  const activitySchema = await context.activitySchemaDataLoader.load({ missionModelId, activityTypeName });
  const activityTypescript = generateTypescriptForGraphQLActivitySchema(activitySchema);

  res.status(200).json({
    status: Status.SUCCESS,
    typescriptFiles: [
      {
        filePath: 'activity-types.ts',
        content: activityTypescript,
      },
      {
        filePath: 'TemporalPolyfillTypes.ts',
        content: temporalPolyfillTypes,
      },
    ],
    reason: null,
  });
  return next();
});

// General error handler
app.use((err: any, _: Request, res: Response, next: NextFunction) => {
  logger.error(err);

  res.status(err.status ?? err.statusCode ?? 500).send({
    message: err.message,
    extensions: {
      ...(err.cause ? { cause: err.cause } : {}),
      ...(err.stack ? { stack: err.stack } : {}),
      object: err,
    },
  });
  return next();
});

app.listen(PORT, () => {
  logger.info(`connected to port ${PORT}`);
});
