import vm from 'vm';
import ts from 'typescript';
import {SourceMapConsumer} from 'source-map';
import type {Command} from './lib/CommandEDSLPreamble.js';

// TODO: Replace with something more informative
type ActivityInstance = {[key: string]: any};

export default async function executeCommandExpansion(opts: {
  expansionLogic: string,
  activityTypeName: string,
  commandTypes: string,
  activityTypes: string,
  activityInstance: ActivityInstance,
}): Promise<{
  commands: ReturnType<Command['toSeqJson']>[]
}> {

  const harnessedCode = `${opts.expansionLogic}
${opts.commandTypes}
${opts.activityTypes}
(async function (): Promise<Command> {
  return exports.default($$props, $$context);
})()`;

  const transpiledSource = ts.transpileModule(harnessedCode, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ESNext,
      sourceMap: true,
    }
  });
  const sourceMapPromise = await new SourceMapConsumer(transpiledSource.sourceMapText as string);
  Error.prepareStackTrace = (error: Error, stack: NodeJS.CallSite[]) => {
    const stackWithoutHarness = stack.filter(callsite => callsite.getFileName()?.endsWith(opts.activityTypeName))
      .filter(callsite => {
        const mappedLocation = sourceMapPromise.originalPositionFor({
          line: callsite.getLineNumber()!,
          column: callsite.getColumnNumber()!,
        });
        return mappedLocation.line !== null;
      });

    const stackMessage = stackWithoutHarness
      .map(callsite => {
        const mappedLocation = sourceMapPromise.originalPositionFor({
          line: callsite.getLineNumber()!,
          column: callsite.getColumnNumber()!
        });
        const functionName = callsite.getFunctionName();
        const filename = callsite.getFileName();
        const lineNumber = mappedLocation.line;
        return '\tat ' + ((callsite as any).isAsync() ? 'async ' : '') + functionName + (filename ? ' (' + filename + ':' + lineNumber + ')' : '');
      })
      .join('\n');
    sourceMapPromise.destroy();
    return error.name + ': ' + error.message + '\n' + stackMessage;
  };

  let result: Command[] | Command = (await vm.runInNewContext(transpiledSource.outputText, {
    exports: {},
    $$props: {
      activityInstance: opts.activityInstance,
    },
    $$context: {
    },
  }, {
    filename: opts.activityTypeName,
    timeout: 10000,
  }));

  return {
    commands: Array.isArray(result)
        ? result.flat(Infinity).map(command => command.toSeqJson())
        : [result.toSeqJson()],
  }
}
