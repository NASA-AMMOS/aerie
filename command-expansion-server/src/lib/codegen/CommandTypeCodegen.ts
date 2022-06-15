// Language: TypeScript
// Path: src/libs/CommandTypeCodegen.ts

import type * as ampcs from '@nasa-jpl/aerie-ampcs';
import fs from 'fs';
import reservedWords from 'reserved-words';
import { getEnv } from '../../env.js';

const typescriptReservedWords = ['boolean', 'string', 'number'];

function generateTypescriptCode(dictionary: ampcs.CommandDictionary): {
  declarations: string;
  values: string;
} {
  const typescriptFswCommands: { value: string; declaration: string }[] = [];
  for (const fswCommand of dictionary.fswCommands) {
    typescriptFswCommands.push(generateFswCommandCode(fswCommand, dictionary.enumMap));
  }

  // language=TypeScript
  const declarations = `
declare global {
${typescriptFswCommands.map(fswCommand => fswCommand.declaration).join('\n')}
\tconst Commands: {\n${dictionary.fswCommands
    .map(fswCommand => `\t\t${fswCommand.stem}: ${fswCommand.stem},\n`)
    .join('')}\t};
}`;

  // language=TypeScript
  const values = `
${typescriptFswCommands.map(fswCommand => fswCommand.value).join('\n')}
export const Commands = {${dictionary.fswCommands
    .map(fswCommand => `\t\t${fswCommand.stem}: ${fswCommand.stem},\n`)
    .join('')}};

Object.assign(globalThis, Commands);
`;

  return {
    declarations,
    values,
  };
}

function generateFswCommandCode(
  fswCommand: ampcs.FswCommand,
  enumMap: ampcs.EnumMap,
): { value: string; declaration: string } {
  const needsUnderscore =
    /^\d/.test(fswCommand.stem) ||
    reservedWords.check(fswCommand.stem) ||
    typescriptReservedWords.includes(fswCommand.stem);

  const fswCommandName = (needsUnderscore ? '_' : '') + fswCommand.stem;
  const numberOfArguments = fswCommand.arguments.length;

  const hasRepeatedArgs = fswCommand.arguments.some(arg => arg.arg_type === 'repeat');

  const doc = `
\t/**${fswCommand.description}*/`;

  if (numberOfArguments === 0) {
    // language=TypeScript
    const value = `
${doc}
export const ${fswCommandName}: ${fswCommandName} = Command.new({
\tstem: '${fswCommand.stem}',
\targuments: [],
})`;
    // language=TypeScript
    const declaration = `
${doc}
\tconst ${fswCommandName}: ${fswCommandName};
\tinterface ${fswCommandName} extends Command<[]> {}
`;
    return {
      value,
      declaration,
    };
  }

  if (hasRepeatedArgs) {
    const repeatArg = fswCommand.arguments.find(arg => arg.arg_type === 'repeat')! as ampcs.FswCommandArgumentRepeat;
    const otherArgs = fswCommand.arguments.filter(arg => arg.arg_type !== 'repeat');
    const minRepeat = repeatArg.repeat?.min ?? 0;
    const maxRepeat = repeatArg.repeat?.max ?? 10;

    // language=TypeScript
    const value = `
${doc}
export function ${fswCommandName}<T extends any[]>(args: T[]) {
  return Command.new({
    stem: '${fswCommandName}',
    arguments: typeof args[0] === 'object' ? orderCommandArguments(args[0],${fswCommandName}_ARGS_ORDER) : args,
  }) as ${fswCommandName};
}`;

    const overloadDeclarations: string[] = [];
    for (let i = minRepeat; i <= maxRepeat; i++) {
      // language=TypeScript
      let repeatArgsDeclaration = '';
      for (let n = 1; n < i; n++) {
        repeatArgsDeclaration += repeatArg.repeat?.arguments
          .map(arg => `\t${arg.name}${n}: ${mapArgumentType(arg, enumMap)},\n`)
          .join('');
      }
      // language=TypeScript
      const overloadPositionalDeclaration = `
${doc}
\tfunction ${fswCommandName}(
${repeatArgsDeclaration}${otherArgs.map(arg => `\t${arg.name}: ${mapArgumentType(arg, enumMap)}, \n`).join('')}
\t): ${fswCommandName};`;
      // language=TypeScript
      const overloadNamedDeclaration = `
${doc}
\tfunction ${fswCommandName}(args: {
${repeatArgsDeclaration}${otherArgs.map(arg => `\t${arg.name}: ${mapArgumentType(arg, enumMap)}, \n`).join('')}
\t}): ${fswCommandName};`;
      overloadDeclarations.push(overloadPositionalDeclaration);
      overloadDeclarations.push(overloadNamedDeclaration);
    }
    const declaration = `
${overloadDeclarations.join('')}
\tinterface ${fswCommandName} extends Command<any[]> {}`;
    return {
      value,
      declaration,
    };
  }
  // language=TypeScript

  // language=TypeScript
  const value = `
${doc}
const ${fswCommandName}_ARGS_ORDER = [${fswCommand.arguments.map(argument => `'${argument.name}'`).join(', ')}];
export function ${fswCommandName}(...args: [\n${fswCommand.arguments
    .map(argument => (argument.arg_type === 'repeat' ? '' : `\t${mapArgumentType(argument, enumMap)},\n`))
    .join('')}] | [{\n${fswCommand.arguments
    .map(argument =>
      argument.arg_type === 'repeat' ? '' : `\t${argument.name}: ${mapArgumentType(argument, enumMap)},\n`,
    )
    .join('')}}]): ${fswCommandName} {
  return Command.new({
    stem: '${fswCommand.stem}',
    arguments: typeof args[0] === 'object' ? orderCommandArguments(args[0],${fswCommandName}_ARGS_ORDER) : args,
  }) as ${fswCommandName};
}`;

  // language=TypeScript
  const declaration = `
${doc}
\tfunction ${fswCommandName}(\n${fswCommand.arguments
    .map(argument =>
      argument.arg_type === 'repeat' ? '' : `\t\t${argument.name}: ${mapArgumentType(argument, enumMap)},\n`,
    )
    .join('')}\t): ${fswCommandName};
${doc}
\tfunction ${fswCommandName}(args: {\n${fswCommand.arguments
    .map(argument =>
      argument.arg_type === 'repeat' ? '' : `\t\t${argument.name}: ${mapArgumentType(argument, enumMap)},\n`,
    )
    .join('')}\t}): ${fswCommandName};
\tinterface ${fswCommandName} extends Command<[\n${fswCommand.arguments
    .map(argument => (argument.arg_type === 'repeat' ? '' : `\t\t${mapArgumentType(argument, enumMap)},\n`))
    .join('')}\t] | {\n${fswCommand.arguments
    .map(argument =>
      argument.arg_type === 'repeat' ? '' : `\t\t${argument.name}: ${mapArgumentType(argument, enumMap)},\n`,
    )
    .join('')}\t}> {}`;

  return {
    value,
    declaration,
  };
}

