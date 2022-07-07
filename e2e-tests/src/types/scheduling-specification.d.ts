type SchedulingSpecificationInput = {
    plan_id: number,
    plan_revision: number,
    horizon_start: string,
    horizon_end: string,
    simulation_arguments: object,
    analysis_only: boolean
};
