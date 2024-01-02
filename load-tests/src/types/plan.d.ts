export type PlanIdList = {
    plan: [{ id: number } ]
}

export type CreatePlanInput = {
    model_id: number,
    name: string,
    start_time: string,
    duration: string
}

export type CreatePlanResponse = {
    insert_plan_one: {
        id: number,
        revision: number
    }
}

export type DuplicatePlanResponse = {
    duplicate_plan: {
        new_plan_id: number
    }
}
