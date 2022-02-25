import * as fs from "fs";
import ts from "typescript";

async function main() {
  const args = process.argv.slice(2)
  const inFile = args[0]
  console.assert(args[1] === "--outfile")
  const outFile = args[2]
  let compilerOptions = {
    module: ts.ModuleKind.CommonJS,
    target: ts.ScriptTarget.ES5,
    sourceMap: true,
  };
  const transpiledProgram = ts.createProgram([inFile], compilerOptions);
  const allDiagnostics = [
    ...transpiledProgram.getSyntacticDiagnostics(),
    ...transpiledProgram.getSemanticDiagnostics(),
    ...transpiledProgram.getGlobalDiagnostics(),
    ...transpiledProgram.getDeclarationDiagnostics(),
    ...transpiledProgram.getOptionsDiagnostics(),
    ...transpiledProgram.getConfigFileParsingDiagnostics()
  ]
  for (const diagnostic of allDiagnostics) {
    console.log(`error TS${diagnostic.code}: ${diagnostic.messageText}`)
  }
  if (allDiagnostics.length > 0) {
    process.exit(1)
  }
  const source = await fs.promises.readFile(inFile, 'utf8');
  await fs.promises.writeFile(outFile, ts.transpileModule(source, {compilerOptions}).outputText)
}

void main();
