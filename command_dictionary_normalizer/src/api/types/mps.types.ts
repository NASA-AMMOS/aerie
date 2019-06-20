export interface MpsParameter {
  name: string;
  type: string;
  units: string;
  defaultValue: string;
  range: string;
  help: string;
}

export interface MpsCommand {
  name: string;
  parameter: MpsParameter;
}
