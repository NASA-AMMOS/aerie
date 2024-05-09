import './polyfills.js';

import vm from 'node:vm';
import * as fs from 'node:fs';

import ts from 'typescript';
import { CacheItem, UserCodeError, UserCodeRunner } from '@nasa-jpl/aerie-ts-user-code-runner';
import type { ChannelDictionary, ParameterDictionary } from '@nasa-jpl/aerie-ampcs';
import type { SimulatedActivity } from './lib/batchLoaders/simulatedActivityBatchLoader.js';
import type { CommandStem } from './lib/codegen/CommandEDSLPreface.js';
import type { SeqJson } from '@nasa-jpl/seq-json-schema/types';
import { deserializeWithTemporal } from './utils/temporalSerializers.js';
import { Result, SerializedResult } from '@nasa-jpl/aerie-ts-user-code-runner/build/utils/monads.js';

const temporalPolyfillTypes = fs.readFileSync(
  new URL('../src/types/TemporalPolyfillTypes.ts', import.meta.url).pathname,
  'utf8',
);

const channelDictionaryTypes: string = fs.readFileSync(
  new URL('./types/ChannelTypes.ts', import.meta.url).pathname,
  'utf-8',
);

const parameterDictionaryTypes: string = fs.readFileSync(
  new URL('./types/ParameterTypes.ts', import.meta.url).pathname,
  'utf-8',
);

const tsConfig = JSON.parse(fs.readFileSync(new URL('../tsconfig.json', import.meta.url).pathname, 'utf-8'));
const { options } = ts.parseJsonConfigFileContent(tsConfig, ts.sys, '');
const compilerTarget = options.target ?? ts.ScriptTarget.ES2021;

const codeRunner = new UserCodeRunner();

export async function typecheckExpansion(opts: {
  expansionLogic: string;
  commandTypes: string;
  activityTypes: string;
  activityTypeName?: string;
}): Promise<SerializedResult<CacheItem, ReturnType<UserCodeError['toJSON']>[]>> {
  const startTime = Date.now();
  console.log(
    `[ Worker ] started transpiling authoring logic ${opts.activityTypeName ? `- ${opts.activityTypeName}` : ''}`,
  );

  const result = await codeRunner.preProcess(
    opts.expansionLogic,
    'ExpansionReturn',
    [
      '{ activityInstance: ActivityType, channelDictionary: ChannelDictionary, parameterDictionaries: ParameterDictionary[] }',
    ],
    [
      ts.createSourceFile('command-types.ts', opts.commandTypes, compilerTarget),
      ts.createSourceFile('activity-types.ts', opts.activityTypes, compilerTarget),
      ts.createSourceFile('TemporalPolyfillTypes.ts', temporalPolyfillTypes, compilerTarget),
      ts.createSourceFile('ChannelTypes.ts', channelDictionaryTypes, compilerTarget),
      ts.createSourceFile('ParameterDictionaryTypes.ts', parameterDictionaryTypes, compilerTarget),
    ],
  );

  const endTime = Date.now();
  console.log(
    `[ Worker ] finished transpiling ${opts.activityTypeName ? `- ${opts.activityTypeName}` : ''}, (${
      (endTime - startTime) / 1000
    } s)`,
  );

  if (result.isOk()) {
    return Result.Ok(result.unwrap()).toJSON();
  } else {
    return Result.Err(result.unwrapErr().map(err => err.toJSON())).toJSON();
  }
}

export async function executeEDSL(opts: {
  edslBody: string;
  commandTypes: string;
}): Promise<SerializedResult<SeqJson, ReturnType<UserCodeError['toJSON']>[]>> {
  const result = await codeRunner.executeUserCode(
    opts.edslBody,
    [],
    'Sequence',
    [],
    1000,
    [
      ts.createSourceFile('command-types.ts', opts.commandTypes, compilerTarget),
      ts.createSourceFile('TemporalPolyfillTypes.ts', temporalPolyfillTypes, compilerTarget),
    ],
    vm.createContext({
      Temporal,
    }),
  );

  if (result.isOk()) {
    let sequence = result.unwrap() as Sequence;
    return Result.Ok(sequence.toSeqJson()).toJSON();
  } else {
    return Result.Err(result.unwrapErr().map(err => err.toJSON())).toJSON();
  }
}

export async function executeExpansionFromBuildArtifacts(opts: {
  buildArtifacts: CacheItem;
  channelData?: ChannelDictionary;
  parameterData: ParameterDictionary[];
  serializedActivityInstance: SimulatedActivity;
}): Promise<SerializedResult<ReturnType<CommandStem['toSeqJson']>[], ReturnType<UserCodeError['toJSON']>[]>> {
  const activityInstance = deserializeWithTemporal(opts.serializedActivityInstance) as SimulatedActivity;
  const result = await codeRunner.executeUserCodeFromArtifacts<
    [
      {
        activityInstance: SimulatedActivity;
        channelDictionary: ChannelDictionary | undefined;
        parameterDictionaries: ParameterDictionary[];
      },
    ],
    ExpansionReturn
  >(
    opts.buildArtifacts.jsFileMap,
    opts.buildArtifacts.userCodeSourceMap,
    [
      {
        activityInstance,
        channelDictionary: opts.channelData,
        parameterDictionaries: opts.parameterData,
      },
    ],
    3000,
    vm.createContext({
      Temporal,
    }),
  );

  if (result.isOk()) {
    let commands = result.unwrap();
    if (!Array.isArray(commands)) {
      commands = [commands];
    }
    let commandsFlat = commands.flat() as CommandStem[];
    commandsFlat = commandsFlat.map(command => {
      return command.METADATA({
        ...command.GET_METADATA(),
        simulatedActivityId: activityInstance.id,
      });
    });

    return Result.Ok(commandsFlat.map(c => c.toSeqJson())).toJSON();
  } else {
    return Result.Err(result.unwrapErr().map(err => err.toJSON())).toJSON();
  }
}
