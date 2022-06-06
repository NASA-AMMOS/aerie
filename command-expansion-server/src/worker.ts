import './polyfills.js';
import ts from 'typescript';
import vm from 'vm';
import { UserCodeError, UserCodeRunner } from '@nasa-jpl/aerie-ts-user-code-runner';
import type { SimulatedActivity } from './lib/batchLoaders/simulatedActivityBatchLoader';
import type { Command } from './lib/codegen/CommandEDSLPreface.js';
import * as fs from 'fs';
import getLogger from './utils/logger.js';
import type { Mutable } from './lib/codegen/CodegenHelpers';

const logger = getLogger('worker');

const temporalPolyfillTypes = fs.readFileSync(
  new URL('../src/TemporalPolyfillTypes.ts', import.meta.url).pathname,
  'utf8',
);
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
        ts.createSourceFile('command-types.ts', opts.commandTypes, ts.ScriptTarget.ES2021),
        ts.createSourceFile('activity-types.ts', opts.activityTypes, ts.ScriptTarget.ES2021),
        ts.createSourceFile('TemporalPolyfillTypes.ts', temporalPolyfillTypes, ts.ScriptTarget.ES2021),
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
  activityInstance: SimulatedActivity;
  commandTypes: string;
  activityTypes: string;
}): Promise<{
  activityInstance: SimulatedActivity;
  commands: ReturnType<Command['toSeqJson']>[] | null;
  errors: ReturnType<UserCodeError['toJSON']>[];
}> {
  try {
    const result = await codeRunner.executeUserCode<[{ activityInstance: SimulatedActivity }], ExpansionReturn>(
      opts.expansionLogic,
      [
        {
          activityInstance: opts.activityInstance,
        },
      ],
      'ExpansionReturn',
      ['{ activityInstance: ActivityType }'],
      3000,
      [
        ts.createSourceFile('command-types.ts', opts.commandTypes, ts.ScriptTarget.ES2021),
        ts.createSourceFile('activity-types.ts', opts.activityTypes, ts.ScriptTarget.ES2021),
        ts.createSourceFile('TemporalPolyfillTypes.ts', temporalPolyfillTypes, ts.ScriptTarget.ES2021),
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
        (command as Mutable<Command>).metadata  = {
          ...command.metadata,
          simulatedActivityId: opts.activityInstance.id,
        };
      }
      return { activityInstance: opts.activityInstance, commands: commandsFlat.map(c => c.toSeqJson()), errors: [] };
    } else {
      return {
        activityInstance: opts.activityInstance,
        commands: null,
        errors: result.unwrapErr().map(err => err.toJSON()),
      };
    }
  } catch (e: any) {
    logger.error(e);
    return { activityInstance: opts.activityInstance, commands: null, errors: [e?.message ?? 'Unexpected error'] };
  }
}
