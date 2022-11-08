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

  const hasRepeatedArgs = fswCommand.arguments.some(arg => arg.arg_type === 'repeat');

  const doc = `
\t/**${fswCommand.description}*/`;

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

  if (hasRepeatedArgs) {
    const repeatArg = fswCommand.arguments.find(arg => arg.arg_type === 'repeat')! as ampcs.FswCommandArgumentRepeat;
    const otherArgs = fswCommand.arguments.filter(arg => arg.arg_type !== 'repeat');
    const minRepeat = repeatArg.repeat?.min ?? 0;
    const maxRepeat = repeatArg.repeat?.max ?? 10;

    let repeatArgsDeclaration: Array<{ name: string; type: string }>[] = [];
    let methodParameters: string[] = [];
    let interfaceParameters: string[] = [];
    let argsOrder: string[] = [];

    for (let i = minRepeat; i <= maxRepeat; i++) {
      let repeatArgs: Array<{ name: string; type: string }> = [];

      if (repeatArg.repeat) {
        for (let n = 1; n < i; n++) {
          repeatArgs = repeatArgs.concat(
            repeatArg.repeat.arguments.map(arg => {
              return { name: `${arg.name}_${n}`, type: mapArgumentType(arg, enumMap) };
            }),
          );
        }
      }
      repeatArgsDeclaration.push(repeatArgs);
    }

    repeatArgsDeclaration.forEach(repeat => {
      const repeatArgNameAndType = `${repeat.map(arg => `\t'${arg.name}': ${arg.type}`).join(', ')}`;
      const repeatOtherNameAndType = `${otherArgs
        .map(arg => `\t'${arg.name}': ${mapArgumentType(arg, enumMap)}`)
        .join(',')}`;
      const repeatArgType = `${repeat.map(arg => `${arg.type}`).join(', ')}`;
      const repeatOtherType = `${otherArgs.map(arg => `${mapArgumentType(arg, enumMap)}`).join(', ')}`;

      methodParameters = methodParameters.concat(
        `[${repeatArgType}${repeatArgType !== '' ? ', ' : ''}${repeatOtherType}] \n|[{${repeatArgNameAndType}${
          repeatArgNameAndType !== '' ? ', ' : ''
        }${repeatOtherNameAndType}}]\n`,
      );
      interfaceParameters = interfaceParameters.concat(
        `[${repeatArgType}${repeatArgType !== '' ? ', ' : ''}${repeatOtherType}] \n|[{${repeatArgNameAndType}${
          repeatArgNameAndType !== '' ? ', ' : ''
        }${repeatOtherNameAndType}}]\n`,
      );

      argsOrder = argsOrder.concat(
        `[${repeat.map(arg => `'${arg.name}'`).concat(otherArgs.map(arg => `'${arg.name}'`))}]`,
      );
    });

    const value = `
const ${fswCommandName}_ARGS_ORDERS = [${argsOrder.join(',')}];
${doc}
function ${fswCommandName}(...args:\n ${methodParameters.join('|')}) {
  return Command.new({
    stem: '${fswCommandName}',
    arguments: typeof args[0] === 'object' ? findAndOrderCommandArguments("${fswCommandName}",args[0],${fswCommandName}_ARGS_ORDERS) : args,
  }) as ${fswCommandName};
}`;

    const interfaces = `
\tinterface ${fswCommandName} extends Command<[\n${interfaceParameters.join('|')}]> {}`;
    return {
      value,
      interfaces,
    };
  }

  const value = `
const ${fswCommandName}_ARGS_ORDER = [${fswCommand.arguments.map(argument => `'${argument.name}'`).join(', ')}];
${doc}
function ${fswCommandName}(...args: [\n${fswCommand.arguments
    .map(argument => (argument.arg_type === 'repeat' ? '' : `\t${mapArgumentType(argument, enumMap)},\n`))
    .join('')}] | [{\n${fswCommand.arguments
    .map(argument =>
      argument.arg_type === 'repeat' ? '' : `\t'${argument.name}': ${mapArgumentType(argument, enumMap)},\n`,
    )
    .join('')}}]): ${fswCommandName} {
  return Command.new({
    stem: '${fswCommand.stem}',
    arguments: typeof args[0] === 'object' ? orderCommandArguments(args[0],${fswCommandName}_ARGS_ORDER) : args,
  }) as ${fswCommandName};
}`;

  const interfaces = `
  ${doc}
\tinterface ${fswCommandName} extends Command<[\n${fswCommand.arguments
    .map(argument => (argument.arg_type === 'repeat' ? '' : `\t\t${mapArgumentType(argument, enumMap)},\n`))
    .join('')}\t] | {\n${fswCommand.arguments
    .map(argument =>
      argument.arg_type === 'repeat' ? '' : `\t\t'${argument.name}': ${mapArgumentType(argument, enumMap)},\n`,
    )
    .join('')}\t}> {}`;

  return {
    value,
    interfaces,
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
