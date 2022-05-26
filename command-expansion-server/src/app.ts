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
import {
  SimulatedActivity,
  simulatedActivitiesBatchLoader,
  simulatedActivityInstanceBySimulatedActivityIdBatchLoader,
} from './lib/batchLoaders/simulatedActivityBatchLoader.js';
import { expansionSetBatchLoader } from './lib/batchLoaders/expansionSetBatchLoader.js';
import Piscina from 'piscina';
import type { executeExpansion, typecheckExpansion } from './worker.js';
import { isRejected, isResolved } from './utils/typeguards.js';
import { expansionBatchLoader } from './lib/batchLoaders/expansionBatchLoader.js';
import type { UserCodeError } from '@nasa-jpl/aerie-ts-user-code-runner';
import { InheritedError } from './utils/InheritedError.js';
import { defaultSeqBuilder } from './defaultSeqBuilder.js';
import { CommandSeqJson, Command, Sequence } from './lib/codegen/CommandEDSLPreface.js';
import { assertOne } from './utils/assertions.js';

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
  simulatedActivitiesDataLoader: InferredDataloader<typeof simulatedActivitiesBatchLoader>;
  simulatedActivityInstanceBySimulatedActivityIdDataLoader: InferredDataloader<
    typeof simulatedActivityInstanceBySimulatedActivityIdBatchLoader
  >;
  expansionSetDataLoader: InferredDataloader<typeof expansionSetBatchLoader>;
  expansionDataLoader: InferredDataloader<typeof expansionBatchLoader>;
};

