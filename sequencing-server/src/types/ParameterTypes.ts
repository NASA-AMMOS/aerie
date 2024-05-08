declare global {
  export type ParameterDictionary = {
    enumMap: EnumMap;
    header: Header;
    enums: Enum[];
    id: string;
    path: string | null;
    params: Parameter[];
    paramMap: ParamMap<Parameter>;
    paramByTypeMap: {
      E16: ParamMap<ParameterEnum>;
      E32: ParamMap<ParameterEnum>;
      E8: ParamMap<ParameterEnum>;
      F64: ParamMap<ParameterFloat>;
      I16: ParamMap<ParameterInteger>;
      I32: ParamMap<ParameterInteger>;
      I8: ParamMap<ParameterInteger>;
      STR: ParamMap<ParameterString>;
      U16: ParamMap<ParameterUnsigned>;
      U32: ParamMap<ParameterUnsigned>;
      U8: ParamMap<ParameterUnsigned>;
    };
  };

  export type Enum = {
    name: string;
    values: EnumValue[];
  };
  export type EnumMap = {
    [name: string]: Enum;
  };
  export type EnumValue = {
    numeric: number | null;
    symbol: string;
  };

  export type ParamMap<Type extends Parameter> = {
    [stem: string]: Type;
  };
  export type Parameter = ParameterEnum | ParameterFloat | ParameterInteger | ParameterString | ParameterUnsigned;
  type ParameterBase = {
    param_id: number;
    param_name: string;
    parameter_version: number | null;
    parameter_group: string;
    bit_length: number | null;
    description: string;
    rationale: string;
  };
  type ParameterNumber = {
    default_value: number | null;
    range: NumericRange | null;
    units: string;
  } & ParameterBase;

  export type NumericRange = {
    min: number;
    max: number;
  };

  export type ParameterFloat = ParameterNumber & {
    param_type: 'float_param';
  };

  export type ParameterUnsigned = ParameterNumber & {
    param_type: 'unsigned_int_param';
  };
  export type ParameterInteger = ParameterNumber & {
    param_type: 'integer_param';
  };

  export type ParameterString = Omit<ParameterBase, 'bit_length'> & {
    default_value: string | null;
    param_type: 'string_param';
    max_bit_length: number;
  };
  export type ParameterEnum = ParameterBase & {
    default_value: string | null;
    range: string[] | null;
    param_type: 'enum_param';
    enum_type: Enum;
    units: string;
  };
}
export {};
