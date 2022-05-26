import './polyfills.js';
import express, { Application, NextFunction, Request, Response } from 'express';
import bodyParser from 'body-parser';
import DataLoader from 'dataloader';
import { GraphQLClient } from 'graphql-request';
import * as ampcs from '@nasa-jpl/aerie-ampcs';
import getLogger from './utils/logger.js';
import { getEnv } from './env.js';
import { DbExpansion } from './db.js';
import { processDictionary } from './lib/codegen/CommandTypeCodegen.js';
import { generateTypescriptForGraphQLActivitySchema } from './lib/codegen/ActivityTypescriptCodegen.js';
import { InferredDataloader, objectCacheKeyFunction, unwrapPromiseSettledResults } from './lib/batchLoaders/index.js';
import { commandDictionaryTypescriptBatchLoader } from './lib/batchLoaders/commandDictionaryTypescriptBatchLoader.js';
import { activitySchemaBatchLoader } from './lib/batchLoaders/activitySchemaBatchLoader.js';
import { simulatedActivityInstanceBatchLoader } from './lib/batchLoaders/simulatedActivityInstanceBatchLoader.js';
import { expansionSetBatchLoader } from './lib/batchLoaders/expansionSetBatchLoader.js';
import Piscina from 'piscina';
import type { executeExpansion, typecheckExpansion } from './worker.js';
import { isRejected, isResolved } from './utils/typeguards.js';
import { mapGraphQLActivityInstance } from './lib/mapGraphQLActivityInstance.js';
import { expansionBatchLoader } from './lib/batchLoaders/expansionBatchLoader.js';
import type { UserCodeError } from '@nasa-jpl/aerie-ts-user-code-runner';
import { InheritedError } from './utils/InheritedError.js';

const logger = getLogger('app');

const PORT: number = parseInt(getEnv().PORT, 10) ?? 27184;

const app: Application = express();
// WARNING: bodyParser.json() is vulnerable to a string too long issue. Iff that occurs,
// we should switch to a stream parser like https://www.npmjs.com/package/stream-json
app.use(bodyParser.json({ limit: '25mb' }));

DbExpansion.init();
const db = DbExpansion.getDb();

const piscina = new Piscina({ filename: new URL('worker.js', import.meta.url).pathname });

type Context = {
  commandTypescriptDataLoader: InferredDataloader<typeof commandDictionaryTypescriptBatchLoader>;
  activitySchemaDataLoader: InferredDataloader<typeof activitySchemaBatchLoader>;
  simulatedActivityInstanceDataLoader: InferredDataloader<typeof simulatedActivityInstanceBatchLoader>;
  expansionSetDataLoader: InferredDataloader<typeof expansionSetBatchLoader>;
  expansionDataLoader: InferredDataloader<typeof expansionBatchLoader>;
};

app.use(async (req: Request, res: Response, next: NextFunction) => {
  const graphqlClient = new GraphQLClient(getEnv().MERLIN_GRAPHQL_URL);

  const context: Context = {
    commandTypescriptDataLoader: new DataLoader(commandDictionaryTypescriptBatchLoader({ graphqlClient }), {
      cacheKeyFn: objectCacheKeyFunction,
    }),
    activitySchemaDataLoader: new DataLoader(activitySchemaBatchLoader({ graphqlClient }), {
      cacheKeyFn: objectCacheKeyFunction,
    }),
    simulatedActivityInstanceDataLoader: new DataLoader(simulatedActivityInstanceBatchLoader({ graphqlClient }), {
      cacheKeyFn: objectCacheKeyFunction,
    }),
    expansionSetDataLoader: new DataLoader(expansionSetBatchLoader({ graphqlClient }), {
      cacheKeyFn: objectCacheKeyFunction,
    }),
    expansionDataLoader: new DataLoader(expansionBatchLoader({graphqlClient}),{
      cacheKeyFn: objectCacheKeyFunction,
    }),
  };

  res.locals.context = context;
  return next();
});