function mapArgumentType(argument: ampcs.FswCommandArgument, enumMap: ampcs.EnumMap): string {
  switch (argument.arg_type) {
    case 'enum':
      // boolean enum shouldn't be 'TRUE | FALSE' but of `boolean` type
      if (
        enumMap[argument.enum_name]?.values.length === 2 &&
        enumMap[argument.enum_name]?.values.some(({ symbol }) => symbol.toLocaleLowerCase() === 'true') &&
        enumMap[argument.enum_name]?.values.some(({ symbol }) => symbol.toLocaleLowerCase() === 'false')
      ) {
        return 'boolean';
      } else {
        return `(${enumMap[argument.enum_name]?.values.map(value => `'${value.symbol}'`).join(' | ')})`;
      }
    case 'boolean':
      return 'boolean';
    case 'float':
      return `F${argument.bit_length}`;
    case 'unsigned':
      return `U${argument.bit_length}`;
    case 'integer':
      return `I${argument.bit_length}`;
    case 'var_string':
      return `VarString<${argument.prefix_bit_length}, ${argument.max_bit_length}>`;
    case 'fixed_string':
      return 'FixedString';
    case 'time':
      return 'string';
    default:
      throw new Error(`Unsupported argument type: ${argument.arg_type}`);
  }
}

export async function processDictionary(dictionary: ampcs.CommandDictionary) {
  const prefaceUrl = new URL('./CommandEDSLPreface.ts', import.meta.url);
  const folder = `${getEnv().STORAGE}/${dictionary.header.mission_name.toLowerCase()}/`;
  const fileName = `command_lib.${dictionary.header.version}.ts`;
  const prefaceString = await fs.promises.readFile(prefaceUrl.pathname, 'utf8');

  const { values, declarations } = generateTypescriptCode(dictionary); //update sql table store seprate

  await fs.promises.mkdir(folder, { recursive: true });

  await fs.promises.writeFile(folder + fileName, prefaceString + declarations + values, { flag: 'w' });
  return folder + fileName;
}
