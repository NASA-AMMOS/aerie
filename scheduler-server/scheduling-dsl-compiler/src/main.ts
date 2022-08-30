import fs from 'fs';
import ts from 'typescript';
import {UserCodeRunner} from '@nasa-jpl/aerie-ts-user-code-runner';
import * as readline from 'readline';

const codeRunner = new UserCodeRunner();
const schedulerEDSL = fs.readFileSync(
  `${process.env["SCHEDULING_DSL_COMPILER_ROOT"]}/src/libs/scheduler-edsl-fluent-api.ts`,
  "utf8"
);
const schedulerAST = fs.readFileSync(
  `${process.env["SCHEDULING_DSL_COMPILER_ROOT"]}/src/libs/scheduler-ast.ts`,
  "utf8"
);
const windowsEDSL = fs.readFileSync(
  `${process.env["SCHEDULING_DSL_COMPILER_ROOT"]}/src/libs/constraints/constraints-edsl-fluent-api.ts`,
  "utf8"
);
const windowsAST = fs.readFileSync(
  `${process.env["SCHEDULING_DSL_COMPILER_ROOT"]}/src/libs/constraints/constraints-ast.ts`,
  "utf8"
);
const tsConfig = JSON.parse(fs.readFileSync(new URL('../tsconfig.json', import.meta.url).pathname, 'utf-8'));
const { options } = ts.parseJsonConfigFileContent(tsConfig, ts.sys, '');
const compilerTarget = options.target ?? ts.ScriptTarget.ES2021

process.on('uncaughtException', (err) => {
  console.error('uncaughtException');
  console.error((err && err.stack) ? err.stack : err);
  process.stdout.write('panic' + '\n');
  process.stdout.write((err.stack ?? err.message) + '\n');
  process.exit(1);
});

const lineReader = readline.createInterface({
  input: process.stdin,
});
lineReader.once('line', handleRequest);

interface AstNode {
  __astNode: object
}

async function handleRequest(data: Buffer) {
  try {
    // Test the health of the service by responding to "ping" with "pong".
    if (data.toString() === 'ping') {
      process.stdout.write('pong\n');
      lineReader.once('line', handleRequest);
      return;
    }
    const { goalCode, schedulerGeneratedCode, constraintsGeneratedCode, expectedReturnType } = JSON.parse(data.toString()) as {
      goalCode: string,
      schedulerGeneratedCode: string,
      constraintsGeneratedCode: string,
      expectedReturnType: string
    };

    const additionalSourceFiles: {'filename': string, 'contents': string}[] = [
      { 'filename': 'constraints-ast.ts', 'contents': windowsAST},
      { 'filename': 'constraints-edsl-fluent-api.ts', 'contents': windowsEDSL},
      { 'filename': 'mission-model-generated-code.ts', 'contents': constraintsGeneratedCode},
      { 'filename': 'scheduler-ast.ts', 'contents': schedulerAST},
      { 'filename': 'scheduler-edsl-fluent-api.ts', 'contents': schedulerEDSL},
      { 'filename': 'scheduler-mission-model-generated-code.ts', 'contents': schedulerGeneratedCode},
    ];

    const result = await codeRunner.executeUserCode<[], AstNode>(
        goalCode,
        [],
        expectedReturnType,
        [],
        10000,
        additionalSourceFiles.map(({filename, contents}) => ts.createSourceFile(filename, contents, compilerTarget))
    );

    if (result.isErr()) {
      process.stdout.write('error\n');
      process.stdout.write(JSON.stringify(result.unwrapErr().map(err => err.toJSON())) + '\n');
      lineReader.once('line', handleRequest);
      return;
    }

    const stringified = JSON.stringify(result.unwrap().__astNode);
    if (stringified === undefined) {
      throw Error(JSON.stringify(result.unwrap().__astNode) + ' was not JSON serializable');
    }
    process.stdout.write('success\n');
    process.stdout.write(stringified + '\n');
  } catch (error: any) {
    process.stdout.write('panic\n');
    process.stdout.write(JSON.stringify(error.stack ?? error.message) + '\n');
  }
  lineReader.once('line', handleRequest);
}
