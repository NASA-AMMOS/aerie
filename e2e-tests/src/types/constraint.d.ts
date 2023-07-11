type Constraint = {
  definition: string;
  description: string;
  id: number;
  model_id: number | null;
  name: string;
  plan_id: number | null;
};

type ConstraintInsertInput = Omit<Constraint, 'id'> & {
  simulationDatasetId?: number;
};

type ConstraintType = 'model' | 'plan';

type ConstraintViolationAssociations = {
  activityIds?: number[];
  resourceIds?: string[];
};

type TimeRange = {
  start: number;
  end: number;
};

type ConstraintViolation = {
  associations: ConstraintViolationAssociations;
  constraintId: Constraint['id'];
  constraintName: Constraint['name'];
  type: ConstraintType;
  gaps: TimeRange[];
  windows: TimeRange[];
};

type ConstraintRun = {
  constraint_definition: string;
  constraint_id: number;
  simulation_dataset_id: number;
  definition_outdated: boolean;
  violations: ConstraintViolation;
};
