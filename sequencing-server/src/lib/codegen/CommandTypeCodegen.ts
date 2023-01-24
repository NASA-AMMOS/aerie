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
  const typescriptFswCommands: { value: string; interfaces: string }[] = [];
  for (const fswCommand of dictionary.fswCommands) {
    typescriptFswCommands.push(generateFswCommandCode(fswCommand, dictionary.enumMap));
  }

  // language=TypeScript
  const declarations = `
declare global {
${typescriptFswCommands.map(fswCommand => fswCommand.interfaces).join('\n')}
\tconst Commands: {\n${dictionary.fswCommands
    .map(fswCommand => `\t\t${fswCommand.stem}: typeof ${fswCommand.stem},\n`)
    .join('')}\t};
}`;

  // language=TypeScript
  const values = `
${typescriptFswCommands.map(fswCommand => fswCommand.value).join('\n')}
export const Commands = {${dictionary.fswCommands
    .map(fswCommand => `\t\t${fswCommand.stem}: ${fswCommand.stem},\n`)
    .join('')}};

Object.assign(globalThis, { A:A, R:R, E:E, C:Commands, Sequence});
`;

  return {
    declarations,
    values,
  };
}

function generateFswCommandCode(
  fswCommand: ampcs.FswCommand,
  enumMap: ampcs.EnumMap,
): { value: string; interfaces: string } {
  const needsUnderscore =
    /^\d/.test(fswCommand.stem) ||
    reservedWords.check(fswCommand.stem) ||
    typescriptReservedWords.includes(fswCommand.stem);

  const fswCommandName = (needsUnderscore ? '_' : '') + fswCommand.stem;
  const numberOfArguments = fswCommand.arguments.length;

  const doc = generateDoc(fswCommand);

  if (numberOfArguments === 0) {
    // language=TypeScript
    const value = `
${doc}
const ${fswCommandName}: ${fswCommandName} = Command.new({
\tstem: '${fswCommand.stem}',
\targuments: [],
})`;

    const interfaces = `
${doc}
\tinterface ${fswCommandName} extends Command<[]> {}
`;
    return {
      value,
      interfaces,
    };
  }

  let argsWithType: Array<{ name: string; type: string }> = [];

  fswCommand.arguments.forEach(arg => {
    // If we come across a repeat arg we need to make an array type of all the repeats arguments.
    if (arg.arg_type === 'repeat' && arg.repeat) {
      let repeatArgs: Array<{ name: string; type: string }> = [];

      repeatArgs = repeatArgs.concat(
        arg.repeat.arguments.map(repeatArg => {
          return { name: `${repeatArg.name}`, type: mapArgumentType(repeatArg, enumMap) };
        }),
      );

      argsWithType = argsWithType.concat({
        name: `'${arg.name}'`,
        type: `Array<{ ${repeatArgs.map(repeatArg => `'${repeatArg.name}': ${repeatArg.type}`).join(', ')} }>`,
      });
    } else {
      // Otherwise we just have a normal arg with a type.
      argsWithType = argsWithType.concat({
        name: `'${arg.name}'`,
        type: mapArgumentType(arg, enumMap),
      });
    }
  });

  const value = `
${doc}
function ${fswCommandName}(...args: [{ ${argsWithType.map(arg => arg.name + ': ' + arg.type).join(',')} }]) {
  return Command.new({
    stem: '${fswCommandName}',
    arguments: args
  }) as ${fswCommandName};
}`;

  const interfaces = `
\tinterface ${fswCommandName} extends Command<[ [{ ${argsWithType
    .map(arg => arg.name + ': ' + arg.type)
    .join(',')} }] ]> {}`;

  return {
    value,
    interfaces,
  };
}

/**
 * Creates a jsdoc style doc for the given command. Right now it just includes the args as
 * parameters.
 *
 * @param fswCommand The command we're generating documentation for.
 * @returns The generated documentation.
 */
function generateDoc(fswCommand: ampcs.FswCommand): string {
  let parameters: string[] = [];

  fswCommand.arguments.forEach(arg => {
    parameters.push(`* @param ${arg.name} ${arg.description}`);
  });

  return `
/**
* ${fswCommand.description}
${parameters.length > 0 ? parameters.join('\n') : '*'}
*/`;
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
