type ExternalDatasetInsertInput = {
  plan_id: number,
  simulation_dataset_id?: number,
  dataset_start: String,
  profile_set: {
    [key: string]: {
      type: string,
      schema: any,
      segments: { duration: number, dynamics?: any }[]
    }
  }
};

type ExternalDatasetExtendInput = {
  dataset_id: number,
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
