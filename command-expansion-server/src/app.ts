import './polyfills.js';
import express, { Application, NextFunction, Request, Response } from 'express';
import bodyParser from 'body-parser';
import DataLoader from 'dataloader';
import { GraphQLClient } from 'graphql-request';
import * as ampcs from '@nasa-jpl/aerie-ampcs';
import getLogger from "./utils/logger.js";
import { getEnv } from './env.js';
import { DbExpansion } from './db.js';
import { processDictionary } from './lib/codegen/CommandTypeCodegen.js';
import { generateTypescriptForGraphQLActivitySchema } from './lib/codegen/ActivityTypescriptCodegen.js';
import { InferredDataloader } from './lib/batchLoaders/index.js';
import { commandDictionaryTypescriptBatchLoader } from './lib/batchLoaders/commandDictionaryTypescriptBatchLoader.js';
import {activitySchemaBatchLoader, GraphQLActivitySchema} from './lib/batchLoaders/activitySchemaBatchLoader.js';
import { simulatedActivityInstanceBatchLoader } from './lib/batchLoaders/simulatedActivityInstanceBatchLoader.js';
import {ActivityInstance, mapGraphQLActivityInstance} from './lib/mapGraphQLActivityInstance.js';
import {expansionSetBatchLoader} from "./lib/batchLoaders/expansionSetBatchLoader.js";
import path from "path";
import Piscina from "piscina";
import {ExpansionResult} from "./worker";


const PORT: number = parseInt(getEnv().PORT, 10) ?? 3000;

const app: Application = express();
app.use(bodyParser.json({ limit: '25mb' }));

DbExpansion.init();
const db = DbExpansion.getDb();

const piscina = new Piscina({ filename: `${path.resolve()}/build/worker.js` });

type Context = {
  commandTypescriptDataLoader: InferredDataloader<typeof commandDictionaryTypescriptBatchLoader>,
  activitySchemaDataLoader: InferredDataloader<typeof activitySchemaBatchLoader>,
  simulatedActivityInstanceDataLoader: InferredDataloader<typeof simulatedActivityInstanceBatchLoader>,
  expansionSetDataLoader: InferredDataloader<typeof expansionSetBatchLoader>,
};

app.use(async(req: Request, res: Response, next: NextFunction) => {
  const graphqlClient = new GraphQLClient(getEnv().MERLIN_GRAPHQL_URL);

  const context: Context = {
    commandTypescriptDataLoader: new DataLoader(commandDictionaryTypescriptBatchLoader({graphqlClient})),
    activitySchemaDataLoader: new DataLoader(activitySchemaBatchLoader({graphqlClient})),
    simulatedActivityInstanceDataLoader: new DataLoader(simulatedActivityInstanceBatchLoader({graphqlClient})),
    expansionSetDataLoader: new DataLoader(expansionSetBatchLoader({graphqlClient})),
  };

  res.locals.context = context;
  return next();
});

const logger = getLogger("app");

app.get("/", (req: Request, res: Response) => {
  res.send("Aerie Command Service");
});

app.post('/put-dictionary', async (req, res) => {
  const base64Dictionary: string = req.body.input.dictionary;
  const dictionary = Buffer.from(base64Dictionary, "base64").toString("utf8");
  logger.info(`Dictionary received`);

  const parsedDictionary = ampcs.parse(dictionary);
  logger.info(
    `Dictionary parsed - version: ${parsedDictionary.header.version}, mission: ${parsedDictionary.header.mission_name}`
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
    logger.error(`POST /dictionary: No command dictionary was updated in the database`);
    res.status(500).send(`POST /dictionary: No command dictionary was updated in the database`);
    return;
  }
  const id = rows[0].id;
  res.status(200).json({ id });
  return;
});

app.post('/put-expansion', async (req, res) => {
  const activityTypeName = req.body.input.activityTypeName as string;
  const expansionLogicBase64 = req.body.input.expansionLogic as string;

  const { rows } = await db.query(`
    INSERT INTO expansion_rule (activity_type, expansion_logic)
    VALUES ($1, $2)
    RETURNING id;
  `, [
    activityTypeName,
    expansionLogicBase64,
  ]);

  if (rows.length < 1) {
    throw new Error(`POST /put-expansion: No expansion was updated in the database`);
  }

  const id = rows[0].id;
  logger.info(`POST /put-expansion: Updated expansion in the database: id=${id}`);
  res.status(200).json({ id });
  return;
});

app.post('/put-expansion-set', async (req, res) => {
  const commandDictionaryId = req.body.input.commandDictionaryId as number;
  const missionModelId = req.body.input.missionModelId as number;
  const expansionIds = req.body.input.expansionIds as number[];

  const { rows } = await db.query(`
    WITH expansion_set_id AS (
      INSERT INTO expansion_set (command_dict_id, mission_model_id)
        VALUES ($1, $2)
        RETURNING id
    )
    INSERT INTO expansion_set_to_rule (set_id, rule_id)
      SELECT * FROM unnest(
        array_fill((SELECT id FROM expansion_set_id), ARRAY[array_length($3::int[], 1)]),
        $3::int[]
      )
    RETURNING (SELECT id FROM expansion_set_id);
  `, [
    commandDictionaryId,
    missionModelId,
    expansionIds,
  ]);

  if (rows.length < 1) {
    throw new Error(`POST /put-expansion-set: No expansion set was inserted in the database`);
  }
  const id = rows[0].id;
  console.log(`POST /put-expansion-set: Updated expansion set in the database: id=${id}`);
  res.status(200).json({ id });
  return;
});

