import vm from 'vm';
import ts from 'typescript';
import {SourceMapConsumer} from 'source-map';
import * as schedulerApi from './libs/scheduler-edsl-fluent-api.js';

export default async function executeSourceCode(opts: { source: string, filename: string, generatedCode: string }) {
  const transpiledSource = ts.transpileModule(opts.generatedCode + "\n" + opts.source, {
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

  const harnessedCode = `${transpiledSource.outputText}
(async function () {
  return exports.default();
})()`;

  let result = (await vm.runInNewContext(harnessedCode, {
    exports: {},
    ...schedulerApi,
  }, {
    filename: opts.filename,
    timeout: 10000,
  })) as schedulerApi.Goal;

  return schedulerApi.serializeGoal(result);
}
