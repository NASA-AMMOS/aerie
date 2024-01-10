export type UploadJarResponse = {
    id: number
}

export type MissionModelInsertInput = {
    jar_id: number,
    mission: string,
    name: string,
    version: string
}

export type MissionModelInsertResponse = {
    insert_mission_model_one: {
        id: number
    }
}
