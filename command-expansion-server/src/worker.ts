import ts from 'typescript';
import vm from 'vm';
import { UserCodeRunner, UserCodeError } from '@nasa-jpl/aerie-ts-user-code-runner';
import {ActivityInstance} from "./lib/mapGraphQLActivityInstance";
import {Command} from "./lib/codegen/CommandEDSLPreface";

export type ExpansionResult = {
  activityInstance: ActivityInstance;
  commands: ReturnType<Command['toSeqJson']>[];
  errors: UserCodeError[];
}

export default async function executeExpansion(opts: {
  expansionLogic: string,
  activityInstance: ActivityInstance,
  commandTypes: string,
  activityTypes: string,
}): Promise<ExpansionResult> {
  try {
    const result = await new UserCodeRunner().executeUserCode(
      opts.expansionLogic,
      [{
        activity: opts.activityInstance
      }],
      'Command[] | Command | null',
      ['{ activity: ActivityType }'],
      3000,
      [
        ts.createSourceFile('command-types.ts', opts.commandTypes, ts.ScriptTarget.ES2021),
        ts.createSourceFile('activity-types.ts', opts.activityTypes, ts.ScriptTarget.ES2021),
      ],
      vm.createContext({ Temporal })
    );

    return result.isOk() ?
      { activityInstance: opts.activityInstance, commands: result.unwrap(), errors: [] } :
      { activityInstance: opts.activityInstance, commands: [], errors: result.unwrapErr() };
  }
  catch (e: any) {
    console.error(e);
    return { activityInstance: opts.activityInstance, commands: [], errors: [e?.message ?? "Unexpected error"] };
  }
}
