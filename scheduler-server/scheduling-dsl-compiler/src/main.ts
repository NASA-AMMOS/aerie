import fs from 'fs';
import * as readline from 'readline';
import ts from 'typescript';
import { UserCodeRunner } from '@nasa-jpl/aerie-ts-user-code-runner';
import type { Goal } from './libs/scheduler-edsl-fluent-api.js';

const codeRunner = new UserCodeRunner();
const schedulerEDSL = fs.readFileSync(`${process.env.SCHEDULING_DSL_COMPILER_ROOT}/src/libs/scheduler-edsl-fluent-api.ts`, 'utf8');
const schedulerAST = fs.readFileSync(`${process.env.SCHEDULING_DSL_COMPILER_ROOT}/src/libs/scheduler-ast.ts`, 'utf8');
const windowsEDSL = fs.readFileSync(`${process.env.SCHEDULING_DSL_COMPILER_ROOT}/src/libs/constraints/constraints-edsl-fluent-api.ts`, 'utf8');
const windowsAST = fs.readFileSync(`${process.env.SCHEDULING_DSL_COMPILER_ROOT}/src/libs/constraints/constraints-ast.ts`, 'utf8');

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
          ts.createSourceFile('constraints-ast.ts', windowsAST, ts.ScriptTarget.ESNext),
          ts.createSourceFile('constraints-edsl-fluent-api.ts', windowsEDSL, ts.ScriptTarget.ESNext),
          ts.createSourceFile('scheduler-ast.ts', schedulerAST, ts.ScriptTarget.ESNext),
          ts.createSourceFile('scheduler-edsl-fluent-api.ts', schedulerEDSL, ts.ScriptTarget.ESNext),
          ts.createSourceFile('scheduler-mission-model-generated-code.ts', missionModelGeneratedCode, ts.ScriptTarget.ESNext),
        ],
    );

    if (result.isErr()) {
      writeError(JSON.stringify(result.unwrapErr().map(err => err.toJSON())));
      registerInputCallback(handleRequest);
      return;
    }

    const stringified = JSON.stringify(result.unwrap().__astNode);
    if (stringified === undefined) {
      throw Error(JSON.stringify(result.unwrap().__astNode) + ' was not JSON serializable');
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

