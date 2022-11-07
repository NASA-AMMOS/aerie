type ExternalDatasetInsertInput = {
  plan_id: number,
  dataset_start: String,
  profile_set: {
    [key: string]: {
      type: string,
      schema: any,
      segments: { duration: number, dynamics?: any }[]
    }
  }
};

type ExternalDatasetQueryInput = {
  dataset_id: number,
  plan_id: number
};
