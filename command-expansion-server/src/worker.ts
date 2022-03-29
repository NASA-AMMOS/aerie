import './polyfills.js';
import ts from 'typescript';
import vm from 'vm';
import { UserCodeError, UserCodeRunner } from '@nasa-jpl/aerie-ts-user-code-runner';
import {ActivityInstance} from "./lib/mapGraphQLActivityInstance.js";
import {Command} from "./lib/codegen/CommandEDSLPreface.js";
import * as fs from 'fs';
import getLogger from "./utils/logger.js";


const logger = getLogger("worker");

const temporalPolyfillTypes = fs.readFileSync(new URL('../src/TemporalPolyfillTypes.ts', import.meta.url).pathname, 'utf8');

export default async function executeExpansion(opts: {
  expansionLogic: string,
  activityInstance: ActivityInstance,
  commandTypes: string,
  activityTypes: string,
}): Promise<{
  activityInstance: ActivityInstance;
  commands: ReturnType<Command['toSeqJson']>[] | null;
  errors: ReturnType<UserCodeError['toJSON']>[];
}> {
  try {

    const result = await new UserCodeRunner().executeUserCode(
      opts.expansionLogic,
      [{
        activityInstance: opts.activityInstance
      }],
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

    return result.isOk()
      ? { activityInstance: opts.activityInstance, commands: result.unwrap(), errors: [] }
      : { activityInstance: opts.activityInstance, commands: null, errors: result.unwrapErr().map(err => err.toJSON()) };
  }
  catch (e: any) {
    logger.error(e);
    return { activityInstance: opts.activityInstance, commands: null, errors: [e?.message ?? "Unexpected error"] };
  }
}