app.use(async (req: Request, res: Response, next: NextFunction) => {
  const graphqlClient = new GraphQLClient(getEnv().MERLIN_GRAPHQL_URL);

  const activitySchemaDataLoader = new DataLoader(activitySchemaBatchLoader({ graphqlClient }), {
    cacheKeyFn: objectCacheKeyFunction,
  });

  const context: Context = {
    commandTypescriptDataLoader: new DataLoader(commandDictionaryTypescriptBatchLoader({ graphqlClient }), {
      cacheKeyFn: objectCacheKeyFunction,
    }),
    activitySchemaDataLoader,
    simulatedActivitiesDataLoader: new DataLoader(
      simulatedActivitiesBatchLoader({
        graphqlClient,
        activitySchemaDataLoader,
      }),
      {
        cacheKeyFn: objectCacheKeyFunction,
      },
    ),
    simulatedActivityInstanceBySimulatedActivityIdDataLoader: new DataLoader(
      simulatedActivityInstanceBySimulatedActivityIdBatchLoader({
        graphqlClient,
        activitySchemaDataLoader,
      }),
      {
        cacheKeyFn: objectCacheKeyFunction,
      },
    ),
    expansionSetDataLoader: new DataLoader(expansionSetBatchLoader({ graphqlClient }), {
      cacheKeyFn: objectCacheKeyFunction,
    }),
    expansionDataLoader: new DataLoader(expansionBatchLoader({ graphqlClient }), {
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
    insert into expansion_rule (activity_type, expansion_logic, authoring_command_dict_id,
                                authoring_mission_model_id)
    values ($1, $2, $3, $4)
    returning id;
  `,
    [activityTypeName, expansionLogic, authoringCommandDictionaryId, authoringMissionModelId],
  );

  if (rows.length < 1) {
    throw new Error(`POST /put-expansion: No expansion was updated in the database`);
  }

  const id = rows[0].id;
  logger.info(`POST /put-expansion: Updated expansion in the database: id=${id}`);

  if (authoringMissionModelId == null || authoringCommandDictionaryId == null) {
    res.status(200).json({ id });
    return next();
  }

  const commandTypes = await context.commandTypescriptDataLoader.load({ dictionaryId: authoringCommandDictionaryId });
  const activitySchema = await context.activitySchemaDataLoader.load({
    missionModelId: authoringMissionModelId,
    activityTypeName,
  });
  const activityTypescript = generateTypescriptForGraphQLActivitySchema(activitySchema);

  const result = await (piscina.run(
    {
      expansionLogic,
      commandTypes: commandTypes,
      activityTypes: activityTypescript,
    },
    { name: 'typecheckExpansion' },
  ) as ReturnType<typeof typecheckExpansion>);

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

  const typecheckErrorPromises = await Promise.allSettled(
    expansions.map(async (expansion, index) => {
      if (expansion instanceof Error) {
        throw new InheritedError(`Expansion with id: ${expansionIds[index]} could not be loaded`, expansion);
      }
      const activitySchema = await context.activitySchemaDataLoader.load({
        missionModelId,
        activityTypeName: expansion.activityType,
      });
      const activityTypescript = generateTypescriptForGraphQLActivitySchema(activitySchema);
      const result = await (piscina.run(
        {
          expansionLogic: expansion.expansionLogic,
          commandTypes: commandTypes,
          activityTypes: activityTypescript,
        },
        { name: 'typecheckExpansion' },
      ) as ReturnType<typeof typecheckExpansion>);

      return result.errors;
    }),
  );

  const errors = unwrapPromiseSettledResults(typecheckErrorPromises).reduce((accum, item) => {
    if (item instanceof Error) {
      accum.push(item);
    } else {
      accum.concat(item);
    }
    return accum;
  }, [] as (Error | ReturnType<UserCodeError['toJSON']>)[]);

  if (errors.length > 0) {
    throw new InheritedError(
      `Expansion set could not be typechecked`,
      errors.map(e => ({
        name: 'TypeCheckError',
        stack: e.stack,
        // @ts-ignore  Message is not spread when it comes from an Error object because it's a getter
        message: e.message,
        ...e,
      })),
    );
  }

  const { rows } = await db.query(
    `
        with expansion_set_id as (
          insert into expansion_set (command_dict_id, mission_model_id)
            values ($1, $2)
            returning id),
             rules as (select id, activity_type from expansion_rule where id = any ($3::int[]) order by id)
        insert
        into expansion_set_to_rule (set_id, rule_id, activity_type)
        select a.id, b.id, b.activity_type
        from (select id from expansion_set_id) a,
             (select id, activity_type from rules) b
        returning (select id from expansion_set_id);
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
  const [expansionSet, simulatedActivities] = await Promise.all([
    context.expansionSetDataLoader.load({ expansionSetId }),
    context.simulatedActivitiesDataLoader.load({ simulationDatasetId }),
  ]);
  const commandTypes = expansionSet.commandDictionary.commandTypesTypeScript;

  const settledExpansionResults = await Promise.allSettled(
    simulatedActivities.map(async simulatedActivity => {
      const activitySchema = await context.activitySchemaDataLoader.load({
        missionModelId: expansionSet.missionModel.id,
        activityTypeName: simulatedActivity.activityTypeName,
      });
      const expansion = expansionSet.expansionRules.find(
        expansionRule => expansionRule.activityType === simulatedActivity.activityTypeName,
      );

      if (expansion === undefined) {
        return {
          activityInstance: simulatedActivity,
          commands: null,
          errors: null,
        };
      }
      const activityTypes = generateTypescriptForGraphQLActivitySchema(activitySchema);
      return (await piscina.run(
        {
          expansionLogic: expansion.expansionLogic,
          activityInstance: simulatedActivity,
          commandTypes,
          activityTypes,
        },
        { name: 'executeExpansion' },
      )) as ReturnType<typeof executeExpansion>;
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
        with expansion_run_id as (
          insert into expansion_run (simulation_dataset_id, expansion_set_id)
            values ($1, $2)
            returning id)
        insert
        into activity_instance_commands (expansion_run_id,
                                         activity_instance_id,
                                         commands,
                                         errors)
        select *
        from unnest(
            array_fill((select id from expansion_run_id), array [array_length($3::int[], 1)]),
            $3::int[],
            $4::jsonb[],
            $5::jsonb[]
          )
        returning (select id from expansion_run_id);
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

export interface SeqBuilder {
  (
    sortedActivityInstancesWithCommands: (SimulatedActivity & {
      commands: Command[] | null;
      errors: ReturnType<UserCodeError['toJSON']>[] | null;
    })[],
    seqId: string,
    seqMetadata: Record<string, any>,
  ): Sequence;
}

app.post('/get-seqjson-for-sequence', async (req, res, next) => {
  // Get the specified sequence + activity instance ids + commands from the latest expansion run for each activity instance (filtered on simulation dataset)
  // get start time for each activity instance and join with activity instance command data
  // Create sequence object with all the commands and return the seqjson
  const context: Context = res.locals.context;

  const seqId = req.body.input.seqId as string;
  const simulationDatasetId = req.body.input.simulationDatasetId as number;

  const [{ rows: activityInstanceCommandRows }, { rows: seqRows }] = await Promise.all([
    db.query<{
      metadata: Record<string, unknown>;
      commands: CommandSeqJson[];
      activity_instance_id: number;
      errors: ReturnType<UserCodeError['toJSON']>[] | null;
    }>(
      `
          with joined_table as (select activity_instance_commands.commands,
                                       activity_instance_commands.activity_instance_id,
                                       activity_instance_commands.errors,
                                       activity_instance_commands.expansion_run_id
                                from sequence
                                       join sequence_to_simulated_activity
                                            on sequence.seq_id = sequence_to_simulated_activity.seq_id and
                                               sequence.simulation_dataset_id =
                                               sequence_to_simulated_activity.simulation_dataset_id
                                       join activity_instance_commands
                                            on sequence_to_simulated_activity.simulated_activity_id =
                                               activity_instance_commands.activity_instance_id
                                       join expansion_run
                                            on activity_instance_commands.expansion_run_id = expansion_run.id
                                where sequence.seq_id = $2
                                  and sequence.simulation_dataset_id = $1),
               max_values as (select activity_instance_id, max(expansion_run_id) as max_expansion_run_id
                              from joined_table
                              group by activity_instance_id)
          select joined_table.commands,
                 joined_table.activity_instance_id,
                 joined_table.errors
          from joined_table,
               max_values
          where joined_table.activity_instance_id = max_values.activity_instance_id
            and joined_table.expansion_run_id = max_values.max_expansion_run_id;
        `,
      [simulationDatasetId, seqId],
    ),
    db.query<{
      metadata: Record<string, any>;
    }>(
      `
          select metadata
          from sequence
          where sequence.seq_id = $2
            and sequence.simulation_dataset_id = $1;
        `,
      [simulationDatasetId, seqId],
    ),
  ]);

  const seqMetadata = assertOne(
    seqRows,
    `No sequence found with seq_id: ${seqId} and simulation_dataset_id: ${simulationDatasetId}`,
  ).metadata;

  const simulatedActivities = await context.simulatedActivityInstanceBySimulatedActivityIdDataLoader.loadMany(
    activityInstanceCommandRows.map(row => ({
      simulationDatasetId,
      simulatedActivityId: row.activity_instance_id,
    })),
  );
  const simulatedActivitiesLoadErrors = simulatedActivities.filter(ai => ai instanceof Error);
  if (simulatedActivitiesLoadErrors.length > 0) {
    res.status(500).json({
      message: 'Error loading simulated activities',
      cause: simulatedActivitiesLoadErrors,
    });
    return next();
  }

  const sortedActivityInstances = (simulatedActivities as Exclude<typeof simulatedActivities[number], Error>[]).sort(
    (a, b) => Temporal.Duration.compare(a.startOffset, b.startOffset),
  );

  const sortedSimulatedActivitiesWithCommands = sortedActivityInstances.map(ai => {
    const row = activityInstanceCommandRows.find(row => row.activity_instance_id === ai.id);
    // Hasn't ever been expanded
    if (!row) {
      return {
        ...ai,
        commands: null,
        errors: null,
      };
    }
    return {
      ...ai,
      commands: row.commands?.map(Command.fromSeqJson) ?? null,
      errors: row.errors,
    };
  });

  // This is here to easily enable a future feature of allowing the mission to configure their own sequence
  // building. For now, we just use the 'defaultSeqBuilder' until such a feature request is made.
  const seqBuilder = defaultSeqBuilder;

  res.status(200).json(seqBuilder(sortedSimulatedActivitiesWithCommands, seqId, seqMetadata).toSeqJson());
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
