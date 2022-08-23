import './polyfills.js';

import vm from 'node:vm';
import * as fs from 'node:fs';

import ts from 'typescript';
import { UserCodeError, UserCodeRunner } from '@nasa-jpl/aerie-ts-user-code-runner';

import getLogger from './utils/logger.js';
import type { SimulatedActivity } from './lib/batchLoaders/simulatedActivityBatchLoader.js';
import type { Command, SequenceSeqJson } from './lib/codegen/CommandEDSLPreface.js';
import type { Mutable } from './lib/codegen/CodegenHelpers';
import { deserializeWithTemporal } from './utils/temporalSerializers.js';

const logger = getLogger('worker');

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
}): Promise<{
  errors: ReturnType<UserCodeError['toJSON']>[];
}> {
  try {
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
      return { errors: [] };
    } else {
      return { errors: result.unwrapErr().map(e => e.toJSON()) };
    }
  } catch (e: any) {
    logger.error(e);
    return { errors: [e?.message ?? 'Unexpected error'] };
  }
}

export async function executeExpansion(opts: {
  expansionLogic: string;
  serializedActivityInstance: SimulatedActivity;
  commandTypes: string;
  activityTypes: string;
}): Promise<{
  activityInstance: SimulatedActivity;
  commands: ReturnType<Command['toSeqJson']>[] | null;
  errors: ReturnType<UserCodeError['toJSON']>[];
}> {
  const activityInstance = deserializeWithTemporal(opts.serializedActivityInstance) as SimulatedActivity;
  try {
    const result = await codeRunner.executeUserCode<[{ activityInstance: SimulatedActivity }], ExpansionReturn>(
      opts.expansionLogic,
      [
        {
          activityInstance,
        },
      ],
      'ExpansionReturn',
      ['{ activityInstance: ActivityType }'],
      3000,
      [
        ts.createSourceFile('command-types.ts', opts.commandTypes, compilerTarget),
        ts.createSourceFile('activity-types.ts', opts.activityTypes, compilerTarget),
        ts.createSourceFile('TemporalPolyfillTypes.ts', temporalPolyfillTypes, compilerTarget),
      ],
      vm.createContext({
        Temporal,
      }),
    );

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
      return { activityInstance, commands: commandsFlat.map(c => c.toSeqJson()), errors: [] };
    } else {
      return {
        activityInstance,
        commands: null,
        errors: result.unwrapErr().map(err => err.toJSON()),
      };
    }
  } catch (e: any) {
    logger.error(e);
    return { activityInstance, commands: null, errors: [e?.message ?? 'Unexpected error'] };
  }
}

export async function executeEDSL(opts: { edslBody: string; commandTypes: string }): Promise<{
  sequenceJson: SequenceSeqJson | null;
  errors: ReturnType<UserCodeError['toJSON']>[];
}> {
  try {
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
      return {
        sequenceJson: sequence.toSeqJson(),
        errors: [],
      };
    } else {
      return {
        sequenceJson: null,
        errors: result.unwrapErr().map(err => err.toJSON()),
      };
    }
  } catch (e: any) {
    logger.error(e);
    return { sequenceJson: null, errors: [e?.message ?? 'Unexpected error'] };
  }
}
