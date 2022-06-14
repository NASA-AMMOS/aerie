import fs from 'fs';
import ts from 'typescript';
import { UserCodeRunner } from '@nasa-jpl/aerie-ts-user-code-runner';
import type { Constraint } from './libs/constraints-edsl-fluent-api.js';
import * as readline from 'readline';

const codeRunner = new UserCodeRunner();
const constraintsEDSL = fs.readFileSync(
  `${process.env['CONSTRAINTS_DSL_COMPILER_ROOT']}/src/libs/constraints-edsl-fluent-api.ts`,
  'utf8',
);
const constraintsAST = fs.readFileSync(
  `${process.env['CONSTRAINTS_DSL_COMPILER_ROOT']}/src/libs/constraints-ast.ts`,
  'utf8',
);

process.on('uncaughtException', err => {
  console.error('uncaughtException');
  console.error(err && err.stack ? err.stack : err);
  process.stdout.write('panic\n' + err.stack ?? err.message);
  process.exit(1);
});

const lineReader = readline.createInterface({
  input: process.stdin,
});
lineReader.once('line', handleRequest);

async function handleRequest(data: Buffer) {
  try {
    // Test the health of the service by responding to "ping" with "pong".
    if (data.toString() === 'ping') {
      process.stdout.write('pong\n');
      lineReader.once('line', handleRequest);
      return;
    }
    const { constraintCode, missionModelGeneratedCode } = JSON.parse(data.toString()) as {
      constraintCode: string;
      missionModelGeneratedCode: string;
    };

    const result = await codeRunner.executeUserCode<[], Constraint>(constraintCode, [], 'Constraint', [], 10000, [
      ts.createSourceFile('constraints-ast.ts', constraintsAST, ts.ScriptTarget.ESNext),
      ts.createSourceFile('constraints-edsl-fluent-api.ts', constraintsEDSL, ts.ScriptTarget.ESNext),
      ts.createSourceFile('mission-model-generated-code.ts', missionModelGeneratedCode, ts.ScriptTarget.ESNext),
    ]);

    if (result.isErr()) {
      process.stdout.write('error\n' + JSON.stringify(result.unwrapErr().map(err => err.toJSON())) + '\n');
      lineReader.once('line', handleRequest);
      return;
    }

    const stringified = JSON.stringify(result.unwrap().__astNode);
    if (stringified === undefined) {
      throw Error(JSON.stringify(result.unwrap()) + ' was not JSON serializable');
    }
    process.stdout.write('success\n' + stringified + '\n');
  } catch (error: any) {
    process.stdout.write(
      'panic\n' + JSON.stringify(error.stack ?? error.message) + ' attempted to handle: ' + data.toString() + '\n',
    );
  }
  lineReader.once('line', handleRequest);
}
