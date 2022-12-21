import * as ampcs from '@nasa-jpl/aerie-ampcs';
import type { CacheItem, UserCodeError } from '@nasa-jpl/aerie-ts-user-code-runner';
import { Result } from '@nasa-jpl/aerie-ts-user-code-runner/build/utils/monads.js';
import bodyParser from 'body-parser';
import DataLoader from 'dataloader';
import express, { Application, NextFunction, Request, Response } from 'express';
import { GraphQLClient } from 'graphql-request';
import fs from 'node:fs';
import pgFormat from 'pg-format';
import Piscina from 'piscina';
import { Status } from './common.js';
import { DbExpansion } from './db.js';
import { defaultSeqBuilder } from './defaultSeqBuilder.js';
import { getEnv } from './env.js';
import { activitySchemaBatchLoader } from './lib/batchLoaders/activitySchemaBatchLoader.js';
import { commandDictionaryTypescriptBatchLoader } from './lib/batchLoaders/commandDictionaryTypescriptBatchLoader.js';
import { expansionBatchLoader } from './lib/batchLoaders/expansionBatchLoader.js';
import { expansionSetBatchLoader } from './lib/batchLoaders/expansionSetBatchLoader.js';
import { InferredDataloader, objectCacheKeyFunction, unwrapPromiseSettledResults } from './lib/batchLoaders/index.js';
import {
  simulatedActivitiesBatchLoader,
  simulatedActivityInstanceBySimulatedActivityIdBatchLoader,
} from './lib/batchLoaders/simulatedActivityBatchLoader.js';
import { generateTypescriptForGraphQLActivitySchema } from './lib/codegen/ActivityTypescriptCodegen.js';
import { Command, CommandSeqJson, Sequence, SequenceSeqJson } from './lib/codegen/CommandEDSLPreface.js';
import { processDictionary } from './lib/codegen/CommandTypeCodegen.js';
import './polyfills.js';
import { FallibleStatus } from './types.js';
import { assertDefined, assertOne } from './utils/assertions.js';
import { InheritedError } from './utils/InheritedError.js';
import getLogger from './utils/logger.js';
import { serializeWithTemporal } from './utils/temporalSerializers.js';
import { isRejected, isResolved } from './utils/typeguards.js';
import type { executeEDSL, executeExpansionFromBuildArtifacts, typecheckExpansion } from './worker.js';

const logger = getLogger('app');

const PORT: number = parseInt(getEnv().PORT, 10) ?? 27184;

const app: Application = express();
// WARNING: bodyParser.json() is vulnerable to a string too long issue. Iff that occurs,
// we should switch to a stream parser like https://www.npmjs.com/package/stream-json
app.use(bodyParser.json({ limit: '100mb' }));

DbExpansion.init();
const db = DbExpansion.getDb();

const piscina = new Piscina({ filename: new URL('worker.js', import.meta.url).pathname });
const temporalPolyfillTypes = fs.readFileSync(new URL('TemporalPolyfillTypes.ts', import.meta.url).pathname, 'utf-8');

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

app.use(async (_: Request, res: Response, next: NextFunction) => {
  const graphqlClient = new GraphQLClient(getEnv().MERLIN_GRAPHQL_URL);

  const activitySchemaDataLoader = new DataLoader(activitySchemaBatchLoader({ graphqlClient }), {
    cacheKeyFn: objectCacheKeyFunction,
  });

  res.locals['context'] = {
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
  } as Context;
  return next();
});

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

app.post('/put-expansion', async (req, res, next) => {
  const context: Context = res.locals['context'];

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

  const result = Result.fromJSON(
    await (piscina.run(
      {
        expansionLogic,
        commandTypes: commandTypes,
        activityTypes: activityTypescript,
      },
      { name: 'typecheckExpansion' },
    ) as ReturnType<typeof typecheckExpansion>),
  );

  res.status(200).json({ id, errors: result.isErr() ? result.unwrapErr() : [] });
  return next();
});

