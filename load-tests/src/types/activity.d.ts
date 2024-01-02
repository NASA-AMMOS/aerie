export type ActivityInsertInput = {
    arguments: any,
    plan_id: number,
    type: string,
    start_offset: string
}

export type CreateActivityResponse = {
    createActivityDirective: {
        id: number
    }
}
