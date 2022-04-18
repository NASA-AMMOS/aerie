import fs from 'fs';
import * as readline from 'readline';
import ts from 'typescript';
import { UserCodeRunner } from '@nasa-jpl/aerie-ts-user-code-runner';
import type { Goal } from './libs/scheduler-edsl-fluent-api.js';

const codeRunner = new UserCodeRunner();
const schedulerEDSL = fs.readFileSync(`${process.env.SCHEDULING_DSL_COMPILER_ROOT}/src/libs/scheduler-edsl-fluent-api.ts`, 'utf8');
const schedulerAST = fs.readFileSync(`${process.env.SCHEDULING_DSL_COMPILER_ROOT}/src/libs/scheduler-ast.ts`, 'utf8');

const input = readline.createInterface(process.stdin);

function registerInputCallback(callback: (data: string) => void) {
  input.once('line', callback);
}

function writeOutputOneLine(data: string) {
  process.stdout.write(data + '\n');
}

function writeSuccess(data: string) {
  writeOutputOneLine('success');
  writeOutputOneLine(data);
}

function writeError(data: string) {
  writeOutputOneLine('error');
  writeOutputOneLine(data);
}

function writePanic(data: string) {
  writeOutputOneLine('panic');
  writeOutputOneLine(data);
}

process.on('uncaughtException', (err) => {
  console.error('uncaughtException');
  console.error((err && err.stack) ? err.stack : err);
  writePanic(err.stack ?? err.message);
  process.exit(1);
});

async function handleRequest(data: string) {
  try {
    const { goalCode, missionModelGeneratedCode } = JSON.parse(data) as { goalCode: string, missionModelGeneratedCode: string };

    const result = await codeRunner.executeUserCode<[], Goal>(
        goalCode,
        [],
        'Goal',
        [],
        10000,
        [
          ts.createSourceFile('scheduler-ast.ts', schedulerAST, ts.ScriptTarget.ESNext),
          ts.createSourceFile('scheduler-edsl-fluent-api.ts', schedulerEDSL, ts.ScriptTarget.ESNext),
          ts.createSourceFile('mission-model-generated-code.ts', missionModelGeneratedCode, ts.ScriptTarget.ESNext),
        ],
    );

    if (result.isErr()) {
      writeError(JSON.stringify(result.unwrapErr().map(err => err.toJSON())));
      registerInputCallback(handleRequest);
      return;
    }

    // We're doing a string index in order to access the private method __serialize of Goal
    // The previous strategy of exporting a symbol and using it to get at a public method
    // didn't work because the symbol in the vm is different than the symbol we were using
    // here to deserialize the Goal due to different evaluation contexts
    const stringified = JSON.stringify(result.unwrap()['__serialize']());
    if (stringified === undefined) {
      throw Error(JSON.stringify(result.unwrap()) + ' was not JSON serializable');
    }
    writeSuccess(stringified);
  } catch (error: any) {
    writePanic(JSON.stringify(error.stack ?? error.message));
  }
  registerInputCallback(handleRequest);
}

registerInputCallback(data => {
  if (data.trim() === 'ping') {
    // Enable testing the health of the service by sending 'ping' and expecting 'pong' in return.
    writeOutputOneLine('pong');
    registerInputCallback(handleRequest);
  }
});

