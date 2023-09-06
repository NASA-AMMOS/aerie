import fs, {Dirent} from 'fs';
import ts from 'typescript';
import * as readline from 'readline';
// @ts-ignore
import { Temporal } from '@js-temporal/polyfill';
import * as vm from "vm";
import {UserCodeRunner} from "@nasa-jpl/aerie-ts-user-code-runner";

const timelinePath = `/Users/Joelco/repos/aerie/timeline/src`;

function* readDir(root: string, path: string = ""): IterableIterator<{filename: string, contents: string}> {

  const entries:Dirent[] = fs.readdirSync(`${root}/${path}`, {withFileTypes: true});

  for (const entry of entries) {
    const entryPath = `${path}/${entry.name}`;

    if (entry.isFile()) {
      let contents = fs.readFileSync(`${root}/${entryPath}`, 'utf-8');
      const lastSlash = entryPath.lastIndexOf('/');
      const filename = entryPath.slice(1).slice(lastSlash === -1 ? 0 : lastSlash);

      // deal with it
      for (let i = 0; i < 15; i++) {
        contents = contents
            .replace("from '../", "from './")
            .replace('from "../', 'from "./')
            .replace("from './profiles/", "from './")
            .replace('from "./profiles/', 'from "./')
            .replace("from './spans/", "from './")
            .replace('from "./spans/', 'from "./');
      }
      yield {
        filename,
        contents
      };
    }

    if (entry.isDirectory()) {
      yield* readDir(root, entryPath);
    }
  }
}

const timelineSourceFiles = [...readDir(timelinePath)];

console.log(timelineSourceFiles.map($ => $.filename));

const codeRunner = new UserCodeRunner();
const tsConfig = JSON.parse(fs.readFileSync(new URL('../tsconfig.json', import.meta.url).pathname, 'utf-8'));
const { options } = ts.parseJsonConfigFileContent(tsConfig, ts.sys, '');
const compilerTarget = options.target ?? ts.ScriptTarget.Latest;

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

function toJson(unwrappedErr: any){
  var completeStackValue = "";
  if ('error' in unwrappedErr && 'stack' in unwrappedErr.error){
    completeStackValue = JSON.stringify(unwrappedErr.error.stack)
  }
  return {
    message: unwrappedErr.message,
    stack: unwrappedErr.stack,
    location: unwrappedErr.location,
    completeStack:  completeStackValue,
  }
}

async function handleRequest(data: Buffer) {
  try {
    // Test the health of the service by responding to "ping" with "pong".
    if (data.toString() === 'ping') {
      process.stdout.write('pong\n');
      lineReader.once('line', handleRequest);
      return;
    }
    const { constraintCode, missionModelGeneratedCode, expectedReturnType } = JSON.parse(data.toString()) as {
      constraintCode: string;
      missionModelGeneratedCode: string;
      expectedReturnType: string;
    };

    const additionalSourceFiles: { 'filename': string, 'contents': string}[] = timelineSourceFiles.concat([
      { 'filename': 'mission-model-generated-code.ts', 'contents': missionModelGeneratedCode },
    ]);

    const result = await codeRunner.executeUserCode<[], any>(
        constraintCode,
        [],
        expectedReturnType,
        [],
        10000,
        additionalSourceFiles.map(({filename, contents}) => ts.createSourceFile(
            filename,
            contents,
            compilerTarget
        )),
        vm.createContext({
          Temporal,
        }),
    );

    if (result.isErr()) {
      const secondLine = JSON.stringify(result.unwrapErr().map(err => toJson(err))) + '\n';
      process.stdout.write('error\n')
      process.stdout.write(secondLine);
      lineReader.once('line', handleRequest);
      return;
    }

    const stringified = JSON.stringify(
        result.unwrap(),
        function replacer(key, value) {
          if (this[key] instanceof Temporal.Duration) {
            return this[key].total({ unit: "microseconds" });
          }
          return value;
        }
    );
    if (stringified === undefined) {
      throw Error(JSON.stringify(result.unwrap()) + ' was not JSON serializable');
    }
    process.stdout.write('success\n')
    process.stdout.write(stringified + '\n');
  } catch (error: any) {
    process.stdout.write('panic\n');
    process.stdout.write(JSON.stringify(error.stack ?? error.message) + ' attempted to handle: ' + data.toString() + '\n');
  }
  lineReader.once('line', handleRequest);
}
