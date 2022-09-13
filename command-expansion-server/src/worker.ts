import './polyfills.js';

import vm from 'node:vm';
import * as fs from 'node:fs';

import ts from 'typescript';
import { CacheItem, UserCodeError, UserCodeRunner } from '@nasa-jpl/aerie-ts-user-code-runner';

import type { SimulatedActivity } from './lib/batchLoaders/simulatedActivityBatchLoader.js';
import type { Command, SequenceSeqJson } from './lib/codegen/CommandEDSLPreface.js';
import type { Mutable } from './lib/codegen/CodegenHelpers';
import { deserializeWithTemporal } from './utils/temporalSerializers.js';
import { Result, SerializedResult } from '@nasa-jpl/aerie-ts-user-code-runner/build/utils/monads.js';

const temporalPolyfillTypes = fs.readFileSync(
  new URL('../src/TemporalPolyfillTypes.ts', import.meta.url).pathname,
  'utf8',
);
const tsConfig = JSON.parse(fs.readFileSync(new URL('../tsconfig.json', import.meta.url).pathname, 'utf-8'));
const { options } = ts.parseJsonConfigFileContent(tsConfig, ts.sys, '');
const compilerTarget = options.target ?? ts.ScriptTarget.ES2021;

const codeRunner = new UserCodeRunner();

export async function typecheckExpansion(opts: {
  expansionLogic: string;
  commandTypes: string;
  activityTypes: string;
}): Promise<SerializedResult<
    CacheItem,
    ReturnType<UserCodeError['toJSON']>[]
>> {
  const result = await codeRunner.preProcess(
    opts.expansionLogic,
    'ExpansionReturn',
    ['{ activityInstance: ActivityType }'],
    [
      ts.createSourceFile('command-types.ts', opts.commandTypes, compilerTarget),
      ts.createSourceFile('activity-types.ts', opts.activityTypes, compilerTarget),
      ts.createSourceFile('TemporalPolyfillTypes.ts', temporalPolyfillTypes, compilerTarget),
    ],
  );
  if (result.isOk()) {
    return Result.Ok(result.unwrap()).toJSON();
  }
  else {
    return Result.Err(result.unwrapErr().map(err => err.toJSON())).toJSON();
  }
}

export async function executeEDSL(opts: {
  edslBody: string;
  commandTypes: string,
}): Promise<SerializedResult<
  SequenceSeqJson,
  ReturnType<UserCodeError['toJSON']>[]
>> {
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
  }
  else {
    return Result.Err(result.unwrapErr().map(err => err.toJSON())).toJSON();
  }
}

export async function executeExpansionFromBuildArtifacts(opts: {
  buildArtifacts: CacheItem;
  serializedActivityInstance: SimulatedActivity;
}): Promise<SerializedResult<
    ReturnType<Command['toSeqJson']>[],
    ReturnType<UserCodeError['toJSON']>[]
    >> {
  const activityInstance = deserializeWithTemporal(opts.serializedActivityInstance) as SimulatedActivity;
  const result = await codeRunner.executeUserCodeFromArtifacts<
      [{
        activityInstance: SimulatedActivity;
      }],
      ExpansionReturn
      >(
      opts.buildArtifacts.jsFileMap,
      opts.buildArtifacts.userCodeSourceMap,
      [
        {
          activityInstance,
        },
      ],
      3000,
      vm.createContext({
        Temporal,
      }),
  )

  if (result.isOk()) {
    let commands = result.unwrap();
    if (!Array.isArray(commands)) {
      commands = [commands];
    }
    const commandsFlat = commands.flat() as Command[];
    for (const command of commandsFlat) {
      (command as Mutable<Command>).metadata = {
        ...command.metadata,
        simulatedActivityId: activityInstance.id,
      };
    }
    return Result.Ok(commandsFlat.map(c => c.toSeqJson())).toJSON();
  }
  else {
    return Result.Err(result.unwrapErr().map(err => err.toJSON())).toJSON();
  }
}
