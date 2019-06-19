export interface EuropaDictionaryInput {
  command_dictionary: {
    header: any;
    uplink_file_types: any;
    enum_definitions: any;
    command_definitions: any;
  };
}

export interface EuropaEnum {
  commandName: string;
}