app.post('/put-expansion-set', async (req, res, next) => {
  const context: Context = res.locals['context'];

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
      const result = Result.fromJSON(
        await (piscina.run(
          {
            expansionLogic: expansion.expansionLogic,
            commandTypes: commandTypes,
            activityTypes: activityTypescript,
          },
          { name: 'typecheckExpansion' },
        ) as ReturnType<typeof typecheckExpansion>),
      );

      return result;
    }),
  );

  const errors = unwrapPromiseSettledResults(typecheckErrorPromises).reduce((accum, item) => {
    if (item instanceof Error) {
      accum.push(item);
    } else if (item.isErr()) {
      accum.push(...item.unwrapErr());
    }
    return accum;
  }, [] as (Error | ReturnType<UserCodeError['toJSON']>)[]);

  if (errors.length > 0) {
    throw new InheritedError(
      `Expansion set could not be type checked`,
      errors.map(e => ({
        name: 'TypeCheckError',
        stack: e.stack ?? null,
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

app.post('/expand-all-activity-instances', async (req, res, next) => {
  const context: Context = res.locals['context'];

  // Query for expansion set data
  const expansionSetId = req.body.input.expansionSetId as number;
  const simulationDatasetId = req.body.input.simulationDatasetId as number;
  const [expansionSet, simulatedActivities] = await Promise.all([
    context.expansionSetDataLoader.load({ expansionSetId }),
    context.simulatedActivitiesDataLoader.load({ simulationDatasetId }),
  ]);
  const commandTypes = expansionSet.commandDictionary.commandTypesTypeScript;

  // Note: We are keeping the Promise in the cache so that we don't have to wait for resolution to insert into
  // the cache and consequently end up doing the compilation multiple times because of a cache miss.
  const expansionBuildArtifactsCache = new Map<
    number,
    Promise<Result<CacheItem, ReturnType<UserCodeError['toJSON']>[]>>
  >();

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

      if (!expansionBuildArtifactsCache.has(expansion.id)) {
        const typecheckResult = (
          piscina.run(
            {
              expansionLogic: expansion.expansionLogic,
              commandTypes: commandTypes,
              activityTypes,
            },
            { name: 'typecheckExpansion' },
          ) as ReturnType<typeof typecheckExpansion>
        ).then(Result.fromJSON);
        expansionBuildArtifactsCache.set(expansion.id, typecheckResult);
      }

      const expansionBuildArtifacts = await expansionBuildArtifactsCache.get(expansion.id)!;

      if (expansionBuildArtifacts.isErr()) {
        return {
          activityInstance: simulatedActivity,
          commands: null,
          errors: expansionBuildArtifacts.unwrapErr(),
        };
      }

      const buildArtifacts = expansionBuildArtifacts.unwrap();

      const executionResult = Result.fromJSON(
        await (piscina.run(
          {
            serializedActivityInstance: serializeWithTemporal(simulatedActivity),
            buildArtifacts,
          },
          { name: 'executeExpansionFromBuildArtifacts' },
        ) as ReturnType<typeof executeExpansionFromBuildArtifacts>),
      );

      if (executionResult.isErr()) {
        return {
          activityInstance: simulatedActivity,
          commands: null,
          errors: executionResult.unwrapErr(),
        };
      }

      return {
        activityInstance: simulatedActivity,
        commands: executionResult.unwrap(),
        errors: [],
      };
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

/**
 * Generate a sequence JSON from a sequence standalone file
 *
 * @deprecated Use `/bulk-get-seqjson-for-sequence-standalone` instead
 */
app.post('/get-seqjson-for-sequence-standalone', async (req, res, next) => {
  const commandDictionaryID = req.body.input.commandDictionaryID as number;
  const edslBody = req.body.input.edslBody as string;

  let commandTypes;
  try {
    const context: Context = res.locals['context'];
    commandTypes = await context.commandTypescriptDataLoader.load({ dictionaryId: commandDictionaryID });
  } catch (e) {
    res.status(500).json({
      message: 'Error loading command dictionary',
      cause: (e as Error).message,
    });
    return next();
  }

  const result = Result.fromJSON(
    await (piscina.run(
      {
        edslBody,
        commandTypes,
      },
      { name: 'executeEDSL' },
    ) as ReturnType<typeof executeEDSL>),
  );

  if (result.isErr()) {
    res.json({
      status: FallibleStatus.FAILURE,
      seqJson: null,
      errors: result.unwrapErr(),
    });
  }

  res.json({
    status: FallibleStatus.SUCCESS,
    seqJson: result.unwrap(),
    errors: [],
  });

  return next();
});

/** Generate multiple sequence JSONs from multiple sequence standalone files */
app.post('/bulk-get-seqjson-for-sequence-standalone', async (req, res, next) => {
  const inputs = req.body.input.inputs as { commandDictionaryId: number; edslBody: string }[];

  const context: Context = res.locals['context'];

  const results = await Promise.all(
    inputs.map(async ({ commandDictionaryId, edslBody }) => {
      let commandTypes: string;
      try {
        commandTypes = await context.commandTypescriptDataLoader.load({ dictionaryId: commandDictionaryId });
      } catch (e) {
        return {
          status: FallibleStatus.FAILURE,
          seqJson: null,
          errors: new Error('Error loading command dictionary', { cause: e }),
        };
      }

      const result = Result.fromJSON(
        await (piscina.run(
          {
            edslBody,
            commandTypes,
          },
          { name: 'executeEDSL' },
        ) as ReturnType<typeof executeEDSL>),
      );

      if (result.isErr()) {
        return {
          status: FallibleStatus.FAILURE,
          seqJson: null,
          errors: result.unwrapErr(),
        };
      } else {
        return {
          status: FallibleStatus.SUCCESS,
          seqJson: result.unwrap(),
          errors: [],
        };
      }
    }),
  );

  res.json(results);

  return next();
});

/**
 * Get the sequence JSON for a sequence based on seqid and simulation dataset id
 *
 * @deprecated Use `/bulk-get-seqjson-for-seqid-and-simulation-dataset` instead
 */
app.post('/get-seqjson-for-seqid-and-simulation-dataset', async (req, res, next) => {
  // Get the specified sequence + activity instance ids + commands from the latest expansion run for each activity instance (filtered on simulation dataset)
  // get start time for each activity instance and join with activity instance command data
  // Create sequence object with all the commands and return the seqjson
  const context: Context = res.locals['context'];

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

  const errors = sortedSimulatedActivitiesWithCommands.flatMap(ai => ai.errors ?? []);

  // This is here to easily enable a future feature of allowing the mission to configure their own sequence
  // building. For now, we just use the 'defaultSeqBuilder' until such a feature request is made.
  const seqBuilder = defaultSeqBuilder;
  const sequenceJson = seqBuilder(
    sortedSimulatedActivitiesWithCommands,
    seqId,
    seqMetadata,
    simulationDatasetId,
  ).toSeqJson();

  if (errors.length > 0) {
    res.json({
      status: FallibleStatus.FAILURE,
      seqJson: sequenceJson,
      errors,
    });
  } else {
    res.json({
      status: FallibleStatus.SUCCESS,
      seqJson: sequenceJson,
      errors,
    });
  }
  return next();
});

app.post('/bulk-get-seqjson-for-seqid-and-simulation-dataset', async (req, res, next) => {
  // Get the specified sequence + activity instance ids + commands from the latest expansion run for each activity instance (filtered on simulation dataset)
  // get start time for each activity instance and join with activity instance command data
  // Create sequence object with all the commands and return the seqjson
  const context: Context = res.locals['context'];

  const inputs = req.body.input.inputs as { seqId: string; simulationDatasetId: number }[];

  const inputTuples = inputs.map(input => [input.seqId, input.simulationDatasetId] as [string, number]);

  // Grab all the activity instance commands and sequence metadata for the specified sequences
  const [{ rows: activityInstanceCommandRows }, { rows: seqRows }] = await Promise.all([
    db.query<{
      metadata: Record<string, unknown>;
      commands: CommandSeqJson[];
      activity_instance_id: number;
      errors: ReturnType<UserCodeError['toJSON']>[] | null;
      seq_id: string;
      simulation_dataset_id: number;
    }>(
      `
        with
        joined_table as (
          select
            activity_instance_commands.commands,
            activity_instance_commands.activity_instance_id,
            activity_instance_commands.errors,
            activity_instance_commands.expansion_run_id,
            sequence.seq_id,
            sequence.simulation_dataset_id
          from sequence
            join sequence_to_simulated_activity
              on sequence.seq_id = sequence_to_simulated_activity.seq_id
                and sequence.simulation_dataset_id =
                  sequence_to_simulated_activity.simulation_dataset_id
            join activity_instance_commands
              on sequence_to_simulated_activity.simulated_activity_id =
                activity_instance_commands.activity_instance_id
            join expansion_run
              on activity_instance_commands.expansion_run_id = expansion_run.id
          where (sequence.seq_id, sequence.simulation_dataset_id) in (${pgFormat('%L', inputTuples)})
        ),
        max_values as (
          select
            activity_instance_id,
            max(expansion_run_id) as max_expansion_run_id
          from joined_table
          group by activity_instance_id
        )
        select
          joined_table.commands,
          joined_table.activity_instance_id,
          joined_table.errors,
          joined_table.seq_id,
          joined_table.simulation_dataset_id
        from
          joined_table,
          max_values
        where joined_table.activity_instance_id = max_values.activity_instance_id
          and joined_table.expansion_run_id = max_values.max_expansion_run_id;
      `,
    ),
    db.query<{
      seq_id: string;
      simulation_dataset_id: number;
      metadata: Record<string, any>;
    }>(
      `
        select metadata, seq_id, simulation_dataset_id
        from sequence
        where (sequence.seq_id, sequence.simulation_dataset_id) in (${pgFormat('%L', inputTuples)});
      `,
    ),
  ]);

  // This is here to easily enable a future feature of allowing the mission to configure their own sequence
  // building. For now, we just use the 'defaultSeqBuilder' until such a feature request is made.
  const seqBuilder = defaultSeqBuilder;

  const promises = await Promise.allSettled(
    inputs.map(async ({ seqId, simulationDatasetId }) => {
      const activityInstanceCommandRowsForSeq = activityInstanceCommandRows.filter(
        row => row.seq_id === seqId && row.simulation_dataset_id === simulationDatasetId,
      );
      const seqRowsForSeq = seqRows.find(
        row => row.seq_id === seqId && row.simulation_dataset_id === simulationDatasetId,
      );

      const seqMetadata = assertDefined(
        seqRowsForSeq,
        `No sequence found with seq_id: ${seqId} and simulation_dataset_id: ${simulationDatasetId}`,
      ).metadata;

      const simulatedActivitiesForSeqId =
        await context.simulatedActivityInstanceBySimulatedActivityIdDataLoader.loadMany(
          activityInstanceCommandRowsForSeq.map(row => ({
            simulationDatasetId,
            simulatedActivityId: row.activity_instance_id,
          })),
        );

      const simulatedActivitiesLoadErrors = simulatedActivitiesForSeqId.filter(ai => ai instanceof Error);

      if (simulatedActivitiesLoadErrors.length > 0) {
        throw new Error(
          `Error loading simulated activities for seqId: ${seqId}, simulationDatasetId: ${simulationDatasetId}`,
          { cause: simulatedActivitiesLoadErrors },
        );
      }

      const sortedActivityInstances = (
        simulatedActivitiesForSeqId as Exclude<typeof simulatedActivitiesLoadErrors[number], Error>[]
      ).sort((a, b) => Temporal.Instant.compare(a.startTime, b.startTime));

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

      const errors = sortedSimulatedActivitiesWithCommands.flatMap(ai => ai.errors ?? []);

      const sequenceJson = seqBuilder(
        sortedSimulatedActivitiesWithCommands,
        seqId,
        seqMetadata,
        simulationDatasetId,
      ).toSeqJson();

      if (errors.length > 0) {
        return {
          status: FallibleStatus.FAILURE,
          seqJson: sequenceJson,
          errors,
        };
      } else {
        return {
          status: FallibleStatus.SUCCESS,
          seqJson: sequenceJson,
          errors: [],
        };
      }
    }),
  );

  res.json(
    promises.map(promise => {
      if (isResolved(promise)) {
        return promise.value;
      } else {
        return {
          status: FallibleStatus.FAILURE,
          seqJson: null,
          errors: [promise.reason],
        };
      }
    }),
  );

  return next();
});

/** Generate Sequence EDSL from sequence JSON
 *
 * @deprecated Use `/bulk-get-edsl-for-seqjson` instead
 */
app.post('/get-edsl-for-seqjson', async (req, res, next) => {
  const seqJson = req.body.input.seqJson as SequenceSeqJson;

  res.json(Sequence.fromSeqJson(seqJson).toEDSLString());
  return next();
});

// Generate Sequence EDSL from many sequence JSONs
app.post('/bulk-get-edsl-for-seqjson', async (req, res, next) => {
  const seqJsons = req.body.input.seqJsons as SequenceSeqJson[];

  res.json(seqJsons.map(seqJson => Sequence.fromSeqJson(seqJson).toEDSLString()));
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
