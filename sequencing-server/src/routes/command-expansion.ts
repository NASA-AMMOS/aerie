import type { CacheItem, UserCodeError } from '@nasa-jpl/aerie-ts-user-code-runner';
import pgFormat from 'pg-format';
import { Context, db, piscina } from './../app.js';
import { Result } from '@nasa-jpl/aerie-ts-user-code-runner/build/utils/monads.js';
import express from 'express';
import { serializeWithTemporal } from './../utils/temporalSerializers.js';
import { generateTypescriptForGraphQLActivitySchema } from './../lib/codegen/ActivityTypescriptCodegen.js';
import { isRejected, isResolved } from './../utils/typeguards.js';
import type { executeExpansionFromBuildArtifacts, typecheckExpansion } from './../worker.js';
import getLogger from './../utils/logger.js';
import { InheritedError } from '../utils/InheritedError.js';
import { unwrapPromiseSettledResults } from '../lib/batchLoaders/index.js';
import { defaultSeqBuilder } from '../defaultSeqBuilder.js';
import { CommandStem } from './../lib/codegen/CommandEDSLPreface.js';
import { getUsername } from '../utils/helpers.js';

const logger = getLogger('app');

export const commandExpansionRouter = express.Router();

commandExpansionRouter.post('/put-expansion', async (req, res, next) => {
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

commandExpansionRouter.post('/put-expansion-set', async (req, res, next) => {
  const context: Context = res.locals['context'];
  const username = getUsername(req.body.session_variables, req.headers.authorization);

  const commandDictionaryId = req.body.input.commandDictionaryId as number;
  const missionModelId = req.body.input.missionModelId as number;
  const expansionIds = req.body.input.expansionIds as number[];
  const description = req.body.input.description as string | null;

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
          insert into expansion_set (command_dict_id, mission_model_id, description, owner)
            values ($1, $2, $3, $4)
            returning id),
             rules as (select id, activity_type from expansion_rule where id = any ($5::int[]) order by id)
        insert
        into expansion_set_to_rule (set_id, rule_id, activity_type)
        select a.id, b.id, b.activity_type
        from (select id from expansion_set_id) a,
             (select id, activity_type from rules) b
        returning (select id from expansion_set_id);
      `,
    [commandDictionaryId, missionModelId, description ?? '', username, expansionIds],
  );

  if (rows.length < 1) {
    throw new Error(`POST /command-expansion/put-expansion-set: No expansion set was inserted in the database`);
  }
  const id = rows[0].id;
  logger.info(`POST /command-expansion/put-expansion-set: Updated expansion set in the database: id=${id}`);
  res.status(200).json({ id });
  return next();
});

commandExpansionRouter.post('/expand-all-activity-instances', async (req, res, next) => {
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
      // The simulatedActivity's duration and endTime will be null if the effect model reaches across the plan end boundaries.
      if (!simulatedActivity.duration && !simulatedActivity.endTime) {
        return {
          activityInstance: simulatedActivity,
          commands: null,
          errors: [{ message: 'Duration is null' }],
        };
      }

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
    throw new Error(
      `POST /command-expansion/expand-all-activity-instances: No expansion run was inserted in the database`,
    );
  }
  const expansionRunId = rows[0].id;
  logger.info(
    `POST /command-expansion/expand-all-activity-instances: Inserted expansion run to the database: id=${expansionRunId}`,
  );

  // Get all the sequence IDs that are assigned to simulated activities.
  const seqToSimulatedActivity = await db.query(
    `
      select seq_id
      from sequence_to_simulated_activity
      where sequence_to_simulated_activity.simulated_activity_id in (${pgFormat(
        '%L',
        expandedActivityInstances.map(eai => eai.id),
      )})
      and simulation_dataset_id = $1
    `,
    [simulationDatasetId],
  );

  if (seqToSimulatedActivity.rows.length > 0) {
    const seqRows = await db.query(
      `
        select metadata, seq_id, simulation_dataset_id
        from sequence
        where sequence.seq_id in (${pgFormat(
          '%L',
          seqToSimulatedActivity.rows.map(row => row.seq_id),
        )})
        and sequence.simulation_dataset_id = $1;
      `,
      [simulationDatasetId],
    );

    // If the user has created a sequence, we can try to save the expanded sequences when an expansion runs.
    for (const seqRow of seqRows.rows) {
      const seqId = seqRow.seq_id;
      const seqMetadata = seqRow.metadata;

      const simulatedActivities = await context.simulatedActivityInstanceBySimulatedActivityIdDataLoader.loadMany(
        expandedActivityInstances.map(row => ({
          simulationDatasetId,
          simulatedActivityId: row.id,
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

      const sortedActivityInstances = (
        simulatedActivities as Exclude<(typeof simulatedActivities)[number], Error>[]
      ).sort((a, b) => Temporal.Duration.compare(a.startOffset, b.startOffset));

      const sortedSimulatedActivitiesWithCommands = sortedActivityInstances.map(ai => {
        const row = expandedActivityInstances.find(row => row.id === ai.id);

        // Hasn't ever been expanded
        if (!row) {
          return {
            ...ai,
            commands: null,
            errors: null,
          };
        }

        const errors = row.errors as unknown;

        return {
          ...ai,
          commands: row.commands?.map(c => CommandStem.fromSeqJson(c)) ?? null,
          errors: errors as { message: string; stack: string; location: { line: number; column: number } }[],
        };
      });

      // This is here to easily enable a future feature of allowing the mission to configure their own sequence
      // building. For now, we just use the 'defaultSeqBuilder' until such a feature request is made.
      const seqBuilder = defaultSeqBuilder;
      const sequence = seqBuilder(sortedSimulatedActivitiesWithCommands, seqId, seqMetadata, simulationDatasetId);

      const { rows } = await db.query(
        `
          insert into expanded_sequences (expansion_run_id, seq_id, simulation_dataset_id, expanded_sequence, edsl_string)
            values ($1, $2, $3, $4, $5)
            returning id
      `,
        [expansionRunId, seqId, simulationDatasetId, sequence.toSeqJson(), sequence.toEDSLString()],
      );

      if (rows.length < 1) {
        throw new Error(
          `POST /command-expansion/expand-all-activity-instances: No expanded sequences were inserted into the database`,
        );
      }
      const expandedSequenceId = rows[0].id;
      logger.info(
        `POST /command-expansion/expand-all-activity-instances: Inserted expanded sequence to the database: id=${expandedSequenceId}`,
      );
    }
  }

  res.status(200).json({
    id: expansionRunId,
    expandedActivityInstances,
  });
  return next();
});
