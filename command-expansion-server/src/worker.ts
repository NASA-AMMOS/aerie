import vm from 'vm';
import ts from 'typescript';
import {SourceMapConsumer} from 'source-map';
import type {Command} from './lib/CommandEDSLPreamble.js';

// TODO: Replace with something more informative
type ActivityInstance = {[key: string]: any};

export default async function executeCommandExpansion(opts: {
  source: string,
  filename: string,
  missionModelId: string,
  commandLibraryId: string,
}): Promise<{
  commands: Command[]
}> {

  // TODO: Pull this dynamically

  const harnessedCode = `${opts.source}

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
    const stackWithoutHarness = stack.filter(callsite => callsite.getFileName()?.endsWith(opts.filename))
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

  const activityInstance: ActivityInstance = {
  };

  let result = (await vm.runInNewContext(harnessedCode, {
    exports: {},
    $$props: {
      activityInstance,
    },
    $$context: {
    },
  }, {
    filename: opts.filename,
    timeout: 10000,
  }));

  if (Array.isArray(result)) {
    result = result.flat(Infinity);
  }

  return {
    commands: Array.isArray(result) ? result : [result],
  }
}
