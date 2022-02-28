import Piscina from 'piscina';
import * as AST from './libs/scheduler-ast.js';

async function main() {

  const workerUrl = new URL('./worker.js', import.meta.url);

  const piscina = new Piscina({
    filename: workerUrl.href,
  });

  process.stdin.on("data", async (data: Buffer) => {
    try {
      const { source, filename } = JSON.parse(data.toString()) as { source: string, filename: string };
      const result = await piscina.run({
        source,
        filename,
      }) as Promise<{ ast: AST.GoalSpecifier }>;
      let stringified = JSON.stringify(result);
      if (stringified === undefined) {
        throw Error(result + " was not JSON serializable")
      }
      process.stdout.write("success\n" + stringified + "\n");
    } catch (error: any) {
      process.stdout.write("error\n" + error.message + '\n');
    }
  });

}

void main();
