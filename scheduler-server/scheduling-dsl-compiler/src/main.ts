import Piscina from 'piscina';
import * as fs from 'fs'
import * as AST from './libs/scheduler-ast.js';

async function main() {

  var access = fs.createWriteStream('scheduling-dsl-compiler-log.txt', {flags: 'a'});

  // @ts-ignore
  process.stderr.write = access.write.bind(access);

  console.error("Initial log message")

  process.on('uncaughtException', function(err) {
    console.error("uncaughtException")
    console.error((err && err.stack) ? err.stack : err);
  });

  const workerUrl = new URL('./worker.js', import.meta.url);

  const piscina = new Piscina({
    filename: workerUrl.href,
  });

  process.stdin.on("data", async (data: Buffer) => {
    console.error("data: " + data.toString().trim())
    try {
      if (data.toString().trim() === "ping") {
        process.stdout.write("pong\n");
        return
      }
      const { source, filename } = JSON.parse(data.toString()) as { source: string, filename: string };
      const result = await piscina.run({
        source,
        filename,
      }) as Promise<{ ast: AST.GoalSpecifier }>;
      let stringified = JSON.stringify(result);
      if (stringified === undefined) {
        throw Error(result + " was not JSON serializable")
      }
      console.error("success\n" + stringified + "\n")
      process.stdout.write("success\n" + stringified + "\n");
    } catch (error: any) {
      console.error("error\n" + error.message + '\n')
      process.stdout.write("error\n" + error.message + '\n');
    }
  });

}

void main();
