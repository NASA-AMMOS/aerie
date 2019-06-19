import { EuropaDictionaryInput } from '../types/europa.type';
import { MpsParameter } from '../types/mps.types';
import { europaEnumArgumentMapping } from '../util/europa.enum.parameter.mapping';

/**
 * Returns a normalized command dictionary in JSON
 * @param xml XML received from a POST request
 */
export const europaConverter = (xml: EuropaDictionaryInput) => {
  const root = xml['command_dictionary'];

  const metadata = getMetadata(root);
  const uplinkFileTypes = getUplinkFileTypes(root);
  const enumDefinitions = getEnumDefinitions(root);
  const commands = normalizeCommands(root, enumDefinitions);

  return {
    commands,
    metadata,
    uplinkFileTypes,
  };
};

/**
 * Returns the metadata for the command dictionary
 * @param root The root of the XML tree
 */
const getMetadata = (root: any) => {
  const { mission_name, spacecraft_id, version, schema_version } = root[
    'header'
  ][0]['$'];

  return {
    mission_name,
    schema_version,
    spacecraft_id,
    version,
  };
};

/**
 * Returns the uplink file types for the command dictionary
 * @param The root of the XML tree
 */
const getUplinkFileTypes = (root: any) => {
  const uplinkFileTypesXml = root['uplink_file_types'][0]['file_type'];
  return uplinkFileTypesXml.map(({ $: fileType }: any) => {
    return {
      extension: fileType.extension,
      fileFormatSpec: fileType.file_format_spec,
      id: fileType.id,
      name: fileType.name,
    };
  });
};

/**
 * Returns a dictionary mapping an Enum type with it's enumerations
 * @param root The root of the XML tree
 */
const getEnumDefinitions = (root: any) => {
  const enumDefinitionsXml = root['enum_definitions'][0]['enum_table'];
  const enumDefinitions: any = {};

  enumDefinitionsXml.forEach((enumDef: any) => {
    const commandName: string = enumDef['$'].name;
    enumDefinitions[commandName] = {
      enums: enumDef['values'][0]['enum'].map(
        (value: any) => value['$'].symbol,
      ),
    };
  });

  return enumDefinitions;
};

/**
 * Returns a normalized form of commands
 * @param root The root of the XML tree
 * @param enumDefinitions A dictionary mapping enum types to it's enumerations
 */
const normalizeCommands = (root: any, enumDefinitions: any) => {
  // const hwCommands = _normalizeHwCommands(
  //   root["command_definitions"][0]["hw_command"]
  // );
  const fswCommands = _normalizeFswCommands(
    root['command_definitions'][0]['fsw_command'],
    enumDefinitions,
  );

  return [...fswCommands];
  // return [...hwCommands, ...fswCommands];
};

/**
 * Helper function to normalize HW Commands
 */
// @ts-ignore
const _normalizeHwCommands = (commands: any) => {
  const hwCommandsXml = commands;
  return hwCommandsXml.map((command: any) => {
    const { stem: name } = command['$'];
    const description = command['description'][0];

    return {
      description,
      name,
      parameters: [],
    };
  });
};

/**
 * Helper function to normalize FSW Commands
 */
const _normalizeFswCommands = (commands: any, enumDefinitions: any) => {
  const fswCommandsXmls = commands;
  return fswCommandsXmls.map((command: any) => {
    const { stem: name } = command['$'];
    const description = command['description'][0];

    const parametersXml = command['arguments'];

    let parameters: MpsParameter[] = [];
    // If the command has arguments
    if (parametersXml) {
      parameters = normalizeParameters(parametersXml, enumDefinitions);
    }

    return {
      description,
      name,
      parameters,
    };
  });
};

/**
 * Normalizes the parameters to the MPS format
 * @param parametersXmlInput Parameter list for a command
 * @param enumDefinitions A mapping between Enum type and it's enumerations
 */
const normalizeParameters = (parametersXmlInput: any, enumDefinitions: any) => {
  const parametersXml = parametersXmlInput[0];
  const parameterTypeKeys = Object.keys(parametersXml);
  let parameters: MpsParameter[] = [];

  // Different parameter types are stored in separate lists
  // We want to enumerate through each type then transform the parameter
  parameterTypeKeys.forEach((type: string) => {
    const mpsType = europaEnumArgumentMapping[type];

    parameters = parametersXml[type].map((param: any) => {
      switch (mpsType) {
        case 'ENUM':
          return {
            defaultValue: '',
            help: param['description'][0],
            name: param['$'].name,
            range: enumDefinitions[param['$']['enum_name']]['enums'],
            type: param['$']['enum_name'],
            units: '',
          };
        case 'STRING':
          return {
            defaultValue: '',
            help: param['description'][0],
            name: param['$'].name,
            range: '',
            type: mpsType,
            units: mpsType,
          };
        case 'UNSIGNED_DECIMAL':
          let range = '';
          if (param['range_of_values']) {
            range = `${
              param['range_of_values'][0]['include'][0]['$']['min']
            }...${param['range_of_values'][0]['include'][0]['$']['max']}`;
          }

          return {
            defaultValue: '',
            help: param['description'][0],
            name: param['$'].name,
            range,
            type: mpsType,
            units: param['$']['units'],
          };
        case 'FLOAT':
        case 'SIGNED_DECIMAL':
          return {
            defaultValue: '',
            help: param['description'][0],
            name: param['$'].name,
            range: '',
            type: mpsType,
            units: param['$']['units'],
          };
        // TODO: How do we handle lists?
        case 'LIST':
          return {
            defaultValue: '',
            help: '',
            name: param['$'].name,
            range: '',
            type: '',
            units: '',
          };
        default:
          throw new Error(
            `Parameter ${
              param['$'].name
            } for command ${name} has an unknown parameter type of ${type}.`,
          );
      }
    });
  });

  return parameters;
};
