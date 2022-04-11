type Constraint = {
  definition: string;
  description: string;
  id: number;
  model_id: number | null;
  name: string;
  plan_id: number | null;
  summary: string;
};

type ConstraintInsertInput = Omit<Constraint, 'id'>;

type ConstraintType = 'model' | 'plan';

type ConstraintViolationAssociations = {
  activityIds?: number[];
  resourceIds?: string[];
};
