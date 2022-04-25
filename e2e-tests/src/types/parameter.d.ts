type EffectiveArguments = {
  arguments: ArgumentsMap;
  errors: ParametersErrorMap;
  success: boolean;
};


type Argument = any;

type ArgumentsMap = Record<ParameterName, Argument>;

type Parameter = { order: number; schema: ValueSchema };

type ParameterError = { message: string; schema: ValueSchema };

type ParametersErrorMap = Record<ParameterName, ParameterError>;

type ParameterName = string;

type ParametersMap = Record<ParameterName, Parameter>;

type ParameterValidationResponse = {
  errors: string[] | null;
  success: boolean;
};
