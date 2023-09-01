type EffectiveArguments = {
  arguments?: ArgumentsMap;
  errors?: ParametersErrorMap;
  success: boolean;
  typeName?: string;
};

type EffectiveArgumentItem = {
  activityTypeName: string;
  activityArguments: ArgumentsMap;
};

type Argument = any;

type ArgumentsMap = Record<ParameterName, Argument>;

type Parameter = { order: number; schema: ValueSchema; unit: string };

type ParameterError = { message: string; schema: ValueSchema };

type ParametersErrorMap = Record<ParameterName, ParameterError>;

type ParameterName = string;

type ParametersMap = Record<ParameterName, Parameter>;

type ParameterValidationResponse = {
  errors: string[] | null;
  success: boolean;
};