app.get('/', (req: Request, res: Response) => {
  res.send('Aerie Command Service');
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
    returning id;
  `;

  const { rows } = await db.query(sqlExpression, [
    commandDictionaryPath,
    parsedDictionary.header.mission_name,
    parsedDictionary.header.version,
  ]);

  if (rows.length < 1) {
    throw new Error(`POST /dictionary: No command dictionary was updated in the database`);
  }
  const id = rows[0].id;
  res.status(200).json({ id });
  return next();
});

app.post('/put-expansion', async (req, res, next) => {
  const context: Context = res.locals.context;

  const activityTypeName = req.body.input.activityTypeName as string;
  const expansionLogic = req.body.input.expansionLogic as string;
  const authoringCommandDictionaryId = req.body.input.authoringCommandDictionaryId as number | null;
  const authoringMissionModelId = req.body.input.authoringMissionModelId as number | null;

  const { rows } = await db.query(
    `
    INSERT INTO expansion_rule (activity_type, expansion_logic, authoring_command_dict_id, authoring_mission_model_id)
    VALUES ($1, $2, $3, $4)
    RETURNING id;
  `,
    [activityTypeName, expansionLogic, authoringCommandDictionaryId, authoringMissionModelId],
  );

  if (rows.length < 1) {
    throw new Error(`POST /put-expansion: No expansion was updated in the database`);
  }

  const id = rows[0].id;
  logger.info(`POST /put-expansion: Updated expansion in the database: id=${id}`);

  if (authoringMissionModelId == null || authoringCommandDictionaryId == null) {
    res.status(200).json({ id })
    return next();
  }

  const commandTypes = await context.commandTypescriptDataLoader.load({ dictionaryId: authoringCommandDictionaryId });
  const activitySchema = await context.activitySchemaDataLoader.load({missionModelId: authoringMissionModelId, activityTypeName });
  const activityTypescript = generateTypescriptForGraphQLActivitySchema(activitySchema);

  const result = await (piscina.run({
    expansionLogic,
    commandTypes: commandTypes,
    activityTypes: activityTypescript,
  }, { name: 'typecheckExpansion' }) as ReturnType<typeof typecheckExpansion>);

  res.status(200).json({ id, errors: result.errors });
  return next();
});

app.post('/put-expansion-set', async (req, res, next) => {
  const context: Context = res.locals.context;

  const commandDictionaryId = req.body.input.commandDictionaryId as number;
  const missionModelId = req.body.input.missionModelId as number;
  const expansionIds = req.body.input.expansionIds as number[];

  const [expansions, commandTypes] = await Promise.all([
    context.expansionDataLoader.loadMany(expansionIds.map(id => ({ expansionId: id }))),
    context.commandTypescriptDataLoader.load({ dictionaryId: commandDictionaryId }),
  ]);

  const typecheckErrorPromises = await Promise.allSettled(expansions.map(async (expansion, index) => {
    if (expansion instanceof Error) {
      throw new InheritedError(`Expansion with id: ${expansionIds[index]} could not be loaded`, expansion);
    }
    const activitySchema = await context.activitySchemaDataLoader.load({missionModelId, activityTypeName: expansion.activityType });
    const activityTypescript = generateTypescriptForGraphQLActivitySchema(activitySchema);
    const result = await (piscina.run({
      expansionLogic: expansion.expansionLogic,
      commandTypes: commandTypes,
      activityTypes: activityTypescript,
    }, { name: 'typecheckExpansion' }) as ReturnType<typeof typecheckExpansion>);

    return result.errors;
  }));

  const errors = unwrapPromiseSettledResults(typecheckErrorPromises).reduce((accum, item) => {
    if (item instanceof Error) {
      accum.push(item);
    } else {
      accum.concat(item);
    }
    return accum;
  }, [] as (Error | ReturnType<UserCodeError['toJSON']>)[]);

  if (errors.length > 0) {
    throw new InheritedError(`Expansion set could not be typechecked`, errors.map(e => ({
      name: 'TypeCheckError',
      stack: e.stack,
      // @ts-ignore  Message is not spread when it comes from an Error object because it's a getter
      message: e.message,
      ...e,
    })));
  }

  const { rows } = await db.query(`
    WITH expansion_set_id AS (
      INSERT INTO expansion_set (command_dict_id, mission_model_id)
        VALUES ($1, $2)
        RETURNING id
    ),
         rules as (
           SELECT id, activity_type FROM expansion_rule WHERE id = ANY($3::int[]) ORDER BY id
         )
    INSERT INTO expansion_set_to_rule (set_id, rule_id, activity_type)
      SELECT a.id, b.id, b.activity_type
      FROM (SELECT id from expansion_set_id) a,
           (SELECT id, activity_type from rules) b
      RETURNING (SELECT id FROM expansion_set_id);
  `,
    [commandDictionaryId, missionModelId, expansionIds],
  );

  if (rows.length < 1) {
    throw new Error(`POST /put-expansion-set: No expansion set was inserted in the database`);
  }
  const id = rows[0].id;
  logger.info(`POST /put-expansion-set: Updated expansion set in the database: id=${id}`);
  res.status(200).json({ id });
  return next();
});

app.post('/get-command-typescript', async (req, res, next) => {
  const context: Context = res.locals.context;

  const commandDictionaryId = req.body.input.commandDictionaryId as number;
  const commandTypescript = await context.commandTypescriptDataLoader.load({ dictionaryId: commandDictionaryId });

  res.status(200).json({
    typescript: commandTypescript,
  });
  return next();
});

app.post('/get-activity-typescript', async (req, res, next) => {
  const context: Context = res.locals.context;

  const missionModelId = req.body.input.missionModelId as number;
  const activityTypeName = req.body.input.activityTypeName as string;

  const activitySchema = await context.activitySchemaDataLoader.load({ missionModelId, activityTypeName });
  const activityTypescript = generateTypescriptForGraphQLActivitySchema(activitySchema);

  res.status(200).json({
    typescript: activityTypescript,
  });
  return next();
});

app.post('/expand-all-activity-instances', async (req, res, next) => {
  const context: Context = res.locals.context;

  // Query for expansion set data
  const expansionSetId = req.body.input.expansionSetId as number;
  const simulationDatasetId = req.body.input.simulationDatasetId as number;
  const [expansionSet, simulatedActivityInstances] = await Promise.all([
    context.expansionSetDataLoader.load({ expansionSetId }),
    context.simulatedActivityInstanceDataLoader.load({ simulationDatasetId }),
  ]);
  const commandTypes = expansionSet.commandDictionary.commandTypesTypeScript;

  const settledExpansionResults = await Promise.allSettled(
    simulatedActivityInstances.map(async simulatedActivityInstance => {
      const activitySchema = await context.activitySchemaDataLoader.load({
        missionModelId: expansionSet.missionModel.id,
        activityTypeName: simulatedActivityInstance.type,
      });
      const expansion = expansionSet.expansionRules.find(
        expansionRule => expansionRule.activityType === simulatedActivityInstance.type,
      );

      const activityInstance = mapGraphQLActivityInstance(simulatedActivityInstance, activitySchema);

      if (expansion === undefined) {
        return {
          activityInstance,
          commands: null,
          errors: null,
        };
      }
      const activityTypes = generateTypescriptForGraphQLActivitySchema(activitySchema);
      return (await piscina.run({
        expansionLogic: expansion.expansionLogic,
        activityInstance,
        commandTypes,
        activityTypes,
      }, { name: 'executeExpansion' })) as ReturnType<typeof executeExpansion>;
    }),
  );

  const rejectedExpansionResults = settledExpansionResults.filter(isRejected).map(p => p.reason);

  for (const expansionResult of rejectedExpansionResults) {
    logger.error(expansionResult.reason);
  }
  if (rejectedExpansionResults.length > 0) {
    throw new Error(rejectedExpansionResults.map(rejectedExpansionResult => rejectedExpansionResult.reason).join('\n'));
  }

  const expandedActivityInstances = settledExpansionResults.filter(isResolved).map(p => ({
    id: p.value.activityInstance.id,
    commands: p.value.commands,
    errors: p.value.errors,
  }));

  // Store expansion run  and activity instance commands in DB
  const { rows } = await db.query(
    `
    WITH expansion_run_id AS (
      INSERT INTO expansion_run (simulation_dataset_id, expansion_set_id)
        VALUES ($1, $2)
        RETURNING id
    )
    INSERT
    INTO activity_instance_commands (expansion_run_id,
                                     activity_instance_id,
                                     commands,
                                     errors)
    SELECT *
    FROM unnest(
        array_fill((SELECT id FROM expansion_run_id), ARRAY [array_length($3::int[], 1)]),
        $3::int[],
        $4::jsonb[],
        $5::jsonb[]
      )
    RETURNING (SELECT id FROM expansion_run_id);
    `,
    [
      simulationDatasetId,
      expansionSetId,
      expandedActivityInstances.map(result => result.id),
      expandedActivityInstances.map(result => (result.commands !== null ? JSON.stringify(result.commands) : null)),
      expandedActivityInstances.map(result => JSON.stringify(result.errors)),
    ],
  );

  if (rows.length < 1) {
    throw new Error(`POST /expand-all-activity-instances: No expansion run was inserted in the database`);
  }
  const id = rows[0].id;
  logger.info(`POST /expand-all-activity-instances: Inserted expansion run to the database: id=${id}`);

  res.status(200).json({
    id,
    expandedActivityInstances,
  });
  return next();
});

// General error handler
app.use((err: any, req: Request, res: Response, next: NextFunction) => {
  logger.error(err);
  res.status(err.status ?? err.statusCode ?? 500).send({ message: err.message });
  return next();
});

app.listen(PORT, () => {
  logger.info(`connected to port ${PORT}`);
});
