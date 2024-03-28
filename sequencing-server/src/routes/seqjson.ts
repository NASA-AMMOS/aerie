import type { UserCodeError } from '@nasa-jpl/aerie-ts-user-code-runner';
import { Context, db, piscina } from './../app.js';
import { Result } from '@nasa-jpl/aerie-ts-user-code-runner/build/utils/monads.js';
import express from 'express';
import pgFormat from 'pg-format';
import { defaultSeqBuilder } from './../defaultSeqBuilder.js';
import Ajv from 'ajv/dist/2020.js';
import schema from '@nasa-jpl/seq-json-schema/schema.json' assert { type: 'json' };
import { ActivateStep, LoadStep, Sequence } from './../lib/codegen/CommandEDSLPreface.js';
import type { SeqJson } from '@nasa-jpl/seq-json-schema/types';
import { CommandStem } from './../lib/codegen/CommandEDSLPreface.js';
import type { Command , Activate, Load} from '@nasa-jpl/seq-json-schema/types';
import { FallibleStatus } from './../types.js';
import { assertDefined, assertOne } from './../utils/assertions.js';
import { isResolved } from './../utils/typeguards.js';
import type { executeEDSL } from './../worker.js';

export const seqjsonRouter = express.Router();

/**
 * Generate a sequence JSON from a sequence standalone file
 *
 * @deprecated Use `/bulk-get-seqjson-for-sequence-standalone` instead
 */
seqjsonRouter.post('/get-seqjson-for-sequence-standalone', async (req, res, next) => {
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
seqjsonRouter.post('/bulk-get-seqjson-for-sequence-standalone', async (req, res, next) => {
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
seqjsonRouter.post('/get-seqjson-for-seqid-and-simulation-dataset', async (req, res, next) => {
  // Get the specified sequence + activity instance ids + commands from the latest expansion run for each activity instance (filtered on simulation dataset)
  // get start time for each activity instance and join with activity instance command data
  // Create sequence object with all the commands and return the seqjson
  const context: Context = res.locals['context'];

  const seqId = req.body.input.seqId as string;
  const simulationDatasetId = req.body.input.simulationDatasetId as number;

  const [{ rows: activityInstanceCommandRows }, { rows: seqRows }] = await Promise.all([
    db.query<{
      metadata: Record<string, unknown>;
      commands: (Command | Activate | Load)[];
      activity_instance_id: number;
      errors: ReturnType<UserCodeError['toJSON']>[] | null;
    }>(
      `
        with joined_table as (
          select  aic.commands,
                  aic.activity_instance_id,
                  aic.errors,
                  aic.expansion_run_id
          from sequence_to_simulated_activity ssa
          join activity_instance_commands aic
            on ssa.simulated_activity_id = aic.activity_instance_id
          where (ssa.simulation_dataset_id, ssa.seq_id) = ($1, $2)),
          max_values as (
            select
              activity_instance_id,
              max(expansion_run_id) as max_expansion_run_id
            from joined_table
            group by activity_instance_id
          )
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

  const sortedActivityInstances = (simulatedActivities as Exclude<(typeof simulatedActivities)[number], Error>[]).sort(
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
      commands: row.commands?.map(c => {
        switch (c.type) {
          case 'command':
            return CommandStem.fromSeqJson(c);
          case 'load':
            return LoadStep.fromSeqJson(c);
          case 'activate':
            return ActivateStep.fromSeqJson(c)
          default:
            throw new Error(`Unknown command type: ${c}`);
        }
      }) ?? null,
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

seqjsonRouter.post('/bulk-get-seqjson-for-seqid-and-simulation-dataset', async (req, res, next) => {
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
      commands: (Command | Activate | Load)[];
      activity_instance_id: number;
      errors: ReturnType<UserCodeError['toJSON']>[] | null;
      seq_id: string;
      simulation_dataset_id: number;
    }>(
      `
        with
        joined_table as (
          select
            aic.commands,
            aic.activity_instance_id,
            aic.errors,
            aic.expansion_run_id,
            ssa.seq_id,
            ssa.simulation_dataset_id
          from sequence_to_simulated_activity ssa
          join activity_instance_commands aic
            on ssa.simulated_activity_id = aic.activity_instance_id
          where (ssa.seq_id, ssa.simulation_dataset_id) in (${pgFormat('%L', inputTuples)})
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
        from sequence s
        where (s.seq_id, s.simulation_dataset_id) in (${pgFormat('%L', inputTuples)});
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
        simulatedActivitiesForSeqId as Exclude<(typeof simulatedActivitiesLoadErrors)[number], Error>[]
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
          commands: row.commands?.map(c => {
            switch (c.type) {
              case 'command':
                return CommandStem.fromSeqJson(c);
              case 'load':
                return LoadStep.fromSeqJson(c);
              case 'activate':
                return ActivateStep.fromSeqJson(c)
              default:
                throw new Error(`Unknown command type: ${c}`);
            }
          }) ?? null,
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
seqjsonRouter.post('/get-edsl-for-seqjson', async (req, res, next) => {
  const seqJson = req.body.input.seqJson as SeqJson;
  const validate = new Ajv({ strict: false }).compile(schema);

  if (!validate(seqJson)) {
    throw new Error(
      `POST /seqjson/bulk-get-edsl-for-seqjson: Uploaded sequence JSON is invalid and does not match the current spec`,
    );
  }

  res.json(Sequence.fromSeqJson(seqJson).toEDSLString());

  return next();
});

// Generate Sequence EDSL from many sequence JSONs
seqjsonRouter.post('/bulk-get-edsl-for-seqjson', async (req, res, next) => {
  const seqJsons = req.body.input.seqJsons as SeqJson[];
  const validate = new Ajv({ strict: false }).compile(schema);

  res.json(
    seqJsons.map(seqJson => {
      if (!validate(seqJson)) {
        throw new Error(
          `POST /seqjson/bulk-get-edsl-for-seqjson: Uploaded sequence JSON is invalid and does not match the current spec`,
        );
      }

      return Sequence.fromSeqJson(seqJson).toEDSLString();
    }),
  );

  return next();
});
