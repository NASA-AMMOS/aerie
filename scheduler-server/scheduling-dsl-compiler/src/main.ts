import executeSourceCode from "./executor.js";
import fs from "fs";

process.on('uncaughtException', (err) => {
  console.error("uncaughtException")
  console.error((err && err.stack) ? err.stack : err);
  process.stdout.write("panic\n" + err.stack);
  process.exit(1)
});

async function handleRequest(data: Buffer) {
  try {
    const { source, filename, generatedCode } = JSON.parse(data.toString()) as { source: string, filename: string, generatedCode: string };
    const result = await executeSourceCode({
      source,
      filename,
      generatedCode,
      dslTS: (await fs.promises.readFile(`${process.env.SCHEDULING_DSL_COMPILER_ROOT}/src/libs/scheduler-edsl-fluent-api.ts`)).toString()
    })
    const stringified = JSON.stringify(result);
    if (stringified === undefined) {
      throw Error(result + " was not JSON serializable")
    }
    process.stdout.write("success\n" + stringified + "\n");
  } catch (error: any) {
    process.stdout.write("error\n" + error.message + '\n');
  }
  process.stdin.once("data", handleRequest);
}

process.stdin.once("data", data => {
  if (data.toString().trim() === "ping") {
    // Enable testing the health of the service by sending "ping" and expecting "pong" in return.
    process.stdout.write("pong\n");
    process.stdin.once("data", handleRequest);
  }
})