app.post('/get-command-typescript', async (req, res) => {
  const context: Context = res.locals.context;

  const commandDictionaryId = req.body.input.commandDictionaryId as number;
  const commandTypescript = await context.commandTypescriptDataLoader.load({dictionaryId: commandDictionaryId});
  const commandTypescriptBase64 = Buffer.from(commandTypescript).toString('base64');

  res.status(200).json({
    typescript: commandTypescriptBase64,
  });
  return;
});

app.post('/get-activity-typescript', async (req, res) => {
  const context: Context = res.locals.context;

  const missionModelId = req.body.input.missionModelId as number;
  const activityTypeName = req.body.input.activityTypeName as string;

  const activitySchema = await context.activitySchemaDataLoader.load({missionModelId, activityTypeName });
  const activityTypescript = generateTypescriptForGraphQLActivitySchema(activitySchema);
  const activityTypescriptBase64 = Buffer.from(activityTypescript).toString('base64');

  res.status(200).json({
    typescript: activityTypescriptBase64,
  });
  return;
});

app.post('/expand-all-activity-instances', async (req, res) => {
  const context: Context = res.locals.context;

  // Query for expansion set data
  console.log(`input = ${JSON.stringify(req.body.input)}`);
  const expansionSetId = req.body.input.expansionSetId as number;
  const simulationDatasetId = req.body.input.simulationDatasetId as number;
  const expansionSet = await context.expansionSetDataLoader.load({expansionSetId});
  const commandTypes = expansionSet.commandDictionary.commandTypesTypeScript;

  // Populate expansion run table, this table serves as a "ledger" for attempted expansions
  const { rows } = await db.query(`
    INSERT INTO expansion_run (simulation_dataset_id, expansion_set_id)
    VALUES ($1, $2)
    RETURNING id;
  `, [
    expansionSet.commandDictionary.id,
    expansionSet.id,
  ]);
  if (rows.length < 1) {
    throw new Error(`POST /expand-all-activity-instances: No expansion run record was inserted in the database`);
  }
  const expansionRunId = rows[0];

  // Collect and couple all activity instances and associated schemas
  let activities: { instance: ActivityInstance, schema: GraphQLActivitySchema }[];
  {
    const instances = await context.simulatedActivityInstanceDataLoader.load({simulationDatasetId});
    activities = await Promise.all(instances.map(async activityInstance => {
      const activitySchema = await context.activitySchemaDataLoader.load({
        missionModelId: expansionSet.missionModel.id,
        activityTypeName: activityInstance.type,
      });
      return {
        instance: mapGraphQLActivityInstance(activityInstance, activitySchema),
        schema: activitySchema,
      };
    }));
  }

  // 1. Map act. instances to expansions
  // 2. Create list of expansion promises
  // 3. Pass all promises to `allSettled` to get concrete expansion results
  console.log(`Expanding ${activities.length} activity instance(s)`);
  const expansionPromises: Promise<ExpansionResult>[] = activities.map(activity => {
    const activityTypeName = activity.schema.name;
    console.log(`Looking for activity type name: ${activityTypeName}`);
    expansionSet.expansionRules.forEach(expansionRule => console.log(`Finding expansion rule with activity type name: ${expansionRule.activityType}`));
    const expansion = expansionSet.expansionRules.find(expansionRule => expansionRule.activityType === activityTypeName);
    if (expansion === undefined) {
      throw new Error(`POST /expand-all-activity-instances: Activity type name ("${activityTypeName}") not found within any expansion rules associated with expansion set with id: ${expansionSetId}`);
    }
    const activityTypes = generateTypescriptForGraphQLActivitySchema(activity.schema);
    return piscina.run({
      expansionLogic: expansion.expansionLogic,
      activityInstance: activity.instance,
      commandTypes: commandTypes,
      activityTypes: activityTypes,
    });
  });

  const activityInstanceCommandsIds: number[] = [];

  for (const settledPromise of await Promise.allSettled(expansionPromises)) {
    if (settledPromise.status === 'fulfilled') {
      const { activityInstance, commands, errors } = (settledPromise as PromiseFulfilledResult<ExpansionResult>).value;
      if (commands.length < 1) {
        // TODO should we fail-fast or aggregate all errors?
        return res.json({ errors: errors.map(error => error.message) });
      }

      // Populate activity instance commands table
      console.log(`commands = ${JSON.stringify(commands)}`);
      const { rows } = await db.query(`
        INSERT INTO activity_instance_commands (activity_instance_id, commands, expansion_run_id)
        VALUES ($1, $2, $3)
        RETURNING id;
      `, [
        activityInstance.id,
        commands,
        expansionRunId,
      ]);
      if (rows.length < 1) {
        throw new Error(`POST /expand-all-activity-instances: No activity instance commands were inserted in the database`);
      }
      activityInstanceCommandsIds.push(rows[0].id);
    }
    else if (settledPromise.status === 'rejected') {
      const reason = (settledPromise as PromiseRejectedResult).reason;
      throw new Error(`POST /expand-all-activity-instances: Unexpected error: ${reason}`);
    }
  }

  // Return expansion run ID and all associated activity instance commands IDs
  return res.json({
    expansionRunId: expansionRunId,
    activityInstanceCommandsIds: activityInstanceCommandsIds
  });
});

// General error handler
app.use((err: any, req: Request, res: Response, _next: NextFunction) => {
  logger.error(err);
  res.status(err.status ?? err.statusCode ?? 500).send(err.message);
  return next();
});

app.listen(PORT, () => {
  logger.info(`connected to port ${PORT}`);
});
