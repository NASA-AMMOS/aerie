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

  const typescriptHwCommands: { value: string; interfaces: string }[] = [];
  for (const hwCommand of dictionary.hwCommands) {
    typescriptHwCommands.push(generateHwCommandCode(hwCommand));
  }

  // language=TypeScript
  const declarations = `
declare global {
${typescriptFswCommands.map(fswCommand => fswCommand.interfaces).join('\n')}${typescriptHwCommands
    .map(hwCommand => hwCommand.interfaces)
    .join('\n')}\n
\tconst Commands: {\n${dictionary.fswCommands
    .map(fswCommand => `\t\t${fswCommand.stem}: typeof ${fswCommand.stem}_STEP,\n`)
    .join('')}\t};

\tconst Hardwares : {\n${dictionary.hwCommands
    .map(hwCommand => `\t\t${hwCommand.stem}: typeof ${hwCommand.stem},\n`)
    .join('')} \t};
}`;

  // language=TypeScript
  const values = `
\nconst argumentOrders = {\n${dictionary.fswCommands
    .map(fswCommand => `\t'${fswCommand.stem}': [${generateArgOrder(fswCommand)}],\n`)
    .join('')}};

${typescriptFswCommands.map(fswCommand => fswCommand.value).join('\n')}
${typescriptHwCommands.map(hwCommands => hwCommands.value).join('\n')}\n
export const Commands = {\n${dictionary.fswCommands
    .map(fswCommand => `\t\t${fswCommand.stem}: ${fswCommand.stem}_STEP,\n`)
    .join('')}};

export const Immediates = {\n${dictionary.fswCommands
    .map(fswCommand => `\t\t${fswCommand.stem}: ${fswCommand.stem},\n`)
    .join('')}};

export const Hardwares = {\n${dictionary.hwCommands
    .map(hwCommands => `\t\t${hwCommands.stem}: ${hwCommands.stem},\n`)
    .join('')}};

Object.assign(globalThis, { A:A, R:R, E:E, C:Object.assign(Commands, STEPS), Sequence, VARIABLE, BUILD_LOCALS, BUILD_PARAMETERS, MAP_VARIABLES}, Hardwares, Immediates);
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
const ${fswCommandName}: ${fswCommandName}_IMMEDIATE = ImmediateStem.new({
\tstem: '${fswCommand.stem}',
\targuments: [],
});
const ${fswCommandName}_STEP: ${fswCommandName}_STEP = CommandStem.new({
\tstem: '${fswCommand.stem}',
\targuments: [],
});`;

    const interfaces = `
${doc}
\tinterface ${fswCommandName}_IMMEDIATE extends ImmediateStem<[]> {}
\tinterface ${fswCommandName}_STEP extends CommandStem<[]> {}
\tconst ${fswCommandName}: BAKE_BREAD_IMMEDIATE;
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
  return ImmediateStem.new({
    stem: '${fswCommandName}',
    arguments: args
  }) as ${fswCommandName}_IMMEDIATE;
}
function ${fswCommandName}_STEP(...args: [{ ${argsWithType
    .map(arg => arg.name + ': ' + arg.type + '| VariableDeclaration')
    .join(',')} }]) {
  return CommandStem.new({
    stem: '${fswCommandName}',
    arguments: sortCommandArguments(args, argumentOrders['${fswCommandName}'])
  }) as ${fswCommandName}_STEP;
}`;

  const interfaces = `
\tinterface ${fswCommandName}_IMMEDIATE extends ImmediateStem<[ [{ ${argsWithType
    .map(arg => arg.name + ': ' + arg.type)
    .join(',')} }] ]> {}
\tinterface ${fswCommandName}_STEP extends CommandStem<[ [{ ${argsWithType
    .map(arg => arg.name + ': ' + arg.type + '| VariableDeclaration')
    .join(',')} }] ]> {}
\tfunction ${fswCommandName}(...args: [{ ${argsWithType
    .map(arg => arg.name + ': ' + arg.type)
    .join(',')} }]) : ${fswCommandName}_IMMEDIATE`;

  return {
    value,
    interfaces,
  };
}

function generateHwCommandCode(hwCommand: ampcs.HwCommand): { value: string; interfaces: string } {
  const needsUnderscore =
    /^\d/.test(hwCommand.stem) ||
    reservedWords.check(hwCommand.stem) ||
    typescriptReservedWords.includes(hwCommand.stem);

  const hwCommandName = (needsUnderscore ? '_' : '') + hwCommand.stem;

  const doc = generateDoc(hwCommand);
  const value =
    `${doc}` +
    `\nconst ${hwCommandName}: ${hwCommandName} = HardwareStem.new({` +
    `\n\tstem: '${hwCommand.stem}'` +
    `\n})`;

  const interfaces =
    `\t\t${doc}` + `\n\tinterface ${hwCommandName} extends HardwareStem {}\n\tconst ${hwCommandName}: ${hwCommandName}`;
  return {
    value,
    interfaces,
  };
}

function generateArgOrder(fswCommand: ampcs.FswCommand): string[] {
  let argOrder = [];

  for (const argument of fswCommand.arguments) {
    argOrder.push("'" + argument.name + "'");

    if (argument.arg_type === 'repeat' && argument.repeat?.arguments) {
      for (const repeatArg of argument.repeat?.arguments) {
        argOrder.push("'" + repeatArg.name + "'");
      }
    }
  }

  return argOrder;
}

/**
 * Creates a jsdoc style doc for the given command. Right now it just includes the args as
 * parameters.
 *
 * @param command The command we're generating documentation for.
 * @returns The generated documentation.
 */
function generateDoc(command: ampcs.FswCommand | ampcs.HwCommand): string {
  let parameters: string[] = [];

  if ('arguments' in command) {
    command.arguments.forEach(arg => {
      parameters.push(`* @param ${arg.name} ${arg.description}`);
    });
  }

  return `
/**
* ${command.description}
${parameters.length > 0 ? parameters.join('\n') : '*'}
*/`;
}

function mapArgumentType(argument: ampcs.FswCommandArgument, enumMap: ampcs.EnumMap): string {
  switch (argument.arg_type) {
    case 'enum':
      return `(${enumMap[argument.enum_name]?.values.map(value => `'${value.symbol}'`).join(' | ')})`;
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
  const jsonSpecUrl = new URL('../../../node_modules/@nasa-jpl/seq-json-schema/types.ts', import.meta.url);
  const folder = `${getEnv().STORAGE}/${dictionary.header.mission_name.toLowerCase()}/`;
  const fileName = `command_lib.${dictionary.header.version}.ts`;
  const prefaceString = await fs.promises.readFile(prefaceUrl.pathname, 'utf8');
  const jsonSpecString = `/** START Sequence JSON Spec */
  //https://github.com/NASA-AMMOS/seq-json-schema/blob/develop/types.ts\n
  ${await fs.promises.readFile(jsonSpecUrl.pathname, 'utf8')}
  \n/** END Sequence JSON Spec */`;

  const { values, declarations } = generateTypescriptCode(dictionary); //update sql table store seprate

  await fs.promises.mkdir(folder, { recursive: true });

  await fs.promises.writeFile(folder + fileName, prefaceString + jsonSpecString + declarations + values, { flag: 'w' });
  return folder + fileName;
}
