import vm from 'vm';
import ts from 'typescript';
import {SourceMapConsumer} from 'source-map';

// Fill in the missing module vm types
declare module 'vm' {
  export class Module {
    dependencySpecifiers: string[];
    error: any;
    identifier: string;
    namespace: unknown; // GetModuleNamespace;
    status: 'unlinked' | 'linking' | 'linked' | 'evaluating' | 'evaluated' | 'errored';

    evaluate(options?: { timeout?: number; breakOnSigInt?: boolean }): Promise<undefined>;

    link(
        linker: (
            specifier: string,
            extra: { assert?: { [key: string]: any } },
            referencingModule: vm.Module,
        ) => vm.Module | Promise<vm.Module>,
    ): void;
  }

  export class SourceTextModule extends Module {
    public constructor(
        code: string,
        options?: {
          identifier?: string;
          cachedData?: Buffer | NodeJS.TypedArray | DataView;
          context?: vm.Context;
          lineOffset?: number;
          columnOffset?: number;
          initializeImportMeta?: {
            meta?: any;
            module?: vm.SourceTextModule;
          };
          importModuleDynamically?: (specifier: string, importMeta: any) => Promise<vm.Module>;
        },
    );

    createCachedData(): Buffer;
  }
}

export default async function executeSourceCode(opts: { source: string, filename: string, dslTS: string, generatedCode: string }) {
  const compilerOptions = {
    module: ts.ModuleKind.ES2022,
      target: ts.ScriptTarget.ESNext,
      sourceMap: true,
  }
  const transpiledSource = ts.transpileModule(opts.source, {
    compilerOptions
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

  const transpiledModelSpecific = ts.transpileModule(opts.generatedCode, {
    compilerOptions
  });

  const contextifiedObject = vm.createContext({
    result: undefined
  });

  const dslJS = ts.transpileModule(opts.dslTS, {
    compilerOptions
  }).outputText

  const dslModule = new vm.SourceTextModule(dslJS, {context: contextifiedObject})

  const USER_FILE_ALIAS = 'USER_FILE_ALIAS';
  const harnessModule = new vm.SourceTextModule(`
    import * as dsl from 'dsl'
    import * as model from 'model-specific'
    import defaultExport from '${USER_FILE_ALIAS}';
    Object.assign(globalThis, dsl)
    Object.assign(globalThis, model)
    result = dsl.serializeGoal(defaultExport())
  `, {context: contextifiedObject})

  const moduleCache: {[name: string]: vm.Module } = {}
  moduleCache[USER_FILE_ALIAS] = new vm.SourceTextModule(transpiledSource.outputText, {context: contextifiedObject})
  moduleCache['model-specific'] = new vm.SourceTextModule(transpiledModelSpecific.outputText, {context: contextifiedObject})
  moduleCache['dsl'] = dslModule

  const linker = async (specifier: string) => {
    if (moduleCache.hasOwnProperty(specifier)) {
      return moduleCache[specifier];
    }
    throw new Error(`Unable to resolve dependency: ${specifier}`);
  }

  await harnessModule.link(linker);
  await harnessModule.evaluate({timeout: 10000})
  return contextifiedObject.result;
}
