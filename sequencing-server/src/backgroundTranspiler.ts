import { piscina, graphqlClient, typeCheckingCache, promiseThrottler } from './app.js';
import { objectCacheKeyFunction } from './lib/batchLoaders/index.js';
import crypto from 'crypto';
import { generateTypescriptForGraphQLActivitySchema } from './lib/codegen/ActivityTypescriptCodegen.js';
import DataLoader from 'dataloader';
import { activitySchemaBatchLoader } from './lib/batchLoaders/activitySchemaBatchLoader.js';
import { commandDictionaryTypescriptBatchLoader } from './lib/batchLoaders/commandDictionaryTypescriptBatchLoader.js';
import type { typecheckExpansion } from './worker';
import { Result } from '@nasa-jpl/aerie-ts-user-code-runner/build/utils/monads.js';
import { getLatestCommandDictionary, getLatestMissionModel, getExpansionRule } from './utils/hasura.js';

export async function backgroundTranspiler(numberOfThreads: number = 2) {
  if (graphqlClient === null) {
    return;
  }

  // Fetch latest mission model
  const { mission_model_aggregate: {aggregate: {max: {id: missionModelId} } } } = await getLatestMissionModel(graphqlClient);
  if (!missionModelId) {
    console.log(
      '[ Background Transpiler ] Unable to fetch the latest mission model. Aborting background transpiling...',
    );
    return;
  }

  // Fetch latest command dictionary
  const { command_dictionary_aggregate: {aggregate: {max: {id: commandDictionaryId} } } } = await getLatestCommandDictionary(graphqlClient);
  if (!commandDictionaryId) {
    console.log(
      '[ Background Transpiler ] Unable to fetch the latest command dictionary. Aborting background transpiling...',
    );
    return;
  }

  const { expansion_rule } = await getExpansionRule(graphqlClient, missionModelId, commandDictionaryId);

  if (expansion_rule === null || expansion_rule.length === 0) {
    console.log(`[ Background Transpiler ] No expansion rules to transpile.`);
    return;
  }

  const commandTypescriptDataLoader = new DataLoader(commandDictionaryTypescriptBatchLoader({ graphqlClient }), {
    cacheKeyFn: objectCacheKeyFunction,
    name: null,
  });
  const activitySchemaDataLoader = new DataLoader(activitySchemaBatchLoader({ graphqlClient }), {
    cacheKeyFn: objectCacheKeyFunction,
    name: null,
  });

  const commandTypes = await commandTypescriptDataLoader.load({
    dictionaryId: commandDictionaryId,
  });

  if (commandTypes === null) {
    console.log(`[ Background Transpiler ] Unable to fetch command ts lib.
    Aborting transpiling...`);
    return;
  }

  // only process 'numberOfThreads' worth at a time ex. transpile 2 logics at a time
  // This allows for expansion set and sequence expansion to utilize the remaining workers
  for (let i = 0; i < expansion_rule.length; i += numberOfThreads) {
    await Promise.all(
      expansion_rule.slice(i, i + numberOfThreads).map(async expansion => {
        await promiseThrottler.run(async () => {
          // Assuming expansion_rule elements have the same type
          if (expansion instanceof Error) {
            console.log(`[ Background Transpiler ] Expansion: ${expansion.name} could not be loaded`, expansion);
            return Promise.reject(`Expansion: ${expansion.name} could not be loaded`);
          }

          const hash = crypto
            .createHash('sha256')
            .update(
              JSON.stringify({
                commandDictionaryId,
                missionModelId,
                id: expansion.id,
                expansionLogic: expansion.expansion_logic,
                activityType: expansion.activity_type,
              }),
            )
            .digest('hex');

          // ignore already transpiled hash info
          if (typeCheckingCache.has(hash)) {
            return Promise.resolve();
          }

          const activitySchema = await activitySchemaDataLoader.load({
            missionModelId,
            activityTypeName: expansion.activity_type,
          });

          // log error
          if (!activitySchema) {
            console.log(
              `[ Background Transpiler ] Activity schema for ${expansion.activity_type} could not be loaded`,
              activitySchema,
            );
            return Promise.reject('Activity schema for ${expansion.activity_type} could not be loaded');
          }

          const activityTypescript = generateTypescriptForGraphQLActivitySchema(activitySchema);

          // log error
          if (!activityTypescript) {
            console.log(
              `[ Background Transpiler ] Unable to generate typescript for activity ${expansion.activity_type}`,
              activityTypescript,
            );
            return Promise.reject(`Unable to generate typescript for activity ${expansion.activity_type}`);
          }

          const typecheckingResult = (
            piscina.run(
              {
                expansionLogic: expansion.expansion_logic,
                commandTypes: commandTypes,
                activityTypes: activityTypescript,
                activityTypeName: expansion.activity_type,
              },
              { name: 'typecheckExpansion' },
            ) as ReturnType<typeof typecheckExpansion>
          ).then(Result.fromJSON);

          //Display any errors
          typecheckingResult.then(result => {
            if (result.isErr()) {
              console.log(`Error transpiling ${expansion.activity_type}:\n ${result.unwrapErr().map(e => e.message)}`);
            }
          });

          typeCheckingCache.set(hash, typecheckingResult);
          return typecheckingResult;
        });
      }),
    );
  }
}
