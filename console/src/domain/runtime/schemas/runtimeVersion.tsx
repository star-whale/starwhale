import { IListQuerySchema } from '@base/schemas/list'

export interface IRuntimeVersionListQuerySchema extends IListQuerySchema {
    vName?: string
    vTag?: string
}

export interface IRuntimeVersionBuildImageResultSchema {
    success: boolean
    message: string
}
